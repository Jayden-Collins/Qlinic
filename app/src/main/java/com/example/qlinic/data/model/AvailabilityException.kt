package com.example.qlinic.data.model

import java.util.Date

data class AvailabilityException(
    val exceptionID: String = "",
    val startDateTime: Date = Date(),
    val endDateTime: Date = Date(),
    val reason: String = "",
    val doctorID: String = "",
    val staffID: String = ""
)
