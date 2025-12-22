package com.example.qlinic.data.repository

import android.icu.util.Calendar
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
import com.example.qlinic.data.model.AppointmentStatistics
import com.example.qlinic.data.model.ChartData
import com.example.qlinic.data.model.PeakHoursReportData
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface AppointmentRepository {
    suspend fun getAppointmentsForPatient(
        patientId: String,
        status: AppointmentStatus
    ): List<Appointment>

    suspend fun getAllAppointments(status: AppointmentStatus): List<Appointment>
    suspend fun getAppointmentsForDoctor(
        doctorId: String,
        status: AppointmentStatus
    ): List<Appointment>

    suspend fun updateAppointmentStatus(appointmentId: String, newStatus: String)
    suspend fun bookAppointment(
        patientId: String,
        slot: Slot,
        date: Date,
        symptoms: String
    ): Boolean

    suspend fun isSlotTaken(slotId: String, date: Date): Boolean

    suspend fun getStatistics(
        type: String,
        department: String,
        startDateStr: String,
        endDateStr: String
    ): AppointmentStatistics

    suspend fun getPeakHoursReportData(
        type: String,
        department: String,
        startDateStr: String,
        endDateStr: String
    ): PeakHoursReportData
}

class FirestoreAppointmentRepository : AppointmentRepository {

    private val db = Firebase.firestore
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

    override suspend fun isSlotTaken(slotId: String, date: Date): Boolean {
        val (startOfDay, endOfDay) = getDayStartAndEnd(date)
        val querySnapshot = appointmentsCollection
            .whereEqualTo("slotId", slotId)
            .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
            .whereLessThanOrEqualTo("appointmentDate", endOfDay)
            .get()
            .await()
        // Only consider appointments that are not Cancelled or No Show
        return querySnapshot.documents.any { doc ->
            val status = doc.getString("status")
            status != "Cancelled" && status != "No Show"
        }
    }

