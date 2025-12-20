package com.example.qlinic.data.repository

import android.util.Log
import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AppointmentStatus
import com.example.qlinic.data.model.Slot
import com.example.qlinic.data.model.User
import com.example.qlinic.data.model.UserRole
import com.example.qlinic.utils.getDayStartAndEnd
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                val newAppointment = Appointment(
                    appointmentId = appointmentId,
                    appointmentDate = date,
                    symptoms = symptoms,
                    status = "Booked",
                    isNotifSent = false,
                    slotId = slot.SlotID,
                    patientId = patientId,
                )
                appointmentsCollection.document(appointmentId).set(newAppointment).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error booking appointment", e)
            false
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
                        val dateDate = timestamp?.toDate()

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
                        val staffRefId = doctorDoc.getString("DoctorID") ?: ""

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
                results.sortedBy { it.second }.map { it.first }
            } else {
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

                        if (dbStatus != status) return@async null

                        val timestamp = apptData["appointmentDate"] as? Timestamp
                        val dateDate = timestamp?.toDate()

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
                        val staffRefId = doctorDoc.getString("DoctorID") ?: ""

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
                results.sortedBy { it.second }.map { it.first }
            } else {
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
            val mySlotsSnapshot = db.collection("Slot")
                .whereEqualTo("DoctorID", doctorId)
                .get()
                .await()

            val mySlotIds = mySlotsSnapshot.documents.map { it.id }.toSet()
            if (mySlotIds.isEmpty()) return@coroutineScope emptyList()

            val apptSnapshot = db.collection("Appointment")
                .whereEqualTo("status", dbStatusString)
                .get()
                .await()

            val tasks = apptSnapshot.documents.mapNotNull {
                val apptData = it.data ?: return@mapNotNull null
                val slotId = apptData["slotId"] as? String ?: ""

                if (!mySlotIds.contains(slotId)) return@mapNotNull null

                async {
                    try {
                        val dbStatus = mapStatus(apptData["status"] as? String)
                        val timestamp = apptData["appointmentDate"] as? Timestamp
                        val dateDate = timestamp?.toDate()
                        val dateString = if (dateDate != null) {
                            dateFormatter.format(dateDate)
                        } else {
                            "Unknown Date"
                        }

                        val pId = apptData["patientId"] as? String ?: ""
                        val patientDoc = db.collection("Patient").document(pId).get().await()

                        val realPatient = User(
                            id = pId,
                            name = "${patientDoc.getString("FirstName")} ${patientDoc.getString("LastName")}",
                            role = UserRole.PATIENT,
                            details = patientDoc.getString("Gender") ?: "",
                            imageUrl = patientDoc.getString("ImageUrl")
                        )

                        val meAsDoctor = User(
                            id = doctorId,
                            name = "Me",
                            role = UserRole.DOCTOR,
                            details = "My Schedule"
                        )

                        Appointment(
                            id = it.id,
                            dateTime = dateString,
                            locationOrRoom = "My Room",
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
            Log.e("FirestoreRepo", "Failed to update status", e)
        }
    }
}