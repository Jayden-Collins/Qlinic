package com.example.qlinic.data.model

enum class StaffRole {
    DOCTOR,
    FRONT_DESK,

}

data class ClinicStaff(
    val staffId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val role: StaffRole = StaffRole.FRONT_DESK,
    val firebaseUid: String = ""
)

