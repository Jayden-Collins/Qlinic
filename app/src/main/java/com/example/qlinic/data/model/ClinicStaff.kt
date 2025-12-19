package com.example.qlinic.data.model

import com.google.firebase.firestore.DocumentReference

//sealed class ClinicStaff(
//    open val StaffID: String, // staff id
//    open val email: String, // staff email
//    open val gender: String, // gender
//    open val firstName: String, // first name
//    open val lastName: String, // last name
//    open val imageUrl: String, // staff image url
//    open val phoneNumber: String, // phone number
//    val role: String,// staff role (e.g., "DOCTOR", "NURSE")
//    open val isActive: Boolean = true // staff active status
//)

data class ClinicStaff(
//    val id: String, // Document ID (auto-generated)
    val staffID: String, // Custom StaffID field (same as id?)
    val email: String,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val imageUrl: String,
    val phoneNumber: String,
    val role: String,
    val isActive: Boolean
) {
    val fullName: String get() = "$firstName $lastName"
}