package com.example.qlinic.data.repository

import android.util.Log
import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AppointmentStatus
import com.example.qlinic.data.model.User
import com.example.qlinic.data.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

// 1. This is the INTERFACE. It defines the contract for what a repository can do.
interface AppointmentRepository {
    // For Patients: get their own appointments filtered by status
    suspend fun getAppointmentsForPatient(patientId: String, status: AppointmentStatus): List<Appointment>

    // For Staff: get ALL upcoming appointments
    suspend fun getAllUpcomingAppointments(): List<Appointment>

    // For Doctors: get their own upcoming appointments
    suspend fun getAppointmentsForDoctor(doctorId: String): List<Appointment>

}

class FirestoreAppointmentRepository : AppointmentRepository {

    private val db = FirebaseFirestore.getInstance()

    private val dateFormatter = SimpleDateFormat("EEE, MMM dd, yyyy - hh:mm a", Locale.getDefault())
    private fun mapStatus(status: String?): AppointmentStatus {
        return when (status) {
            "Booked" -> AppointmentStatus.UPCOMING
            "Completed" -> AppointmentStatus.COMPLETED
            "Cancelled" -> AppointmentStatus.CANCELLED
            else -> AppointmentStatus.UPCOMING
        }
    }

    override suspend fun getAppointmentsForPatient(patientId: String, status: AppointmentStatus): List<Appointment> = coroutineScope {
        try {
            val apptSnapshot = db.collection("Appointment")
                .whereEqualTo("patientId", patientId)
                .get()
                .await()

            val tasks = apptSnapshot.documents.map { apptDoc ->
                async {
                    try {
                        val apptData = apptDoc.data ?: return@async null
                        val dbStatus = mapStatus(apptData["status"] as? String)

                        if (dbStatus != status) return@async null

                        val timestamp = apptData["appointmentDate"] as? Timestamp
                        val dateDate = timestamp?.toDate() // Convert to Java Date object

                        val dateString = if (dateDate != null) {
                            dateFormatter.format(dateDate)
                        } else {
                            "Unknown Date"
                        }

                        val slotId = apptData["slotId"] as? String ?: ""
                        if (slotId.isEmpty()) return@async null

                        val slotDoc = db.collection("Slot").document(slotId).get().await()
                        val doctorDocId = slotDoc.getString("DoctorID") ?: ""

                        val doctorDoc = db.collection("Doctor").document(doctorDocId).get().await()
                        val roomID = doctorDoc.getString("RoomID") ?: "Unknown Room"
                        val staffRefId = doctorDoc.getString("DoctorID") ?: "" // Matches ClinicStaff ID (e.g., "S001")

                        val staffDoc = db.collection("ClinicStaff").document(staffRefId).get().await()
                        val firstName = staffDoc.getString("FirstName") ?: "Doctor"
                        val lastName = staffDoc.getString("LastName") ?: ""
                        val fullName = "Dr. $firstName $lastName"

                        val doctorImageUrl = staffDoc.getString("ImageUrl")

                        val pId = apptData["patientId"] as? String ?: ""
                        val patientDoc = db.collection("Patient").document(pId).get().await()

                        val realPatient = User(
                            id = pId,
                            name = "${patientDoc.getString("FirstName")} ${patientDoc.getString("LastName")}",
                            role = UserRole.PATIENT,
                            details = patientDoc.getString("Gender") ?: ""
                        )

                        val realDoctor = User(
                            id = doctorDocId,
                            name = fullName,
                            role = UserRole.DOCTOR,
                            details = doctorDoc.getString("Specialization") ?: "Specialist",
                            imageUrl = doctorImageUrl
                        )

                        Pair(
                            Appointment(
                                id = apptDoc.id,
                                dateTime = dateString,
                                locationOrRoom = roomID,
                                status = dbStatus,
                                patient = realPatient,
                                doctor = realDoctor
                            ),
                            dateDate
                        )

                    } catch (e: Exception) {
                        Log.e("FirestoreRepo", "Error linking data for appt: ${apptDoc.id}", e)
                        null
                    }
                }
            }

            val results = tasks.awaitAll().filterNotNull()

            if (status == AppointmentStatus.UPCOMING) {
                // Sort ASCENDING (Nearest date first)
                results.sortedBy { it.second }.map { it.first }
            } else {
                // (Completed/Cancelled): Sort DESCENDING (Newest first)
                results.sortedByDescending { it.second }.map { it.first }
            }

        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error fetching appointments", e)
            emptyList()
        }
    }

    override suspend fun getAllUpcomingAppointments(): List<Appointment> = emptyList()
    override suspend fun getAppointmentsForDoctor(doctorId: String): List<Appointment> = emptyList()
}