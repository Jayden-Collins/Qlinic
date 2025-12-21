package com.example.qlinic.data.repository

import android.util.Log
import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AppointmentStatus
import com.example.qlinic.data.model.Patient
import com.example.qlinic.data.model.ClinicStaff
import com.example.qlinic.data.model.Slot
import com.example.qlinic.utils.getDayStartAndEnd
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.Date

interface AppointmentRepository {
    suspend fun getAppointmentsForPatient(patientId: String, status: AppointmentStatus): List<Appointment>
    suspend fun getAllAppointments(status: AppointmentStatus): List<Appointment>
    suspend fun getAppointmentsForDoctor(doctorId: String, status: AppointmentStatus): List<Appointment>
    suspend fun updateAppointmentStatus(appointmentId: String, newStatus: String)
    suspend fun bookAppointment(patientId: String, slot: Slot, date: Date, symptoms: String): Boolean
}

class FirestoreAppointmentRepository : AppointmentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val appointmentsCollection = db.collection("Appointment")

    private fun mapStatus(status: String?): AppointmentStatus {
        return when (status) {
            "Booked", "Upcoming", "UPCOMING" -> AppointmentStatus.UPCOMING
            "OnGoing", "Ongoing", "ONGOING" -> AppointmentStatus.ONGOING
            "Completed", "COMPLETED" -> AppointmentStatus.COMPLETED
            "Cancelled", "CANCELLED" -> AppointmentStatus.CANCELLED
            "No Show", "NO_SHOW" -> AppointmentStatus.NO_SHOW
            else -> AppointmentStatus.UPCOMING
        }
    }

    private fun mapStatusToString(status: AppointmentStatus): String {
        return when (status) {
            AppointmentStatus.UPCOMING -> "Booked"
            AppointmentStatus.ONGOING -> "OnGoing"
            AppointmentStatus.COMPLETED -> "Completed"
            AppointmentStatus.CANCELLED -> "Cancelled"
            AppointmentStatus.NO_SHOW -> "No Show"
        }
    }

    override suspend fun bookAppointment(patientId: String, slot: Slot, date: Date, symptoms: String): Boolean {
        return try {
            val (startOfDay, endOfDay) = getDayStartAndEnd(date)
            val querySnapshot = appointmentsCollection
                .whereEqualTo("slotId", slot.SlotID)
                .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                .whereLessThanOrEqualTo("appointmentDate", endOfDay)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                val appointmentId = appointmentsCollection.document().id
                val data = hashMapOf(
                    "appointmentId" to appointmentId,
                    "appointmentDate" to date,
                    "symptoms" to symptoms,
                    "status" to "Booked",
                    "isNotifSent" to false,
                    "slotId" to slot.SlotID,
                    "patientId" to patientId
                )
                appointmentsCollection.document(appointmentId).set(data).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error booking appointment", e)
            false
        }
    }

    override suspend fun getAppointmentsForPatient(patientId: String, status: AppointmentStatus): List<Appointment> {
        val snapshot = appointmentsCollection
            .whereEqualTo("patientId", patientId)
            .get()
            .await()
        return fetchAndLink(snapshot.documents, status)
    }

    override suspend fun getAllAppointments(status: AppointmentStatus): List<Appointment> {
        val snapshot = appointmentsCollection.get().await()
        return fetchAndLink(snapshot.documents, status)
    }

    override suspend fun getAppointmentsForDoctor(doctorId: String, status: AppointmentStatus): List<Appointment> {
        val slotsSnapshot = db.collection("Slot").whereEqualTo("DoctorID", doctorId).get().await()
        val slotIds = slotsSnapshot.documents.map { it.id }
        
        if (slotIds.isEmpty()) return emptyList()

        val snapshot = appointmentsCollection
            .whereEqualTo("status", mapStatusToString(status))
            .get()
            .await()
        
        val filteredDocs = snapshot.documents.filter { slotIds.contains(it.getString("slotId")) }
        return fetchAndLink(filteredDocs)
    }

    private suspend fun fetchAndLink(docs: List<DocumentSnapshot>, target: AppointmentStatus? = null): List<Appointment> = coroutineScope {
        docs.map { doc ->
            async {
                val statusStr = doc.getString("status")
                val dbStatus = mapStatus(statusStr)
                
                if (target != null && dbStatus != target) return@async null

                val appt = doc.toObject(Appointment::class.java)?.copy(
                    appointmentId = doc.id,
                    status = dbStatus
                ) ?: return@async null

                // Fetch Patient
                appt.patient = db.collection("Patient").document(appt.patientId).get().await().toObject(Patient::class.java)

                // Fetch Doctor via Slot
                val slotDoc = db.collection("Slot").document(appt.slotId).get().await()
                val docId = slotDoc.getString("DoctorID") ?: ""
                if (docId.isNotEmpty()) {
                    val doctorTable = db.collection("Doctor").document(docId).get().await()
                    appt.doctorSpecialty = doctorTable.getString("Specialization")
                    appt.doctor = db.collection("ClinicStaff").document(docId).get().await().toObject(ClinicStaff::class.java)
                }
                appt
            }
        }.awaitAll().filterNotNull().sortedBy { it.appointmentDate }
    }

    override suspend fun updateAppointmentStatus(appointmentId: String, newStatus: String) {
        try {
            appointmentsCollection.document(appointmentId)
                .update("status", newStatus)
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Failed to update status", e)
        }
    }
}
