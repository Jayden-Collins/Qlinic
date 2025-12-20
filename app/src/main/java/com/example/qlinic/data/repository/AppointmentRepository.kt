package com.example.qlinic.data.repository

import android.util.Log
import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AppointmentStatus
import com.example.qlinic.data.model.User
import com.example.qlinic.data.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

interface AppointmentRepository {
    suspend fun getAppointmentsForPatient(patientId: String, status: AppointmentStatus): List<Appointment>
    suspend fun getAllAppointments(status: AppointmentStatus): List<Appointment>
    suspend fun getAppointmentsForDoctor(doctorId: String, status: AppointmentStatus): List<Appointment>
    suspend fun updateAppointmentStatus(appointmentId: String, newStatus: String)
}

class FirestoreAppointmentRepository : AppointmentRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun getTodayRange(): Pair<Date, Date> {
        val today = LocalDate.now()
        val start = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val end = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant())
        return Pair(start, end)
    }

    private fun mapStatus(status: String?): AppointmentStatus {
        return when (status) {
            "Booked" -> AppointmentStatus.UPCOMING
            "OnGoing" -> AppointmentStatus.ONGOING
            "Completed" -> AppointmentStatus.COMPLETED
            "Cancelled" -> AppointmentStatus.CANCELLED
            else -> AppointmentStatus.UPCOMING
        }
    }

    // --- PATIENT: Custom logic to handle Sorting ---
    override suspend fun getAppointmentsForPatient(patientId: String, status: AppointmentStatus): List<Appointment> = coroutineScope {
        try {
            val apptSnapshot = db.collection("Appointment")
                .whereEqualTo("patientId", patientId)
                .get()
                .await()

            val tasks = apptSnapshot.documents.map { doc ->
                async {
                    val appointment = parseAppointmentFromDoc(doc) ?: return@async null

                    val timestamp = doc.getTimestamp("appointmentDate")

                    if (appointment.status != status) return@async null

                    if (status == AppointmentStatus.COMPLETED || status == AppointmentStatus.CANCELLED) {
                        val threeDaysAgo = LocalDate.now().minusDays(3)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()

                        if (appointment.dateTime.toInstant().isBefore(threeDaysAgo)) {
                            return@async null
                        }
                    }

                    Pair(appointment, appointment.dateTime)
                }
            }

            val results = tasks.awaitAll().filterNotNull()

            // Apply Sorting
            if (status == AppointmentStatus.UPCOMING) {
                results.sortedBy { it.second }.map { it.first }
            } else {
                results.sortedByDescending { it.second }.map { it.first }
            }

        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error fetching patient appointments", e)
            emptyList()
        }
    }

    override suspend fun getAllAppointments(status: AppointmentStatus): List<Appointment> = coroutineScope {
        try {
            val dbStatusString = when(status) {
                AppointmentStatus.UPCOMING -> "Booked"
                AppointmentStatus.ONGOING -> "Booked"
                AppointmentStatus.COMPLETED -> "Completed"
                AppointmentStatus.CANCELLED -> "Cancelled"
            }

            var query: Query = db.collection("Appointment")
                .whereEqualTo("status", dbStatusString)

            // Date Filter for History
            if (status == AppointmentStatus.COMPLETED || status == AppointmentStatus.CANCELLED) {
                val (start, end) = getTodayRange()
                query = query
                    .whereGreaterThanOrEqualTo("appointmentDate", start)
                    .whereLessThanOrEqualTo("appointmentDate", end)
            }

            val snapshot = query.get().await()

            val tasks = snapshot.documents.map { doc ->
                async { parseAppointmentFromDoc(doc) }
            }
            tasks.awaitAll().filterNotNull()

        } catch (e: Exception) {
            Log.e("Repo", "Error fetching all appointments", e)
            emptyList()
        }
    }

    // --- DOCTOR: Get Own (With Date Filter + Slot Match) ---
    override suspend fun getAppointmentsForDoctor(doctorId: String, status: AppointmentStatus): List<Appointment> = coroutineScope {
        try {
            // 1. Get Doctor's Slots
            val mySlotsSnapshot = db.collection("Slot")
                .whereEqualTo("DoctorID", doctorId)
                .get()
                .await()

            val mySlotIds = mySlotsSnapshot.documents.map { it.id }.toSet()
            if (mySlotIds.isEmpty()) return@coroutineScope emptyList()

            // 2. Determine Status Query
            val dbStatusString = when(status) {
                AppointmentStatus.UPCOMING -> "Booked"
                AppointmentStatus.ONGOING -> "Booked"
                AppointmentStatus.COMPLETED -> "Completed"
                AppointmentStatus.CANCELLED -> "Cancelled"
            }

            var query: Query = db.collection("Appointment")
                .whereEqualTo("status", dbStatusString)

            // 3. Date Filter for History
            if (status == AppointmentStatus.COMPLETED || status == AppointmentStatus.CANCELLED) {
                val (start, end) = getTodayRange()
                query = query
                    .whereGreaterThanOrEqualTo("appointmentDate", start)
                    .whereLessThanOrEqualTo("appointmentDate", end)
            }

            val snapshot = query.get().await()

            // 4. Client-side Filter (Match Slot ID)
            val tasks = snapshot.documents.mapNotNull { apptDoc ->
                val slotId = apptDoc.getString("slotId")
                if (slotId != null && mySlotIds.contains(slotId)) {
                    async { parseAppointmentFromDoc(apptDoc) }
                } else null
            }
            tasks.awaitAll().filterNotNull()

        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error getting doctor appointments", e)
            emptyList()
        }
    }

    private suspend fun parseAppointmentFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot): Appointment? {
        return try {
            val data = doc.data ?: return null

            val id = doc.id
            val statusStr = data["status"] as? String
            val appointmentStatus = mapStatus(statusStr)

            val timestamp = data["appointmentDate"] as? Timestamp
            val rawDate = timestamp?.toDate() ?: Date()

            val patientId = data["patientId"] as? String ?: ""
            var patientUser = User(patientId, "Unknown Patient", UserRole.PATIENT, "")
            if (patientId.isNotEmpty()) {
                val patientDoc = db.collection("Patient").document(patientId).get().await()
                if (patientDoc.exists()) {
                    val firstName = patientDoc.getString("FirstName") ?: ""
                    val lastName = patientDoc.getString("LastName") ?: ""
                    val gender = patientDoc.getString("Gender") ?: ""
                    val age = patientDoc.getString("Age") ?: ""

                    patientUser = User(
                        id = patientId,
                        name = "$firstName $lastName",
                        role = UserRole.PATIENT,
                        details = "$gender, $age",
                        imageUrl = patientDoc.getString("ImageUrl")
                    )
                }
            }

            val slotId = data["slotId"] as? String ?: ""
            var doctorUser = User("unknown", "Unknown Doctor", UserRole.DOCTOR, "")

            var finalRoom = data["locationOrRoom"] as? String ?: ""

            if (slotId.isNotEmpty()) {
                val slotDoc = db.collection("Slot").document(slotId).get().await()
                val doctorId = slotDoc.getString("DoctorID")

                if (!doctorId.isNullOrEmpty()) {
                    val doctorDoc = db.collection("Doctor").document(doctorId).get().await()
                    val specialization = doctorDoc.getString("Specialization") ?: "Specialist"
                    val doctorRoom = doctorDoc.getString("RoomID") ?: "Consultation Room"

                    if (finalRoom.isEmpty()) {
                        finalRoom = doctorRoom
                    }

                    val staffQuery = db.collection("ClinicStaff")
                        .whereEqualTo("StaffID", doctorId)
                        .get()
                        .await()

                    if (!staffQuery.isEmpty) {
                        val staffDoc = staffQuery.documents[0]
                        val drFirst = staffDoc.getString("FirstName") ?: ""
                        val drLast = staffDoc.getString("LastName") ?: ""

                        doctorUser = User(
                            id = doctorId,
                            name = "Dr. $drFirst $drLast",
                            role = UserRole.DOCTOR,
                            details = specialization,
                            imageUrl = staffDoc.getString("ImageUrl")
                        )
                    } else {
                        doctorUser = User(doctorId, "Dr. Unknown", UserRole.DOCTOR, specialization)
                    }
                }
            }

            if (finalRoom.isEmpty()) finalRoom = "Consultation Room"

            Appointment(
                id = id,
                dateTime = rawDate,
                locationOrRoom = finalRoom,
                status = appointmentStatus,
                doctor = doctorUser,
                patient = patientUser
            )

        } catch (e: Exception) {
            Log.e("Repo", "Error parsing appointment: ${doc.id}", e)
            null
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