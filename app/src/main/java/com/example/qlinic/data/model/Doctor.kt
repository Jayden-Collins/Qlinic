package com.example.qlinic.data.model

data class Doctor(
    val id: String,
    val specialization: String,
    val description: String, // "About me"
    val yearsOfExp: Int = 0,
    val room: String,
)