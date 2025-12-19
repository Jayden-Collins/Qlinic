package com.example.qlinic.data.model

import com.google.firebase.firestore.DocumentReference

data class Patient(
    val id: String, // patient id
//    val refID: DocumentReference, // patient id
    val email: String, // patient email
    val gender: String, // gender
    val firstName: String, // first name
    val lastName: String, // last name
    val ic: String, // identification card number
    val phoneNumber: String // phone number
)
