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
            "Upcoming", "UPCOMING" -> AppointmentStatus.UPCOMING
            "OnGoing", "Ongoing", "ONGOING" -> AppointmentStatus.ONGOING
            "Completed", "COMPLETED" -> AppointmentStatus.COMPLETED
            "Cancelled", "CANCELLED" -> AppointmentStatus.CANCELLED
            "No Show", "NO_SHOW" -> AppointmentStatus.NO_SHOW
            else -> AppointmentStatus.UPCOMING
        }
    }

    private fun mapStatusToString(status: AppointmentStatus): String {
        return when (status) {
            AppointmentStatus.UPCOMING -> "Upcoming"
            AppointmentStatus.ONGOING -> "On Going"
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
                    "status" to "Upcoming",
                    "isNotifSent" to false,
                    "slotId" to slot.SlotID,
                    "patientId" to patientId,
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
        Log.d("FirestoreRepo", "getAppointmentsForPatient called for ID: $patientId")
        val snapshot = appointmentsCollection
            .whereEqualTo("patientId", patientId)
            .get()
            .await()
        Log.d("FirestoreRepo", "Query returned ${snapshot.size()} documents for patient $patientId")
        return fetchAndLink(snapshot.documents, status)
    }

    override suspend fun getAllAppointments(status: AppointmentStatus): List<Appointment> {
        Log.d("FirestoreRepo", "getAllAppointments (Staff) called")
        val snapshot = appointmentsCollection.get().await()
        Log.d("FirestoreRepo", "Total appointments in DB: ${snapshot.size()}")
        return fetchAndLink(snapshot.documents, status)
    }

    override suspend fun getAppointmentsForDoctor(doctorId: String, status: AppointmentStatus): List<Appointment> {
        Log.d("FirestoreRepo", "getAppointmentsForDoctor called for ID: $doctorId")
        val slotsSnapshot = db.collection("Slot").whereEqualTo("DoctorID", doctorId).get().await()
        val slotIds = slotsSnapshot.documents.map { it.id }
        
        if (slotIds.isEmpty()) {
            Log.d("FirestoreRepo", "No slots found for doctor $doctorId")
            return emptyList()
        }

        val snapshot = appointmentsCollection
            .whereEqualTo("status", mapStatusToString(status))
            .get()
            .await()
        
        val filteredDocs = snapshot.documents.filter { slotIds.contains(it.getString("slotId")) }
        Log.d("FirestoreRepo", "Found ${filteredDocs.size} appointments for doctor $doctorId with status $status")
        return fetchAndLink(filteredDocs)
    }

    private suspend fun fetchAndLink(docs: List<DocumentSnapshot>, target: AppointmentStatus? = null): List<Appointment> = coroutineScope {
        docs.map { doc ->
            async {
                val apptId = doc.id
                val rawData = doc.data
                Log.d("FirestoreRepo", "Processing doc: $apptId, rawData: $rawData")

                val statusStr = doc.getString("status")
                val dbStatus = mapStatus(statusStr)
                
                if (target != null && dbStatus != target) {
                    Log.d("FirestoreRepo", "Skipping doc $apptId: Status mismatch ($dbStatus vs $target)")
                    return@async null
                }

                val appt = doc.toObject(Appointment::class.java)?.copy(
                    appointmentId = apptId,
                    status = dbStatus
                ) ?: run {
                    Log.e("FirestoreRepo", "Failed to deserialize doc $apptId")
                    return@async null
                }

                // Fetch Patient
                val pId = appt.patientId
                Log.d("FirestoreRepo", "Fetching patient info for ID: $pId")
                val pDoc = db.collection("Patient").document(pId).get().await()
                appt.patient = pDoc.toObject(Patient::class.java)
                Log.d("FirestoreRepo", "Patient name: ${appt.patient?.firstName} ${appt.patient?.lastName}")

                // Fetch Doctor via Slot
                val sId = appt.slotId
                Log.d("FirestoreRepo", "Fetching slot info for ID: $sId")
                val slotDoc = db.collection("Slot").document(sId).get().await()
                val docId = slotDoc.getString("DoctorID") ?: ""

                if (docId.isNotEmpty()) {
                    Log.d("FirestoreRepo", "Fetching doctor info for ID: $docId")
                    // Fetch the Room ID from the Doctor table
                    val doctorDoc = db.collection("Doctor").document(docId).get().await()
                    appt.doctorSpecialty = doctorDoc.getString("Specialization")
                    
                    // User said "Correct field name 'room'"
                    val roomVal = doctorDoc.getString("room") ?: doctorDoc.getString("RoomID")
                    appt.roomId = roomVal ?: "TBD"
                    Log.d("FirestoreRepo", "Doctor room fetched: ${appt.roomId}, Specialty: ${appt.doctorSpecialty}")
                    
                    // Fetch Doctor's display info (Name, Image) from ClinicStaff
                    val staffDoc = db.collection("ClinicStaff").document(docId).get().await()
                    appt.doctor = staffDoc.toObject(ClinicStaff::class.java)
                    Log.d("FirestoreRepo", "Doctor name: ${appt.doctor?.firstName} ${appt.doctor?.lastName}")
                } else {
                    Log.w("FirestoreRepo", "No DoctorID found in slot $sId for appointment $apptId")
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
