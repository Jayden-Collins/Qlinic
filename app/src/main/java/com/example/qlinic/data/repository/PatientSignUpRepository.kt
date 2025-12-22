package com.example.qlinic.data.repository

import android.util.Log
import android.util.Patterns
import com.example.qlinic.data.model.Patient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PatientSignUpRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val collectionName: String = "Patient"
) {

    private fun isValidEmail(email: String?): Boolean {
        return !email.isNullOrBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun signupPatient(
        patient: Patient,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isValidEmail(patient.email)) {
            onFailure("Invalid email format")
            return
        }

        Log.d("SignupDebug", "Starting signup for: ${patient.email}")


        db.collection(collectionName)
            .whereEqualTo("FirstName", patient.firstName)
            .whereEqualTo("LastName", patient.lastName)
            .limit(1)
            .get()
            .addOnSuccessListener { usernameDocs ->
                if (!usernameDocs.isEmpty) {
                    onFailure("Name already exists"); return@addOnSuccessListener
                }
                db.collection(collectionName)
                    .whereEqualTo("Email", patient.email)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { emailDocs ->
                        if (!emailDocs.isEmpty) {
                            onFailure("Email already exists"); return@addOnSuccessListener
                        }
                        db.collection(collectionName)
                            .whereEqualTo("PhoneNumber", patient.phoneNumber)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { phoneDocs ->
                                if (!phoneDocs.isEmpty) {
                                    onFailure("Phone number already exists"); return@addOnSuccessListener
                                }
                                db.collection(collectionName)
                                    .whereEqualTo("IC", patient.ic)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener { icDocs ->
                                        if (!icDocs.isEmpty) {
                                            onFailure("NRIC already exists"); return@addOnSuccessListener
                                        }
                                        // all clear -> create auth and save doc
                                        auth.createUserWithEmailAndPassword(patient.email, password)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    val userId = auth.currentUser?.uid
                                                    if (userId == null) {
                                                        onFailure("Failed to obtain user id"); return@addOnCompleteListener
                                                    }
                                                    val map = hashMapOf(
                                                        "FirstName" to patient.firstName,
                                                        "LastName" to patient.lastName,
                                                        "IC" to patient.ic,
                                                        "Email" to patient.email,
                                                        "PhoneNumber" to patient.phoneNumber,
                                                        "Gender" to patient.gender,
                                                        "ImageUri" to ""
                                                    )
                                                    db.collection(collectionName).document(userId)
                                                        .set(map)
                                                        .addOnSuccessListener { onSuccess() }
                                                        .addOnFailureListener { onFailure("Failed to save user data") }
                                                } else {
                                                    onFailure("Signup Failed: ${task.exception?.message}")
                                                }
                                            }
                                    }
                                    .addOnFailureListener { onFailure("Error checking NRIC") }
                            }
                            .addOnFailureListener { onFailure("Error checking phone") }
                    }
                    .addOnFailureListener { onFailure("Error checking email") }
            }
            .addOnFailureListener { onFailure("Error checking name") }
    }
}

