package com.example.qlinic.data.model

import com.google.firebase.firestore.PropertyName

// Data class to represent a document in the clinicStaff collection.
// Using @PropertyName allows us to use camelCase variable names for snake_case fields in Firestore.
data class ClinicStaff(
    @get:PropertyName("StaffID") @set:PropertyName("StaffID") var staffId: String = "",
    @get:PropertyName("Email") @set:PropertyName("Email") var email: String = "",
    @get:PropertyName("Gender") @set:PropertyName("Gender") var gender: String = "",
    @get:PropertyName("ImageUrl") @set:PropertyName("ImageUrl") var imageUrl: String = "",
    @get:PropertyName("FirstName") @set:PropertyName("FirstName") var firstName: String = "",
    @get:PropertyName("LastName") @set:PropertyName("LastName") var lastName: String = "",
    @get:PropertyName("PhoneNumber") @set:PropertyName("PhoneNumber") var phoneNumber: String = "",
    @get:PropertyName("isActive") @set:PropertyName("isActive") var isActive: Boolean = true,
    @get:PropertyName("Role") @set:PropertyName("Role") var role: String = "",
) {
    val fullName: String get() = "$firstName $lastName"
}