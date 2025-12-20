package com.example.qlinic.data.repository

import android.util.Log
import com.example.qlinic.data.model.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

data class PatientProfile(
    val firstName: String = "",
    val lastName: String? = null,
    val imageUrl: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val gender: String? = null
)

data class ClinicStaffProfile(
    val email: String = "",
    val firstName: String = "",
    val lastName: String? = null,
    val gender: String? = null,
    val imageUrl: String? = null,
    val phoneNumber: String? = null,
    val isActive: Boolean = false,
    val staffId: String = ""
)

data class DoctorDetails(
    val id: String = "",
    val description: String? = null,
    val specialization: String? = null,
    val yearsOfExp: Int? = null
)

class ProfileRepository(
    private val sessionManager: SessionManager? = null // Make it optional for now
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val patientColl = firestore.collection("Patient")
    private val staffColl = firestore.collection("ClinicStaff")
    private val doctorColl = firestore.collection("Doctor")
    private val auth = FirebaseAuth.getInstance()

    fun signOut() {
        // 1. Clear Firebase Auth
        auth.signOut()

        // 2. Clear local session if sessionManager is available
        sessionManager?.logout()

        Log.d("ProfileRepository", "User logged out successfully")
    }


    // helper: try multiple possible field names
    private fun DocumentSnapshot.getStringAny(vararg keys: String): String? {
        for (k in keys) {
            val v = this.getString(k)
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    fun fetchPatient(userId: String?, onResult: (PatientProfile?) -> Unit) {
        val id = userId?.takeIf { it.isNotBlank() } ?: auth.currentUser?.uid?.takeIf { it.isNotBlank() }
        if (id.isNullOrBlank()) {
            onResult(null); return
        }

        patientColl.document(id).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onResult(null)
                    return@addOnSuccessListener
                }
                val firstName = doc.getString("FirstName") ?: ""
                val lastName = doc.getString("LastName")
                val imageUrl = doc.getString("ImageUrl")
                val email = doc.getString("Email")
                val phone = doc.getString("PhoneNumber")
                val gender = doc.getString("Gender")
                onResult(
                    PatientProfile(
                        firstName = firstName,
                        lastName = lastName,
                        imageUrl = imageUrl,
                        email = email,
                        phoneNumber = phone,
                        gender = gender
                    )
                )
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun fetchClinicStaff(
        staffId: String?,
        expectedRole: String? = null,
        onResult: (ClinicStaffProfile?) -> Unit
    ) {
        val id = staffId?.takeIf { it.isNotBlank() }
        if (id.isNullOrBlank()) {
            Log.d("ProfileRepository", "fetchClinicStaff: staffId is missing or blank (no auth UID fallback)")
            onResult(null); return
        }

        staffColl.document(id).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Log.d("ProfileRepository", "fetchClinicStaff: doc not found for id=$id")
                    onResult(null); return@addOnSuccessListener
                }

                // --- Updated role handling ---
                val actualRole = doc.getString("Role")?.lowercase()
                val allowedRoles = when (expectedRole?.lowercase()) {
                    "doctor" -> listOf("doctor")
                    "staff" -> listOf("staff", "front_desk")  // accept both "staff" and "Front_desk"
                    else -> listOf()
                }

                if (actualRole !in allowedRoles) {
                    Log.d(
                        "ProfileRepository",
                        "fetchClinicStaff: role mismatch for id=$id actualRole=$actualRole expected=$allowedRoles"
                    )
                    onResult(null)
                    return@addOnSuccessListener
                }

                val email = doc.getStringAny("Email", "email") ?: ""
                val firstName = doc.getStringAny("FirstName", "Firstname", "first_name", "firstName") ?: ""
                val lastName = doc.getStringAny("LastName", "Lastname", "last_name", "lastName")
                val gender = doc.getStringAny("Gender", "gender")
                val imageUrl = doc.getStringAny("ImageUrl", "image_url", "photoUrl", "photo", "photo_url")
                val phone = doc.getStringAny("PhoneNumber", "Phone", "phoneNumber", "phone")
                val isActive = doc.getBoolean("isActive") ?: doc.getBoolean("IsActive") ?: false

                onResult(
                    ClinicStaffProfile(
                        email = email,
                        firstName = firstName,
                        lastName = lastName,
                        gender = gender,
                        imageUrl = imageUrl,
                        phoneNumber = phone,
                        isActive = isActive,
                        staffId = id
                    )
                )
            }
            .addOnFailureListener {
                Log.d("ProfileRepository", "fetchClinicStaff: failure for id=$staffId -> ${it.message}")
                onResult(null)
            }
    }

    // Require explicit doctor id (do NOT fallback to auth UID)
    fun fetchDoctor(doctorId: String?, onResult: (DoctorDetails?) -> Unit) {
        val id = doctorId?.takeIf { it.isNotBlank() }
        if (id.isNullOrBlank()) {
            onResult(null); return
        }

        doctorColl.document(id).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) { onResult(null); return@addOnSuccessListener }
                val desc = doc.getString("Description")
                val spec = doc.getString("Specialization")
                val years = doc.getLong("YearsOfExp")?.toInt()
                onResult(DoctorDetails(description = desc, specialization = spec, yearsOfExp = years))
            }
            .addOnFailureListener { onResult(null) }
    }
}


