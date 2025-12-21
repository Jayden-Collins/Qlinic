package com.example.qlinic.data.repository

import com.example.qlinic.data.model.Doctor
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface DoctorRepository {
    suspend fun getAllDoctors(): List<Doctor>
    suspend fun getDoctorById(id: String): Doctor?
}

class FirestoreDoctorRepository : DoctorRepository {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun getAllDoctors(): List<Doctor> {
        return try {
            val doctorSnapshot = db.collection("Doctor").get().await()

            val profiles = mutableListOf<Doctor>()

            for (doc in doctorSnapshot.documents) {
                val docId = doc.getString("DoctorID") ?: ""
                val room = doc.getString("RoomID") ?: ""
                val specialty = doc.getString("Specialization") ?: ""
                val desc = doc.getString("Description") ?: ""
                val exp = doc.getLong("YearsOfExp")?.toInt() ?: 0

                if (docId.isNotEmpty()) {
                    val staffSnap = db.collection("ClinicStaff")
                        .whereEqualTo("StaffID", docId)
                        .get()
                        .await()

                    if (!staffSnap.isEmpty) {
                        val staffDoc = staffSnap.documents[0]
                        val firstName = staffDoc.getString("FirstName") ?: ""
                        val lastName = staffDoc.getString("LastName") ?: ""
                        val imageUrl = staffDoc.getString("ImageUrl")

                        profiles.add(
                            Doctor(
                                id = docId,
                                specialization = specialty,
                                room = room,
                                description = desc,
                                yearsOfExp = exp
                            )
                        )
                    }
                }
            }
            profiles
        } catch (e: Exception) {
            Log.e("FirestoreDoctorRepo", "Error fetching doctors", e)
            emptyList()
        }
    }

    override suspend fun getDoctorById(id: String): Doctor? {
        // Fetch all and find one (Optimized way would be direct queries)
        return getAllDoctors().find { it.id == id }
    }
}