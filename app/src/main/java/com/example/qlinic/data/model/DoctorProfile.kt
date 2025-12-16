package com.example.qlinic.data.model

data class DoctorProfile(
    val id: String,
    val name: String,
    val specialty: String,
    val room: String,
    val imageUrl: String?,
    val description: String, // "About me"
    val yearsOfExp: Int = 0,
    val isAvailable: Boolean = true
)