package com.example.qlinic.data.model

data class Patient(
    val patientID: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val ic: String = "",           // NRIC
    val email: String = "",
    val phoneNumber: String = "",
    val gender: String = "",
    val photoUrl: String = ""
)