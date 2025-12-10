package com.example.qlinic.data.model

import com.google.firebase.firestore.Exclude

data class AppointmentStatistics(
    var total: Int = 0,
    var completed: Int = 0,
    var cancelled: Int = 0
) {
    constructor() : this(0, 0, 0)

    @get:Exclude
    val completedPercent: String
        get() = calculatePercent(completed)

    @get:Exclude
    val cancelledPercent: String
        get() = calculatePercent(cancelled)

    private fun calculatePercent(value: Int): String {
        if (total == 0) return "0 %"
        val percent = (value.toFloat() / total.toFloat()) * 100
        return if (percent % 1.0 == 0.0) "${percent.toInt()} %" else String.format("%.1f %%", percent)
    }
}
