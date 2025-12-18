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
    suspend fun getAllAppointments(status: AppointmentStatus): List<Appointment>

    // For Doctors: get their own upcoming appointments
    // Change this line:
    suspend fun getAppointmentsForDoctor(doctorId: String, status: AppointmentStatus): List<Appointment> // Added status param
    suspend fun updateAppointmentStatus(appointmentId: String, newStatus: String) // Add this if missing

}

class FirestoreAppointmentRepository : AppointmentRepository {

    private val db = FirebaseFirestore.getInstance()

    private val dateFormatter = SimpleDateFormat("EEE, MMM dd, yyyy - hh:mm a", Locale.getDefault())
    private fun mapStatus(status: String?): AppointmentStatus {
        return when (status) {
            "Booked" -> AppointmentStatus.UPCOMING
            "OnGoing" -> AppointmentStatus.ONGOING
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

            val tasks = apptSnapshot.documents.map {
                async {
                    try {
                        val apptData = it.data ?: return@async null
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
                                id = it.id,
                                dateTime = dateString,
                                locationOrRoom = roomID,
                                status = dbStatus,
                                patient = realPatient,
                                doctor = realDoctor
                            ),
                            dateDate
                        )

                    } catch (e: Exception) {
                        Log.e("FirestoreRepo", "Error linking data for appt: ${it.id}", e)
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

    override suspend fun getAllAppointments(status: AppointmentStatus): List<Appointment> = coroutineScope {
        try {
            val apptSnapshot = db.collection("Appointment")
                .get()
                .await()

            val tasks = apptSnapshot.documents.map {
                async {
                    try {
                        val apptData = it.data ?: return@async null
                        val dbStatus = mapStatus(apptData["status"] as? String)

                        // Filter by the requested status
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

                        // Return the Appointment object and the date for sorting
                        Pair(
                            Appointment(
                                id = it.id,
                                dateTime = dateString,
                                locationOrRoom = roomID,
                                status = dbStatus,
                                patient = realPatient,
                                doctor = realDoctor
                            ),
                            dateDate
                        )

                    } catch (e: Exception) {
                        Log.e("FirestoreRepo", "Error linking data for appt: ${it.id}", e)
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
            Log.e("FirestoreRepo", "Error fetching all appointments", e)
            emptyList()
        }
    }

    override suspend fun getAppointmentsForDoctor(doctorId: String, status: AppointmentStatus): List<Appointment> = coroutineScope {

        val dbStatusString = when(status) {
            AppointmentStatus.UPCOMING -> "Booked"
            AppointmentStatus.COMPLETED -> "Completed"
            AppointmentStatus.CANCELLED -> "Cancelled"
            AppointmentStatus.ONGOING -> "Ongoing"
        }

        try {
            // 1. Get ALL Slots belonging to this Doctor
            // We need this to know which appointments match this doctor
            val mySlotsSnapshot = db.collection("Slot")
                .whereEqualTo("DoctorID", doctorId)
                .get()
                .await()

            val mySlotIds = mySlotsSnapshot.documents.map { it.id }.toSet()

            // Optimization: If doctor has no slots, they definitely have no appointments
            if (mySlotIds.isEmpty()) return@coroutineScope emptyList()

            // 2. Get ALL "Booked" Appointments
            // (In a real production app, you would optimize this query further, but this is fine for now)
            val apptSnapshot = db.collection("Appointment")
                .whereEqualTo("status", dbStatusString)
                .get()
                .await()

            // 3. Filter & Process
            val tasks = apptSnapshot.documents.mapNotNull {
                val apptData = it.data ?: return@mapNotNull null
                val slotId = apptData["slotId"] as? String ?: ""

                // CHECK: Does this appointment belong to one of MY slots?
                if (!mySlotIds.contains(slotId)) return@mapNotNull null

                // If yes, fetch the details (Patient Info) to display on the card
                async {
                    try {
                        val dbStatus = mapStatus(apptData["status"] as? String)

                        // Parse Date (Using the logic we added earlier)
                        val timestamp = apptData["appointmentDate"] as? com.google.firebase.Timestamp
                        val dateDate = timestamp?.toDate()
                        val dateString = if (dateDate != null) {
                            dateFormatter.format(dateDate)
                        } else {
                            "Unknown Date"
                        }

                        // Fetch Patient Details
                        val pId = apptData["patientId"] as? String ?: ""
                        val patientDoc = db.collection("Patient").document(pId).get().await()

                        val realPatient = User(
                            id = pId,
                            name = "${patientDoc.getString("FirstName")} ${patientDoc.getString("LastName")}",
                            role = UserRole.PATIENT,
                            details = patientDoc.getString("Gender") ?: "",
                            imageUrl = patientDoc.getString("ImageUrl")
                        )

                        // We already know the Doctor is US (the current user), so we can mock the Doctor object
                        // or fetch it if we need the specific image/details again.
                        val meAsDoctor = User(
                            id = doctorId,
                            name = "Me", // UI handles this based on role anyway
                            role = UserRole.DOCTOR,
                            details = "My Schedule"
                        )

                        Appointment(
                            id = it.id,
                            dateTime = dateString,
                            locationOrRoom = "My Room", // You could fetch this from the Slot if needed
                            status = dbStatus,
                            patient = realPatient,
                            doctor = meAsDoctor
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            tasks.awaitAll().filterNotNull()

        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error getting doctor appointments", e)
            emptyList()
        }
    }

    override suspend fun updateAppointmentStatus(appointmentId: String, newStatus: String) {
        try {
            db.collection("Appointment").document(appointmentId)
                .update("status", newStatus)
                .await()
        } catch (e: Exception) {
            Log.e("Repo", "Failed to update status", e)
        }
    }
}
