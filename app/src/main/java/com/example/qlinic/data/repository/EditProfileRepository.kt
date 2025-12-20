package com.example.qlinic.data.repository

import android.net.Uri
import com.example.qlinic.data.model.Patient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

data class ClinicStaff(
    val id: String = "",
    val email: String? = null,
    val firstName: String = "",
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val gender: String? = null,
    val imageUrl: String? = null,
    val isActive: Boolean = false
)

data class Doctor(
    val id: String = "",
    val description: String? = null,
    val specialization: String? = null,
    val yearsOfExp: Int? = null,
    val imageUrl: String? = null
)

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
        val snap = patientsRef.document(patientId).get().await()
        if (!snap.exists()) return null

        val firstName = snap.getString("FirstName") ?: snap.getString("Firstname") ?: ""
        val lastName = snap.getString("LastName") ?: snap.getString("Lastname") ?: ""
        val ic = snap.getString("IC") ?: ""
        val email = snap.getString("Email") ?: ""
        val phoneNumber = snap.getString("PhoneNumber")
            ?: snap.getString("phoneNumber")
            ?: snap.getString("Phone")
            ?: ""
        val gender = snap.getString("Gender") ?: ""
        val photoUrl = snap.getString("photoUrl") ?: snap.getString("ImageUrl") ?: ""

        return Patient(
            firstName = firstName,
            lastName = lastName,
            ic = ic,
            email = email,
            phoneNumber = phoneNumber,
            gender = gender,
            photoUrl = photoUrl
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
        val snap = staffRef.document(staffId).get().await()
        if (!snap.exists()) return null
        return ClinicStaff(
            id = staffId,
            email = snap.getString("Email"),
            firstName = snap.getString("FirstName") ?: snap.getString("Firstname") ?: "",
            lastName = snap.getString("LastName") ?: snap.getString("Lastname"),
            phoneNumber = snap.getString("PhoneNumber") ?: snap.getString("Phone"),
            gender = snap.getString("Gender"),
            imageUrl = snap.getString("photoUrl") ?: snap.getString("ImageUrl"),
            isActive = snap.getBoolean("isActive") ?: false
        )
    }
    // Doctor (additional details stored in Doctor collection)
    suspend fun getDoctor(doctorId: String): Doctor? {
        val snap = doctorRef.document(doctorId).get().await()
        if (!snap.exists()) return null
        return Doctor(
            id = doctorId,
            description = snap.getString("Description"),
            specialization = snap.getString("Specialization"),
            yearsOfExp = snap.getLong("YearsOfExp")?.toInt(),
            imageUrl = snap.getString("photoUrl") ?: snap.getString("ImageUrl")
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
