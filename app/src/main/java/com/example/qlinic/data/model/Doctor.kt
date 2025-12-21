package com.example.qlinic.data.model

data class Doctor(
    val doctorID: String, // References ClinicStaff StaffID
    val specialization: String,
    val description: String,
    val yearsOfExp: Int,
    val roomID: String
)