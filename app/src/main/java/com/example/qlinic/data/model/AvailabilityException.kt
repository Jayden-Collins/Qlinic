package com.example.qlinic.data.model

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.Timestamp

data class AvailabilityException(
    val exceptionID: String, // availability exception id
    val startDateTime: Timestamp, // start date and time of exception
    val endDateTime: Timestamp, // end date and time of exception
    val reason: String, // reason for the exception
    val doctorID: String, // reference to the associated doctor
    val staffID: String, // reference to the associated staff who created the exception
)
