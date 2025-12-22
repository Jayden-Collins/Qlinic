package com.example.qlinic.data.repository

import com.example.qlinic.data.model.ClinicStaff
import com.example.qlinic.data.model.Doctor
import com.example.qlinic.data.model.SpecificDoctorInfo
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

// repository for DoctorSchedule
class DoctorSchedule {
    // fetch all doctor
    suspend fun fetchDoctors(): List<SpecificDoctorInfo>{
        // get all doctors from firestore

        return try {
            val staff = Firebase.firestore
                .collection("ClinicStaff")
                .whereEqualTo("Role", "Doctor")
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val doctorList = mutableListOf<SpecificDoctorInfo>()

            // for each doctor document, map to doctor object and add to list
            for (staffDoc in staff.documents){
                try {
                    // Map to ClinicStaff
                    val clinicStaff = ClinicStaff(
                        email = staffDoc.getString("Email") ?: "",
                        firstName = staffDoc.getString("FirstName") ?: "",
                        gender = staffDoc.getString("Gender") ?: "",
                        imageUrl = staffDoc.getString("ImageUrl") ?: "",
                        lastName = staffDoc.getString("LastName") ?: "",
                        phoneNumber = staffDoc.getString("PhoneNumber") ?: "",
                        staffId = staffDoc.getString("StaffID") ?: staffDoc.id,
                        role = staffDoc.getString("Role") ?: "",
                        isActive = staffDoc.getBoolean("isActive") ?: true
                    )
                    // find matching doctor info
                    val staffID = clinicStaff.staffId
                    val doctorInfo = findDoctorInfo(staffID)

                    if (doctorInfo != null){
                        doctorList.add(SpecificDoctorInfo(clinicStaff, doctorInfo))
                    } else {
                        println("No doctor info found for StaffID: $staffID")
                    }

                } catch (e: Exception) {
                    println("Error processing staff document ${staffDoc.id}: ${e.message}")
                }
            }
            doctorList
        } catch (e: Exception) {
            println("Error fetching doctors: ${e.message}")
            emptyList()
        }
    }

    private suspend fun findDoctorInfo(staffID: String): Doctor? {
        return try {
            // Query Doctor collection where DoctorID = staffID
            val doctorSnapshot = Firebase.firestore
                .collection("Doctor") // Assuming there's a Doctor collection
                .whereEqualTo("DoctorID", staffID)
                .get()
                .await()

            if (doctorSnapshot.documents.isNotEmpty()) {
                val doctorDoc = doctorSnapshot.documents[0]

                Doctor(
                    description = doctorDoc.getString("Description") ?: "",
                    doctorID = doctorDoc.getString("DoctorID") ?: "",
                    roomID = doctorDoc.getString("RoomID") ?: "",
                    specialization = doctorDoc.getString("Specialization") ?: "",
                    yearsOfExp = (doctorDoc.getLong("YearsOfExp") ?: 0L).toInt(),
                )
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error fetching doctor info for StaffID $staffID: ${e.message}")
            null
        }
    }
}

