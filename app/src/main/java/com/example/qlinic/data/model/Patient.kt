package com.example.qlinic.data.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

// Added default values to all properties and corrected DateOfBirth type
// to ensure Firestore deserialization works correctly.
data class Patient(
    @get:PropertyName("PatientID") @set:PropertyName("PatientID") var patientId: String = "",
    @get:PropertyName("Email") @set:PropertyName("Email") var email: String = "",
    @get:PropertyName("Gender") @set:PropertyName("Gender") var gender: String = "",
    @get:PropertyName("ImageUrl") @set:PropertyName("ImageUrl") var imageUrl: String = "",
    @get:PropertyName("FirstName") @set:PropertyName("FirstName") var firstName: String = "",
    @get:PropertyName("LastName") @set:PropertyName("LastName") var lastName: String = "",
    @get:PropertyName("IC") @set:PropertyName("IC") var ic: String = "",
    @get:PropertyName("PhoneNumber") @set:PropertyName("PhoneNumber") var phoneNumber: String = "",
    @get:PropertyName("fcmToken") @set:PropertyName("fcmToken") var fcmToken: String = "",
    @get:PropertyName("DateOfBirth") @set:PropertyName("DateOfBirth") var dateOfBirth: Date? = null
)
