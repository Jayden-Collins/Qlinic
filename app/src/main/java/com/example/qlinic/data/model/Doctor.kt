package com.example.qlinic.data.model

import com.google.firebase.firestore.DocumentReference

//data class Doctor(
//    override val StaffID : String, // staff id
//    override val email: String, // staff email
//    override val gender: String, // staff gender
//    override val firstName: String, // staff first name
//    override val lastName: String, // staff last name
//    override val imageUrl: String, // staff image url
//    override val phoneNumber: String, // staff phone number
//    val specialization: String, // specialization doctor
//    val description: String, // description doctor
//    val yearOfExp: Int, // year of experience
//    override val isActive: Boolean, // status doctor true = online, false = offline
//    val roomID: String, // reference of room for each doctor works
//) : ClinicStaff(StaffID, email, gender, firstName, lastName, imageUrl, phoneNumber, role = "Doctor", isActive)


data class Doctor(
//    val id: String, // Document ID of doctor document
    val doctorID: String, // References ClinicStaff StaffID
    val specialization: String,
    val description: String,
    val yearsOfExp: Int,
    val roomID: String
)