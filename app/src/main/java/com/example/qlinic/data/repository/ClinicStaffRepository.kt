package com.example.qlinic.data.repository

import com.example.qlinic.data.model.ClinicStaff
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class ClinicStaffRepository {
    private val db = Firebase.firestore

    // Fetches a single staff member by their ID
    suspend fun getStaffMember(staffId: String): ClinicStaff? {
        return try {
            // The document ID in clinicStaff is the same as the doctorId
            val document = db.collection("ClinicStaff").document(staffId).get().await()
            document.toObject(ClinicStaff::class.java)
        } catch (e: Exception) {
            println("Error fetching staff member: ${e.message}")
            null
        }
    }
}
