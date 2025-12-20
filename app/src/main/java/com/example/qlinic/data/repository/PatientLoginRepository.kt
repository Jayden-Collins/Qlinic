package com.example.qlinic.data.repository

import android.util.Log
import com.example.qlinic.data.model.Patient
import com.example.qlinic.data.model.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


class PatientRepository(
    private val sessionManager: SessionManager? = null,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    // Keep your existing signup method

    // Return the logged in patientId or throw an exception with a message
    suspend fun loginPatient(
        identifier: String, // Can be email or NRIC
        password: String
    ): String {
        try {
            val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(identifier).matches()

            if (isEmail) {
                val result = auth.signInWithEmailAndPassword(identifier, password).await()
                val userId = auth.currentUser?.uid
                return userId ?: throw Exception("Failed to get user ID")
            } else {
                val querySnapshot = db.collection("Patient")
                    .whereEqualTo("IC", identifier)
                    .limit(1)
                    .get()
                    .await()

                if (querySnapshot.isEmpty) {
                    throw Exception("NRIC not found")
                }

                val document = querySnapshot.documents[0]
                val email = document.getString("Email") ?: ""
                if (email.isEmpty()) throw Exception("Email not found for this NRIC")

                auth.signInWithEmailAndPassword(email, password).await()
                val userId = auth.currentUser?.uid
                return userId ?: throw Exception("Failed to get user ID")
            }
        } catch (e: Exception) {
            // Map Firebase exceptions to clearer messages if needed
            val msg = when (e) {
                is FirebaseAuthInvalidUserException -> "User not found"
                is FirebaseAuthInvalidCredentialsException -> "Invalid password"
                else -> e.message ?: "Login failed"
            }
            throw Exception(msg)
        }
    }

    suspend fun getPatientById(patientId: String): Patient? {
        return try {
            val document = db.collection("Patient").document(patientId).get().await()
            if (document.exists()) {
                document.toObject(Patient::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error getting patient: ${e.message}")
            null
        }
    }
}