package com.example.qlinic.data.model

import com.google.firebase.Timestamp

data class Notification(
    val notifId: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: Timestamp? = null,
    val type: String = "",
    val appointmentId: String = ""
)
