package com.example.qlinic.data.repository

import com.example.qlinic.data.model.AppointmentStatistics
import kotlinx.coroutines.delay

class AppointmentRepository {

    // Simulate a network call
    suspend fun getStatistics(type: String, department: String): AppointmentStatistics {
        delay(500) // Simulate network latency

        return if (type == "Custom Date Range") {
            AppointmentStatistics(total = 500, completed = 488, cancelled = 12)
        } else {
            AppointmentStatistics(total = 50, completed = 48, cancelled = 2)
        }
    }
}