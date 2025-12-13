package com.example.qlinic.data.repository

import androidx.compose.ui.graphics.Color
import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AppointmentStatistics
import com.example.qlinic.data.model.ChartData
import com.example.qlinic.data.model.PeakHoursReportData
import com.example.qlinic.data.model.Slot
import com.example.qlinic.utils.getDayStartAndEnd
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.Date

class AppointmentRepository {
    private val db = Firebase.firestore
    private val appointmentsCollection = db.collection("Appointment")

    suspend fun bookAppointment(patientId: String, slot: Slot, date: Date): Boolean {
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
                    slotId = slot.SlotID,
                    patientId = patientId,
                )
                appointmentsCollection.document(appointmentId).set(newAppointment).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Simulate a network call
    suspend fun getStatistics(type: String, department: String): AppointmentStatistics {
        delay(500) // Simulate network latency

        return if (type == "Custom Date Range") {
            AppointmentStatistics(total = 1000, completed = 950, cancelled = 50)
        } else if (type == "Weekly") {
            AppointmentStatistics(total = 50, completed = 48, cancelled = 2)
        } else if (type == "Monthly") {
            AppointmentStatistics(total = 200, completed = 190, cancelled = 10)
        } else if (type == "Yearly") {
            AppointmentStatistics(total = 1000, completed = 950, cancelled = 50)
        } else {
            AppointmentStatistics(total = 0, completed = 0, cancelled = 0)
        }
    }

    // Simulate a network call to get peak hours data
    /**
    suspend fun getPeakHoursInfo(type: String, department: String): PeakHoursInfo {
        delay(600) // Simulate slightly different network latency

        // Return different hard-coded data based on the filter type
        return when (type) {
            "Weekly" -> PeakHoursInfo(busiestDay = "Wednesday", busiestTime = "10 AM - 11 AM")
            "Monthly" -> PeakHoursInfo(busiestDay = "Friday", busiestTime = "2 PM - 3 PM")
            "Yearly" -> PeakHoursInfo(busiestDay = "Tuesday", busiestTime = "9 AM - 10 AM")
            "Custom Date Range" -> PeakHoursInfo(busiestDay = "Monday", busiestTime = "3 PM - 4 PM")
            else -> PeakHoursInfo()
        }
    }
    **/

    suspend fun getPeakHoursReportData(type: String, department: String): PeakHoursReportData {
        delay(600) // Simulate network latency

        // In a real app, you would calculate the busiest day/time from the data.
        // For this example, we'll hard-code it.
        val chartData = listOf(
            ChartData(value = 12f, label = "Mon", color = Color(0xFFB0C4DE)),
            ChartData(value = 18f, label = "Tue", color = Color(0xFFB0C4DE)),
            ChartData(value = 25f, label = "Wed", color = Color(0xFF4682B4)), // Busiest
            ChartData(value = 15f, label = "Thu", color = Color(0xFFB0C4DE)),
            ChartData(value = 22f, label = "Fri", color = Color(0xFFB0C4DE)),
            ChartData(value = 8f, label = "Sat", color = Color(0xFFB0C4DE))
        )

        return PeakHoursReportData(
            chartData = chartData,
            busiestDay = "Wednesday",
            busiestTime = "10 AM - 11 AM"
        )
    }
}