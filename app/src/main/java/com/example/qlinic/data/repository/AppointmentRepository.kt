package com.example.qlinic.data.repository

import android.R.attr.factor
import androidx.compose.ui.graphics.Color
import com.example.qlinic.data.model.AppointmentStatistics
import com.example.qlinic.data.model.ChartData
import com.example.qlinic.data.model.PeakHoursReportData
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random
import kotlin.text.toFloat

class AppointmentRepository {

    private val dailyAppointmentsRange = 12..16

    private val departmentDistribution = mapOf(
        "Cardiology" to 0.20,      // 20%
        "Dermatology" to 0.15,     // 15%
        "Gastroenterology" to 0.10,  // 10%
        "Gynecologist" to 0.25,    // 25%
        "Neurology" to 0.15,       // 15%
        "Orthopedics" to 0.15      // 15%
        // Total = 1.0 (100%)
    )

    suspend fun getStatistics(
        type: String,
        department: String,
        startDate: String,
        endDate: String
    ): AppointmentStatistics {
        delay(500) // Simulate network latency


        // 1. First, calculate the TOTAL appointments as if for "All Department".
        val allDepartmentsTotal: Int
        if (type == "Weekly") {
            allDepartmentsTotal = 100
        } else if (type == "Monthly") {
            allDepartmentsTotal = 450
        } else if (type == "Yearly") {
            allDepartmentsTotal = 5400
        } else if (type == "Custom Date Range") {
            val numDays = getNumberOfDays(startDate, endDate)
            val appointmentsPerDay = dailyAppointmentsRange.random()
            allDepartmentsTotal = appointmentsPerDay * numDays
        } else {
            return AppointmentStatistics() // Return empty for unknown type
        }

        // 2. Now, determine the final total based on the selected department.
        val finalTotal = if (department == "All Department") {
            allDepartmentsTotal
        } else {
            // Get the distribution factor for the selected department, default to 0 if not found.
            val factor = departmentDistribution[department] ?: 0.0
            (allDepartmentsTotal * factor).toInt()
        }

        // 3. Calculate completed and cancelled based on this final, correct total.
        val cancellationRate = (2..6).random() / 100.0
        val cancelled = (finalTotal * cancellationRate).toInt()
        val completed = finalTotal - cancelled

        return AppointmentStatistics(
            total = finalTotal,
            completed = completed,
            cancelled = cancelled
        )
    }

    /**
     * Simulates fetching data for the peak hours bar chart.
     * It now generates a different, appropriate chart for each filter type.
     */
    suspend fun getPeakHoursReportData(
        type: String,
        department: String,
        startDate: String,
        endDate: String
    ): PeakHoursReportData {
        delay(600) // Simulate network latency

        val departmentFactor = if (department == "All Department") {
            1.0
        } else {
            departmentDistribution[department] ?: 0.0
        }

        val data: List<ChartData>
        val busiestDay: String
        val busiestTime: String

        // Use a 'when' block to generate different chart data based on the filter type.
        when (type) {
            "Weekly", "Custom Date Range" -> {
                // For weekly or custom, show a 7-day bar chart.
                val weeklyData = generateRandomChartData(7, listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), departmentFactor)
                data = weeklyData
                busiestDay = findBusiestDay(weeklyData)
                busiestTime = "10 AM - 11 AM" // Hard-coded for simplicity
            }
            "Monthly" -> {
                // For monthly, show a 4-week bar chart.
                val monthlyData = generateRandomChartData(4, listOf("Week 1", "Week 2", "Week 3", "Week 4"), departmentFactor)
                data = monthlyData
                busiestDay = findBusiestDay(monthlyData)
                busiestTime = "2 PM - 3 PM"
            }
            "Yearly" -> {
                // For yearly, show a 12-month bar chart.
                val yearlyData = generateRandomChartData(12, listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"), departmentFactor)
                data = yearlyData
                busiestDay = findBusiestDay(yearlyData)
                busiestTime = "9 AM - 10 AM"
            }
            else -> {
                data = emptyList()
                busiestDay = "No Data"
                busiestTime = "No Data"
            }
        }

        return PeakHoursReportData(
            chartData = data,
            busiestDay = busiestDay,
            busiestTime = busiestTime
        )
    }

    // --- HELPER FUNCTIONS ---

    private fun getNumberOfDays(startDateStr: String, endDateStr: String): Int {
        val format = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        try {
            val startDate = format.parse(startDateStr)
            val endDate = format.parse(endDateStr)

            if (startDate != null && endDate != null) {
                val diffInMillis = abs(endDate.time - startDate.time)
                val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                // Return at least 1 day if the same day is selected.
                return if (diffInDays == 0) 1 else diffInDays + 1
            }
        } catch (e: Exception) {
            // If parsing fails, return a default value.
            return 1
        }
        return 1
    }

    private fun generateRandomChartData(count: Int, labels: List<String>, factor: Double): List<ChartData> {
        val data = mutableListOf<ChartData>()
        repeat(count) { index ->
            val value = ((10..100).random() * factor).toFloat()
            data.add(
                ChartData(
                    value = value,
                    label = labels.getOrElse(index) { "L$index" },
                    color = Color(0xFFB0C4DE) // Default color: LightSteelBlue
                )
            )
        }
        // Highlight the max value with a different color
        val maxValue = data.maxOfOrNull { it.value }
        val maxIndex = data.indexOfFirst { it.value == maxValue }
        if (maxIndex != -1) {
            data[maxIndex] = data[maxIndex].copy(color = Color(0xFF4682B4)) // Busiest color: SteelBlue
        }
        return data
    }

    private fun findBusiestDay(data: List<ChartData>): String {
        return data.maxByOrNull { it.value }?.label ?: "No Data"
    }
}

