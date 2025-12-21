package com.example.qlinic.data.repository

import android.util.Log
import com.example.qlinic.data.model.ClinicStaff
import com.example.qlinic.data.model.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ClinicStaffRepository(
    private val sessionManager: SessionManager? = null,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    fun loginClinicStaff(
        identifier: String, // Staff ID like D001 or S001
        password: String,
        onSuccess: (role: String, staffId: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            db.collection("ClinicStaff")
                .whereEqualTo("StaffID", identifier.uppercase())
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        onFailure("Staff ID not found")
                        return@addOnSuccessListener
                    }

                    val document = documents.documents[0]
                    val email = document.getString("Email") ?: ""
                    val role = document.getString("Role") ?: ""
                    val staffDocId = document.id

                    if (email.isEmpty()) {
                        onFailure("Email not found for this staff ID")
                        return@addOnSuccessListener
                    }

                    if (role.isEmpty()) {
                        onFailure("Role not defined for this staff member")
                        return@addOnSuccessListener
                    }

                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onSuccess(role, staffDocId)
                            } else {
                                val error = task.exception
                                when (error) {
                                    is FirebaseAuthInvalidUserException -> onFailure("User not found")
                                    is FirebaseAuthInvalidCredentialsException -> onFailure("Invalid password")
                                    else -> onFailure("Login failed: ${error?.message}")
                                }
                            }
                        }
                }
                .addOnFailureListener {
                    onFailure("Error finding staff by ID")
                }
        } catch (e: Exception) {
            onFailure("Login failed: ${e.message}")
        }
    }

    suspend fun getStaffMember(staffId: String): ClinicStaff? {
        return try {
            val document = db.collection("ClinicStaff").document(staffId).get().await()
            if (document.exists()) {
                document.toObject(ClinicStaff::class.java)?.apply {
                    this.staffId = document.id
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ClinicStaffRepository", "Error getting staff: ${e.message}")
            null
        }
    }
}
