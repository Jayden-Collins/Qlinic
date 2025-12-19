package com.example.qlinic.data.model

data class SpecificDoctorInfo(
    val clinicStaff: ClinicStaff,
    val doctor: Doctor,
){
    val fullName: String get() = clinicStaff.fullName
    val email: String get() = clinicStaff.email
    val phoneNumber: String get() = clinicStaff.phoneNumber
    val imageUrl: String get() = clinicStaff.imageUrl
    val specialization: String get() = doctor?.specialization ?: "General"
    val displayName: String get() = "Dr. $fullName"
    val id: String get() = doctor.doctorID
}
