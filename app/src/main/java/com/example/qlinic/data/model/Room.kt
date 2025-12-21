package com.example.qlinic.data.model

import com.google.firebase.firestore.DocumentReference

data class Room(
    val ref: DocumentReference, // room id
    val roomNumber: String, // room number
)
