package com.example.qlinic.data.repository

import android.net.Uri
import com.example.qlinic.data.model.ClinicStaff
import com.example.qlinic.data.model.Doctor
import com.example.qlinic.data.model.Patient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class EditProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val patientsRef = firestore.collection("Patient")
    private val staffRef = firestore.collection("ClinicStaff")
    private val doctorRef = firestore.collection("Doctor")
    private val storageRoot = storage.reference

    // normalize phone to storage form +60...
    private fun normalizePhoneForStorage(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val digits = raw.filter { it.isDigit() }
        return if (digits.startsWith("60")) "+$digits" else "+60$digits"
    }

    // Patient
    suspend fun getPatient(patientId: String): Patient? {
        val snapshot = patientsRef.document(patientId).get().await()
        if (!snapshot.exists()) return null

        val firstName = snapshot.getString("FirstName") ?: snapshot.getString("Firstname") ?: ""
        val lastName = snapshot.getString("LastName") ?: snapshot.getString("Lastname") ?: ""
        val ic = snapshot.getString("IC") ?: ""
        val email = snapshot.getString("Email") ?: ""
        val phoneNumber = snapshot.getString("PhoneNumber")
            ?: snapshot.getString("phoneNumber")
            ?: snapshot.getString("Phone")
            ?: ""
        val gender = snapshot.getString("Gender") ?: ""
        val imageUrl = snapshot.getString("imageUrl") ?: snapshot.getString("ImageUrl") ?: ""

        return Patient(
            firstName = firstName,
            lastName = lastName,
            ic = ic,
            email = email,
            phoneNumber = phoneNumber,
            gender = gender,
            imageUrl = imageUrl
        )
    }

    // Uniqueness checks (exclude a given id so current record won't conflict)
    suspend fun isPatientNameTaken(firstName: String, lastName: String, excludeId: String?): Boolean {
        val q = patientsRef
            .whereEqualTo("FirstName", firstName)
            .whereEqualTo("LastName", lastName)
            .limit(2)
            .get()
            .await()
        if (q.isEmpty) return false
        return q.documents.any { it.id != excludeId }
    }

    suspend fun isPatientIcTaken(ic: String, excludeId: String?): Boolean {
        val q = patientsRef
            .whereEqualTo("IC", ic)
            .limit(2)
            .get()
            .await()
        if (q.isEmpty) return false
        return q.documents.any { it.id != excludeId }
    }

    suspend fun isPatientPhoneTaken(rawPhone: String, excludeId: String?): Boolean {
        val phoneToCheck = normalizePhoneForStorage(rawPhone)
        if (phoneToCheck.isBlank()) return false
        val q = patientsRef
            .whereEqualTo("PhoneNumber", phoneToCheck)
            .limit(2)
            .get()
            .await()
        if (q.isEmpty) return false
        return q.documents.any { it.id != excludeId }
    }

    // ClinicStaff uniqueness checks (used by staff and doctor personal profile)
    suspend fun isClinicStaffNameTaken(firstName: String, lastName: String, excludeId: String?): Boolean {
        val q = staffRef
            .whereEqualTo("FirstName", firstName)
            .whereEqualTo("LastName", lastName)
            .limit(2)
            .get()
            .await()
        if (q.isEmpty) return false
        return q.documents.any { it.id != excludeId }
    }

    suspend fun isClinicStaffPhoneTaken(rawPhone: String, excludeId: String?): Boolean {
        val phoneToCheck = normalizePhoneForStorage(rawPhone)
        if (phoneToCheck.isBlank()) return false
        val q = staffRef
            .whereEqualTo("PhoneNumber", phoneToCheck)
            .limit(2)
            .get()
            .await()
        if (q.isEmpty) return false
        return q.documents.any { it.id != excludeId }
    }

    // ClinicStaff
    suspend fun getClinicStaff(staffId: String): ClinicStaff? {
        val snapshot = staffRef.document(staffId).get().await()
        if (!snapshot.exists()) return null
        return ClinicStaff(
            staffId = staffId,
            email = snapshot.getString("Email") ?: "",
            firstName = snapshot.getString("FirstName") ?: "",
            lastName = snapshot.getString("LastName") ?: "",
            phoneNumber = snapshot.getString("PhoneNumber") ?: "",
            gender = snapshot.getString("Gender") ?: "",
            imageUrl = snapshot.getString("ImageUrl") ?: "",
            role = snapshot.getString("Role") ?: "",
            isActive = snapshot.getBoolean("isActive") ?: false
        )
    }
    // Doctor (additional details stored in Doctor collection)
    suspend fun getDoctor(doctorId: String): Doctor? {
        val snapshot = doctorRef.document(doctorId).get().await()
        if (!snapshot.exists()) return null
        return Doctor(
            id = doctorId,
            description = snapshot.getString("Description") ?: "",
            specialization = snapshot.getString("Specialization") ?: "",
            yearsOfExp = snapshot.getLong("YearsOfExp")?.toInt() ?: 0,
            room = snapshot.getString("Room") ?: ""
        )
    }

    // Generic updates
    suspend fun updatePatientFields(patientId: String, updates: Map<String, Any>) {
        if (updates.isNotEmpty()) patientsRef.document(patientId).update(updates).await()
    }

    suspend fun updateClinicStaffFields(staffId: String, updates: Map<String, Any>) {
        if (updates.isNotEmpty()) staffRef.document(staffId).update(updates).await()
    }

    suspend fun updateDoctorFields(doctorId: String, updates: Map<String, Any>) {
        if (updates.isNotEmpty()) doctorRef.document(doctorId).update(updates).await()
    }

    // Upload photo and return download URL
    suspend fun uploadProfilePhoto(collection: String, id: String, fileUri: Uri): String? {
        val path = "$collection/$id/profile_${System.currentTimeMillis()}"
        val ref = storageRoot.child(path)
        ref.putFile(fileUri).await()
        return ref.downloadUrl.await().toString()
    }

    // Convenience wrapper for patient
    suspend fun uploadProfilePhoto(patientId: String, fileUri: Uri): String? =
        uploadProfilePhoto("Patient", patientId, fileUri)
}
