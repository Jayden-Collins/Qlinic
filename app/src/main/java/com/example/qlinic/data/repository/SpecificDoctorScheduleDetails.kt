package com.example.qlinic.data.repository

import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AvailabilityException
import com.example.qlinic.data.model.ClinicStaff
import com.example.qlinic.data.model.Doctor
import com.example.qlinic.data.model.SpecificDoctorInfo
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date

class SpecificDoctorScheduleDetails {
    private val firestore: FirebaseFirestore = Firebase.firestore

    enum class Availability{
        AVAILABLE,
        ONLEAVE,
        BOOKED,
    }

    // get doctors by doctor id
    suspend fun getDoctorById(doctorID: String): SpecificDoctorInfo? {
        return try{
            // fetch from clinic staff collection
            val staffDoc = firestore
                .collection("ClinicStaff")
                .document(doctorID)
                .get()
                .await()

            if (staffDoc.exists()){
                val clinicStaff = ClinicStaff(
                    staffID = staffDoc.getString("StaffID") ?: staffDoc.id,
                    email = staffDoc.getString("Email") ?: "",
                    firstName = staffDoc.getString("FirstName") ?: "",
                    lastName = staffDoc.getString("LastName") ?: "",
                    gender = staffDoc.getString("Gender") ?: "",
                    imageUrl = staffDoc.getString("ImageUrl") ?: "",
                    phoneNumber = staffDoc.getString("PhoneNumber") ?: "",
                    role = staffDoc.getString("Role") ?: "",
                    isActive = staffDoc.getBoolean("isActive") ?: true
                )

                // Fetch doctor details from Doctor collection
                val doctorDoc = firestore.collection("Doctor")
                    .whereEqualTo("DoctorID", doctorID)
                    .get()
                    .await()

                if (!doctorDoc.isEmpty) {
                    val doctorData = doctorDoc.documents[0]
                    val doctor = Doctor(
                        doctorID = doctorData.getString("DoctorID") ?: "",
                        specialization = doctorData.getString("Specialization") ?: "",
                        description = doctorData.getString("Description") ?: "",
                        yearsOfExp = (doctorData.getLong("YearsOfExp") ?: 0L).toInt(),
                        roomID = doctorData.getString("RoomID") ?: ""
                    )
                    SpecificDoctorInfo(clinicStaff, doctor)
                }else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception){
            println("Error fetching doctor: ${e.message}")
            null
        }
    }


    // get appointments based on doctor and slot information
    suspend fun getAppointmentsForDoctor(
        doctorID: String,
        startDate: Date? = null,
        endDate: Date? = null,
    ): List<Appointment> {
        return try {
            val slotSnapshot = firestore.collection("Slot")
                .whereEqualTo("DoctorID", doctorID)
                .get()
                .await()

            if (slotSnapshot.isEmpty) return emptyList()

            val slotIds = slotSnapshot.documents.map { it.id }
            if (slotIds.isEmpty()) return emptyList()

            val result = mutableListOf<Appointment>()

            // chunk to respect Firestore limits
            val chunks = slotIds.chunked(10)
            for (chunk in chunks) {
                val baseQuery = firestore.collection("Appointment")
                var query = if (chunk.size == 1) baseQuery.whereEqualTo("slotId", chunk.first())
                else baseQuery.whereIn("slotId", chunk)

                // If caller provided start/end date limits, apply server-side range filter to reduce documents
                if (startDate != null && endDate != null) {
                    query = query
                        .whereGreaterThanOrEqualTo("appointmentDate", com.google.firebase.Timestamp(startDate))
                        .whereLessThanOrEqualTo("appointmentDate", com.google.firebase.Timestamp(endDate))
                }

                val snap = query.get().await()
                for (doc in snap.documents) {
                    try {
                        val appointmentId = doc.getString("appointmentId") ?: doc.getString("appointmentId") ?: doc.id
                        val ts = doc.getTimestamp("appointmentDate") ?: doc.getTimestamp("appointmentDate")
                        val status = doc.getString("status") ?: doc.getString("status") ?: ""

                        if (ts == null) continue

                        // date filter if provided (redundant if server-side range applied, but keeping as safety)
                        if (startDate != null && endDate != null) {
                            val d = ts.toDate()
                            if (d.before(startDate) || d.after(endDate)) continue
                        }

                        if (!status.equals("Booked", ignoreCase = true)) continue

                        val appointment = Appointment(
                            appointmentID = appointmentId,
                            appointmentDateTime = ts,
                            isReminderSent = doc.getBoolean("isNotifSent") ?: doc.getBoolean("isReminderSent") ?: false,
                            patientID = doc.getString("patientId") ?: doc.getString("patientID") ?: "",
                            slotID = doc.getString("slotId") ?: doc.getString("slotID") ?: "",
                            status = status,
                            symptoms = doc.getString("symptoms") ?: doc.getString("Symptoms") ?: ""
                        )
                        result.add(appointment)
                    } catch (e: Exception) {
                        println("Error parsing appointment doc=${doc.id}: ${e.message}")
                    }
                }
            }

            result
        } catch (e: Exception) {
            println("Error fetching appointments: ${e.message}")
            emptyList()
        }
    }

    // get doctor availability exceptions (on-leave / unavailable periods)
    suspend fun getDoctorAvailabilityExceptions(doctorID: String): List<AvailabilityException> {
        return try {
            // Query by doctorID only to avoid requiring a composite index for combined queries.
            val snapshot = firestore.collection("AvailabilityException")
                .whereEqualTo("doctorID", doctorID)
                .get()
                .await()

            val nowTs = Timestamp.now()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val endTs = doc.getTimestamp("endDateTime") ?: Timestamp(0,0)
                    // filter out already expired exceptions on the client side
                    if (endTs.seconds >= nowTs.seconds) {
                        AvailabilityException(
                            exceptionID = doc.getString("exceptionID") ?: doc.id,
                            startDateTime = doc.getTimestamp("startDateTime") ?: Timestamp.now(),
                            endDateTime = endTs,
                            reason = doc.getString("reason") ?: "",
                            doctorID = doc.getString("doctorID") ?: "",
                            staffID = doc.getString("staffID") ?: "",
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    println("Error fetching availability exception ${doc.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching availability exceptions: ${e.message}")
            emptyList()
        }
    }

    // Get doctor leaves for a specific month
    suspend fun getDoctorLeavesForMonth(
        doctorId: String,
        monthDate: Date
    ): List<Date> {
        return try {
            val exceptions = getDoctorAvailabilityExceptions(doctorId)

            val calendar = Calendar.getInstance().apply {
                time = monthDate
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val startDate = calendar.time
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val endDate = calendar.time

            exceptions.flatMap { exception ->
                getLeaveDatesInRange(exception, startDate, endDate)
            }.distinct()
        } catch (e: Exception) {
            println("Error getting leaves for month: ${e.message}")
            emptyList()
        }
    }

    // check doctor availability
    suspend fun isDoctorAvailable(
        doctorID: String,
        date: Date,
    ): Availability{
        return try{
            // check if doctor is on leave
            val leaves = getDoctorLeavesForMonth(doctorID, date)
            val isOnLeave = leaves.any { leaveDate ->
                isSameDay(leaveDate, date)
            }
            if (isOnLeave){
                return Availability.ONLEAVE
            }

            val appointments = getAppointmentsForDoctor(
                doctorID = doctorID,
                startDate = date,
                endDate = date
            )

            if (appointments.isNotEmpty()) {
                Availability.BOOKED
            } else {
                Availability.AVAILABLE
            }

        } catch (e: Exception){
            println("Error checking doctor availability: ${e.message}")
            Availability.AVAILABLE
        }

    }

    // get leave dates in range from availability exception
    private fun getLeaveDatesInRange(
        exception: AvailabilityException,
        rangeStart: Date,
        rangeEnd: Date
    ): List<Date> {
        val dates = mutableListOf<Date>()

        val exceptionStartDate = exception.startDateTime.toDate()
        val exceptionEndDate = exception.endDateTime.toDate()

        // Check if exception overlaps with range
        if (exceptionStartDate <= rangeEnd && exceptionEndDate >= rangeStart) {
            val overlapStart = maxOf(exceptionStartDate, rangeStart)
            val overlapEnd = minOf(exceptionEndDate, rangeEnd)

            val calendar = Calendar.getInstance()
            calendar.time = overlapStart

            while (calendar.time <= overlapEnd) {
                dates.add(calendar.time)
                calendar.add(Calendar.DATE, 1)
            }
        }


        return dates
    }

    // Helper: get start of day (00:00:00.000) for a given date
    private fun startOfDay(date: Date): Date {
        val cal = Calendar.getInstance().apply { time = date }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    // Helper: get end of day (23:59:59.999) for a given date
    private fun endOfDay(date: Date): Date {
        val cal = Calendar.getInstance().apply { time = date }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        return cal.time
    }

    // mark doctor as on leave for specific dates, when select the dates
    suspend fun markDoctorAsLeave(
        doctorID: String,
        dates: List<Date>,
        staffID: String,
        reason: String = "OnLeave"
    ): Boolean {
        return try {
            val collectionRef = firestore.collection("AvailabilityException")

            dates.forEach { date ->
                val startOfDay = startOfDay(date)
                val endOfDay = endOfDay(date)

                // Get a new document reference to obtain Firestore auto-generated ID
                val newDocRef = collectionRef.document()
                val generatedId = newDocRef.id

                val exceptionData = hashMapOf(
                    "exceptionID" to generatedId,
                    "doctorID" to doctorID,
                    "staffID" to staffID,
                    "startDateTime" to Timestamp(startOfDay),
                    "endDateTime" to Timestamp(endOfDay),
                    "reason" to reason,
                )

                // Use the generated document reference to write the data (single write)
                newDocRef.set(exceptionData).await()
            }
            true
        } catch (e: Exception) {
            println("Error marking leave: ${e.message}")
            false
        }
    }

    // cancel doctor leave for specific dates
    suspend fun cancelDoctorLeave(
        doctorID: String,
        dates: List<Date>
    ): Boolean {
        return try {
            // Get all exceptions for this doctor
            val exceptions = getDoctorAvailabilityExceptions(doctorID)

            dates.forEach { date ->
                // Find exceptions that cover this date (compare using whole-day boundaries)
                val exceptionsForDate = exceptions.filter { exception ->
                    val exceptionStart = exception.startDateTime.toDate()
                    val exceptionEnd = exception.endDateTime.toDate()
                    // if the date's whole-day range intersects the exception range
                    val dStart = startOfDay(date)
                    val dEnd = endOfDay(date)
                    !(dEnd.before(exceptionStart) || dStart.after(exceptionEnd))
                }

                // Delete these exceptions
                exceptionsForDate.forEach { exception ->
                    firestore.collection("AvailabilityException")
                        .document(exception.exceptionID)
                        .delete()
                        .await()
                }
            }
            true
        } catch (e: Exception) {
            println("Error cancelling leave: ${e.message}")
            false
        }
    }

    // Delete appointment by business appointmentId field (removes matching documents)
    suspend fun deleteAppointment(appointmentId: String): Boolean {
        return try {
            val snapshot = firestore.collection("Appointment")
                .whereEqualTo("appointmentId", appointmentId)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                firestore.collection("Appointment").document(doc.id).delete().await()
            }

            true
        } catch (e: Exception) {
            println("Error deleting appointment: ${e.message}")
            false
        }
    }

    fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    // New: fetch patient display names for a list of patient IDs.
    // Returns a map of patientId -> displayName. Uses whereIn in chunks to respect Firestore limits.
    suspend fun getPatientNames(patientIds: List<String>): Map<String, String> {
        return try {
            val distinctIds = patientIds.filter { it.isNotBlank() }.distinct()
            if (distinctIds.isEmpty()) return emptyMap()

            val result = mutableMapOf<String, String>()
            val chunks = distinctIds.chunked(10)

            for (chunk in chunks) {
                val snapshot = firestore.collection("Patient")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                snapshot.documents.forEach { doc ->
                    try {
                        val id = doc.id
                        val first = doc.getString("FirstName")?.trim().orEmpty()
                        val last = doc.getString("LastName")?.trim().orEmpty()
                        val display = listOf(first, last).filter { it.isNotEmpty() }.joinToString(" ").ifEmpty { doc.getString("DisplayName") ?: id }
                        result[id] = display
                    } catch (e: Exception) {
                        // fallback to id
                        result[doc.id] = doc.id
                    }
                }
            }

            // For any ids not found in Firestore, map to id as fallback
            distinctIds.forEach { id ->
                if (!result.containsKey(id)) result[id] = id
            }

            result
        } catch (e: Exception) {
            println("Error fetching patient names: ${e.message}")
            // fallback map
            patientIds.filter { it.isNotBlank() }.distinct().associateWith { it }
        }
    }

    // New: fetch slot start times for a list of slot document IDs (uses whereIn in chunks)
    suspend fun getSlotTimesByIds(slotIds: List<String>): Map<String, String> {
        return try {
            val distinctIds = slotIds.filter { it.isNotBlank() }.distinct()
            if (distinctIds.isEmpty()) return emptyMap()

            val result = mutableMapOf<String, String>()
            val chunks = distinctIds.chunked(10)

            for (chunk in chunks) {
                val snapshot = firestore.collection("Slot")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                snapshot.documents.forEach { doc ->
                    try {
                        val id = doc.id
                        val slotObj = doc.toObject(com.example.qlinic.data.model.Slot::class.java)
                        val time = slotObj?.SlotStartTime ?: doc.getString("SlotStartTime") ?: ""
                        result[id] = time
                    } catch (e: Exception) {
                        println("Error parsing slot ${doc.id}: ${e.message}")
                    }
                }
            }

            result
        } catch (e: Exception) {
            println("Error fetching slot times: ${e.message}")
            emptyMap()
        }
    }
}