    override suspend fun bookAppointment(
        patientId: String,
        slot: Slot,
        date: Date,
        symptoms: String
    ): Boolean {
        return try {
            val (startOfDay, endOfDay) = getDayStartAndEnd(date)
            val querySnapshot = appointmentsCollection
                .whereEqualTo("slotId", slot.SlotID)
                .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                .whereLessThanOrEqualTo("appointmentDate", endOfDay)
                .get()
                .await()

            // Only consider appointments that are not Cancelled or No Show
            val activeAppointments = querySnapshot.documents.filter { doc ->
                val status = doc.getString("status")
                status != "Cancelled" && status != "No Show"
            }

            if (activeAppointments.isEmpty()) {
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

    override suspend fun getAppointmentsForPatient(
        patientId: String,
        status: AppointmentStatus
    ): List<Appointment> {
        Log.d("FirestoreRepo", "getAppointmentsForPatient called for ID: $patientId")
        val snapshot = appointmentsCollection
            .whereEqualTo("patientId", patientId)
            .get()
            .await()
        Log.d(
            "FirestoreRepo",
            "Query returned ${snapshot.size()} documents for patient $patientId"
        )
        return fetchAndLink(snapshot.documents, status)
    }

    override suspend fun getAllAppointments(status: AppointmentStatus): List<Appointment> {
        Log.d("FirestoreRepo", "getAllAppointments (Staff) called")
        val snapshot = appointmentsCollection.get().await()
        Log.d("FirestoreRepo", "Total appointments in DB: ${snapshot.size()}")
        return fetchAndLink(snapshot.documents, status)
    }

    override suspend fun getAppointmentsForDoctor(
        doctorId: String,
        status: AppointmentStatus
    ): List<Appointment> {
        Log.d("FirestoreRepo", "getAppointmentsForDoctor called for ID: $doctorId")
        val slotsSnapshot =
            db.collection("Slot").whereEqualTo("DoctorID", doctorId).get().await()
        val slotIds = slotsSnapshot.documents.map { it.id }

        if (slotIds.isEmpty()) {
            Log.d("FirestoreRepo", "No slots found for doctor $doctorId")
            return emptyList()
        }

        val snapshot = appointmentsCollection
            .whereEqualTo("status", mapStatusToString(status))
            .get()
            .await()

        val filteredDocs =
            snapshot.documents.filter { slotIds.contains(it.getString("slotId")) }
        Log.d(
            "FirestoreRepo",
            "Found ${filteredDocs.size} appointments for doctor $doctorId with status $status"
        )
        return fetchAndLink(filteredDocs)
    }

    private suspend fun fetchAndLink(
        docs: List<DocumentSnapshot>,
        target: AppointmentStatus? = null
    ): List<Appointment> = coroutineScope {
        docs.map { doc ->
            async {
                val apptId = doc.id
                val rawData = doc.data
                Log.d("FirestoreRepo", "Processing doc: $apptId, rawData: $rawData")

                val statusStr = doc.getString("status")
                val dbStatus = mapStatus(statusStr)

                if (target != null && dbStatus != target) {
                    Log.d(
                        "FirestoreRepo",
                        "Skipping doc $apptId: Status mismatch ($dbStatus vs $target)"
                    )
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
                Log.d(
                    "FirestoreRepo",
                    "Patient name: ${appt.patient?.firstName} ${appt.patient?.lastName}"
                )

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

                    // Room field name 'room'
                    val roomVal = doctorDoc.getString("room") ?: doctorDoc.getString("RoomID")
                    appt.roomId = roomVal ?: "TBD"
                    Log.d(
                        "FirestoreRepo",
                        "Doctor room fetched: ${appt.roomId}, Specialty: ${appt.doctorSpecialty}"
                    )

                    // Fetch Doctor's display info (Name, Image) from ClinicStaff
                    val staffDoc = db.collection("ClinicStaff").document(docId).get().await()
                    appt.doctor = staffDoc.toObject(ClinicStaff::class.java)
                    Log.d(
                        "FirestoreRepo",
                        "Doctor name: ${appt.doctor?.firstName} ${appt.doctor?.lastName}"
                    )
                } else {
                    Log.w(
                        "FirestoreRepo",
                        "No DoctorID found in slot $sId for appointment $apptId"
                    )
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

    override suspend fun getStatistics(
        type: String,
        department: String,
        startDateStr: String,
        endDateStr: String
    ): AppointmentStatistics {
        val (startDate, endDate) = getStartAndEndDates(type, startDateStr, endDateStr)

        if (startDate == null || endDate == null) {
            return AppointmentStatistics() // Return empty if dates are invalid
        }

        try {
            // Get a list of all doctor IDs that match the selected department
            val doctorIds = if (department != "All Department") {
                getDoctorIdsForDepartment(department)
            } else {
                emptyList() // Empty list means we don't filter by doctor
            }

            // Fetch all appointment from appointment collection
            var query = db.collection("Appointment")
                .whereGreaterThanOrEqualTo(
                    "appointmentDate",
                    com.google.firebase.Timestamp(startDate)
                )
                .whereLessThanOrEqualTo(
                    "appointmentDate",
                    com.google.firebase.Timestamp(endDate)
                )

            // Add department filter if a specific department is selected
            if (department != "All Department") {
                if (doctorIds.isNotEmpty()) {
                    query = query.whereIn("doctorId", doctorIds)
                } else {
                    query =
                        query.whereEqualTo("doctorId", "impossible_value_that_will_never_exist")
                }
            }

            val snapshot = query.get().await()

            // Process the results
            var total = 0
            var completed = 0
            var cancelled = 0

            for (document in snapshot.documents) {
                total++
                when (document.getString("status")) {
                    "Completed" -> completed++
                    "Cancelled" -> cancelled++
                }
            }

            return AppointmentStatistics(
                total = total,
                completed = completed,
                cancelled = cancelled
            )

        } catch (e: Exception) {
            Log.e("FirestoreQuery", "Error getting appointment statistics", e)
            return AppointmentStatistics()
        }
    }

    // This helper function gets the doctor IDs for a given specialization
    private suspend fun getDoctorIdsForDepartment(departmentName: String): List<String> {
        return try {
            val snapshot = db.collection("Doctor")
                .whereEqualTo("Specialization", departmentName)
                .get()
                .await()
            val doctorIds = snapshot.documents.mapNotNull { it.getString("DoctorID") }
            Log.d("FirestoreQuery", "Department: '$departmentName', Found DoctorIDs: $doctorIds")
            doctorIds
        } catch (e: Exception) {
            Log.e("FirestoreQuery", "Error getting doctors for department: $departmentName", e)
            emptyList()
        }
    }

    private fun getStartAndEndDates(
        type: String,
        startDateStr: String,
        endDateStr: String
    ): Pair<Date?, Date?> {
        val calendar = Calendar.getInstance()
        return when (type) {
            "Weekly" -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val startOfWeek = calendar.time
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val endOfWeek = calendar.time
                Pair(startOfWeek.atStartOfDay(), endOfWeek.atEndOfDay())
            }

            "Monthly" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startOfMonth = calendar.time
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                val endOfMonth = calendar.time
                Pair(startOfMonth.atStartOfDay(), endOfMonth.atEndOfDay())
            }

            "Yearly" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val startOfYear = calendar.time
                calendar.add(Calendar.YEAR, 1)
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val endOfYear = calendar.time
                Pair(startOfYear.atStartOfDay(), endOfYear.atEndOfDay())
            }

            "Custom Date Range" -> {
                val format = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                try {
                    val customStart = format.parse(startDateStr)
                    val customEnd = format.parse(endDateStr)
                    Pair(customStart?.atStartOfDay(), customEnd?.atEndOfDay())
                } catch (e: Exception) {
                    Pair(null, null)
                }
            }

            else -> Pair(null, null)
        }
    }

    private fun Date.atStartOfDay(): Date {
        val calendar = Calendar.getInstance()
        calendar.time = this
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun Date.atEndOfDay(): Date {
        val calendar = Calendar.getInstance()
        calendar.time = this
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    override suspend fun getPeakHoursReportData(
        type: String,
        department: String,
        startDateStr: String,
        endDateStr: String
    ): PeakHoursReportData {
        val (startDate, endDate) = getStartAndEndDates(type, startDateStr, endDateStr)
        if (startDate == null || endDate == null) {
            return PeakHoursReportData()
        }

        try {
            // Get the list of doctor IDs for the selected department, if any.
            val doctorIds = if (department != "All Department") {
                getDoctorIdsForDepartment(department)
            } else {
                emptyList()
            }

            val chartData: List<com.example.qlinic.data.model.ChartData>
            var busiestTime = "No Data"

            // Get slot IDs for those doctors if filtering by department
            val slotIds = if (department != "All Department" && doctorIds.isNotEmpty()) {
                val slotSnapshot = db.collection("Slot")
                    .whereIn("DoctorID", doctorIds)
                    .get().await()
                slotSnapshot.documents.mapNotNull { it.getString("SlotID") }
            } else {
                emptyList()
            }

            val baseQuery = db.collection("Appointment")
                .whereGreaterThanOrEqualTo("appointmentDate", com.google.firebase.Timestamp(startDate))
                .whereLessThanOrEqualTo("appointmentDate", com.google.firebase.Timestamp(endDate))

            val finalQuery = if (department != "All Department" && slotIds.isNotEmpty()) {
                baseQuery.whereIn("slotId", slotIds)
            } else if (department != "All Department" && slotIds.isEmpty()) {
                baseQuery.whereEqualTo("slotId", "impossible_value")
            } else {
                baseQuery
            }

            val snapshot = finalQuery.get().await()
            val hourlyCounts = mutableMapOf<Int, Int>()

            for (document in snapshot.documents) {
                val slotId = document.getString("slotId")
                if (slotId != null) {
                    val slotDoc = db.collection("Slot").document(slotId).get().await()
                    val slotStartTime = slotDoc.getString("SlotStartTime") // e.g., "09:00"
                    if (slotStartTime != null && slotStartTime.length >= 2) {
                        val hour = slotStartTime.substring(0, 2).toIntOrNull()
                        if (hour != null) {
                            hourlyCounts[hour] = (hourlyCounts[hour] ?: 0) + 1
                        }
                    }
                }
            }

            val busiestHour = hourlyCounts.maxByOrNull { it.value }?.key
            if (busiestHour != null) {
                val endHour = busiestHour + 1
                busiestTime = String.format(Locale.US, "%02d:00 - %02d:00", busiestHour, endHour)
            }

            when (type) {
                "Weekly", "Custom Date Range" -> {
                    val dailyCounts = mutableMapOf<String, Int>()
                    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    daysOfWeek.forEach { day -> dailyCounts[day] = 0 }

                    for (document in snapshot.documents) {
                        val timestamp = document.getTimestamp("appointmentDate")
                        timestamp?.let {
                            val cal = Calendar.getInstance()
                            cal.time = it.toDate()
                            val dayIndex = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                            val dayLabel = daysOfWeek[dayIndex]
                            dailyCounts[dayLabel] = (dailyCounts[dayLabel] ?: 0) + 1
                        }
                    }
                    chartData = daysOfWeek.map {
                        com.example.qlinic.data.model.ChartData(
                            value = (dailyCounts[it] ?: 0).toFloat(),
                            label = it
                        )
                    }
                }

                "Monthly" -> {
                    // For monthly, we group appointments by week of the month.
                    val weeklyCounts = IntArray(4) { 0 }
                    for (document in snapshot.documents) {
                        val timestamp = document.getTimestamp("appointmentDate")
                        timestamp?.let {
                            val cal = Calendar.getInstance()
                            cal.time = it.toDate()
                            val weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH) - 1 // 0-based
                            if (weekOfMonth in 0..3) {
                                weeklyCounts[weekOfMonth]++
                            }
                        }
                    }
                    val labels = listOf("Week 1", "Week 2", "Week 3", "Week 4")
                    chartData = labels.mapIndexed { index, label ->
                        com.example.qlinic.data.model.ChartData(
                            value = weeklyCounts[index].toFloat(),
                            label = label
                        )
                    }
                }

                "Yearly" -> {
                    val monthlyCounts = IntArray(12) { 0 }
                    for (document in snapshot.documents) {
                        val timestamp = document.getTimestamp("appointmentDate")
                        timestamp?.let {
                            val cal = Calendar.getInstance()
                            cal.time = it.toDate()
                            val monthIndex = cal.get(Calendar.MONTH) // 0=Jan, 1=Feb, etc.
                            if (monthIndex in 0..11) {
                                monthlyCounts[monthIndex]++
                            }
                        }
                    }
                    val labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    chartData = labels.mapIndexed { index, label ->
                        com.example.qlinic.data.model.ChartData(
                            value = monthlyCounts[index].toFloat(),
                            label = label
                        )
                    }
                }

                else -> {
                    chartData = emptyList()
                }
            }

            val busiestDay = chartData.maxByOrNull { it.value }?.label ?: "No Data"
            return PeakHoursReportData(
                chartData = chartData,
                busiestDay = busiestDay,
                busiestTime = busiestTime
            )
        } catch (e: Exception) {
            Log.e("FirestoreQuery", "Error getting peak hours report data", e)
            return PeakHoursReportData()
        }
    }
}
