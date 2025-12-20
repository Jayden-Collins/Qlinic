package com.example.qlinic.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.Patient
import com.example.qlinic.data.repository.ClinicStaff
import com.example.qlinic.data.repository.Doctor
import com.example.qlinic.data.repository.EditProfileRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProfileViewModel(
    private val repo: EditProfileRepository = EditProfileRepository()
) : ViewModel() {

    private val _patient = MutableStateFlow<Patient?>(null)
    val patient: StateFlow<Patient?> = _patient

    private val _staff = MutableStateFlow<ClinicStaff?>(null)
    val staff: StateFlow<ClinicStaff?> = _staff

    private val _doctor = MutableStateFlow<Doctor?>(null)
    val doctor: StateFlow<Doctor?> = _doctor

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _saveSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveSuccess = _saveSuccess.asSharedFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Loaders
    fun loadPatient(patientId: String) = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try {
            _patient.value = repo.getPatient(patientId)
        } catch (t: Throwable) {
            _error.value = "Failed to load patient: ${t.message}"
        } finally {
            _loading.value = false
        }
    }

    fun loadStaff(staffId: String) = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try {
            _staff.value = repo.getClinicStaff(staffId)
        } catch (t: Throwable) {
            _error.value = "Failed to load staff: ${t.message}"
        } finally {
            _loading.value = false
        }
    }

    fun loadDoctor(doctorId: String) = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try {
            _doctor.value = repo.getDoctor(doctorId)
        } catch (t: Throwable) {
            _error.value = "Failed to load doctor details: ${t.message}"
        } finally {
            _loading.value = false
        }
    }

    private fun validatePatientInput(firstName: String?, lastName: String?, phone: String?, nric: String?): String? {
        firstName?.let {
            if (it.length > 10) return "First name must not exceed 10 characters"
        }
        lastName?.let {
            if (it.length > 20) return "Last name must not exceed 20 characters"
        }
        phone?.let {
            if (it.any { ch -> !ch.isDigit() }) return "Phone number must contain only digits"
            if (!it.startsWith("1")) return "Phone number must start with 1"
            if (it.length !in 9..10) return "Phone number must be 9-10 digits"
        }
        nric?.let {
            val nricPattern = Regex("^\\d{6}-\\d{2}-\\d{4}$")
            if (!nricPattern.matches(it)) return "NRIC must be in format 000000-00-0000"
        }
        return null
    }

    // Helper function to extract phone number without country code for display
    fun extractPhoneNumberForDisplay(phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank()) return ""
        return if (phoneNumber.startsWith("+60")) {
            phoneNumber.substring(3) // Remove "+60"
        } else if (phoneNumber.startsWith("60")) {
            phoneNumber.substring(2) // Remove "60"
        } else {
            phoneNumber
        }
    }

    // Helper function to format phone number for storage
    fun formatPhoneNumberForStorage(phoneNumber: String): String {
        val digits = phoneNumber.filter { ch -> ch.isDigit() }
        return if (digits.startsWith("60")) {
            "+$digits"
        } else {
            "+60$digits"
        }
    }

    // Patient update
    fun updatePatientPartial(patientId: String, candidateValues: Map<String, String>) {
        val current = _patient.value ?: run { _error.value = "No patient loaded"; return }

        val firstCandidate = candidateValues["FirstName"]
        val lastCandidate = candidateValues["LastName"]
        val phoneCandidate = candidateValues["PhoneNumber"]
        val icCandidate = candidateValues["IC"]

        validatePatientInput(firstCandidate, lastCandidate, phoneCandidate, icCandidate)?.let { err ->
            _error.value = err
            return
        }

        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                // Uniqueness checks only when candidate differs from current
                if (firstCandidate != null || lastCandidate != null) {
                    val newFirst = firstCandidate ?: current.firstName
                    val newLast = lastCandidate ?: current.lastName
                    if (newFirst != current.firstName || newLast != current.lastName) {
                        if (repo.isPatientNameTaken(newFirst, newLast, patientId)) {
                            _error.value = "Name already exists"
                            return@launch
                        }
                    }
                }

                if (icCandidate != null && icCandidate != current.ic) {
                    if (repo.isPatientIcTaken(icCandidate, patientId)) {
                        _error.value = "NRIC already exists"
                        return@launch
                    }
                }

                if (phoneCandidate != null && phoneCandidate != extractPhoneNumberForDisplay(current.phoneNumber)) {
                    if (repo.isPatientPhoneTaken(phoneCandidate, patientId)) {
                        _error.value = "Phone number already exists"
                        return@launch
                    }
                }

                val updates = mutableMapOf<String, Any>()
                candidateValues["FirstName"]?.let { if (it != current.firstName) updates["FirstName"] = it }
                candidateValues["LastName"]?.let { if (it != current.lastName) updates["LastName"] = it }
                candidateValues["IC"]?.let { if (it != current.ic) updates["IC"] = it }
                candidateValues["PhoneNumber"]?.let {
                    val formattedPhone = formatPhoneNumberForStorage(it)
                    if (formattedPhone != current.phoneNumber) updates["PhoneNumber"] = formattedPhone
                }
                if (updates.isNotEmpty()) {
                    repo.updatePatientFields(patientId, updates)
                    _patient.value = repo.getPatient(patientId)
                    _saveSuccess.emit(Unit)
                }
            } catch (t: Throwable) {
                _error.value = "Failed to update patient: ${t.message}"
            } finally {
                _loading.value = false
            }
        }
    }


    // Staff update
    fun updateStaffPartial(staffId: String, updatesInput: Map<String, Any>) {
        val current = _staff.value ?: run { _error.value = "No staff loaded"; return }
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                // Uniqueness checks for ClinicStaff (used by staff and doctor personal profile)
                val firstCandidate = updatesInput["FirstName"] as? String
                val lastCandidate = updatesInput["LastName"] as? String
                if (firstCandidate != null || lastCandidate != null) {
                    val newFirst = firstCandidate ?: current.firstName
                    val newLast = lastCandidate ?: current.lastName ?: ""
                    if (newFirst != current.firstName || newLast != current.lastName) {
                        if (repo.isClinicStaffNameTaken(newFirst, newLast, staffId)) {
                            _error.value = "Name already exists"
                            _loading.value = false
                            return@launch
                        }
                    }
                }

                val phoneCandidate = (updatesInput["PhoneNumber"] as? String)
                if (phoneCandidate != null && phoneCandidate != extractPhoneNumberForDisplay(current.phoneNumber)) {
                    if (repo.isClinicStaffPhoneTaken(phoneCandidate, staffId)) {
                        _error.value = "Phone number already exists"
                        _loading.value = false
                        return@launch
                    }
                }

                val updates = mutableMapOf<String, Any>()

                (updatesInput["FirstName"] as? String)?.let { if (it != current.firstName) updates["FirstName"] = it }
                (updatesInput["LastName"] as? String)?.let { if (it != current.lastName) updates["LastName"] = it }
                (updatesInput["Email"] as? String)?.let { if (it != current.email) updates["Email"] = it }
                (updatesInput["PhoneNumber"] as? String)?.let {
                    // Format phone number for storage
                    val formattedPhone = formatPhoneNumberForStorage(it)
                    if (formattedPhone != current.phoneNumber) updates["PhoneNumber"] = formattedPhone
                }
                updatesInput["isActive"]?.let { updates["isActive"] = it }

                if (updates.isNotEmpty()) {
                    repo.updateClinicStaffFields(staffId, updates)
                    _staff.value = repo.getClinicStaff(staffId)
                    _saveSuccess.emit(Unit)
                }
            } catch (t: Throwable) { _error.value = "Failed to update staff: ${t.message}" }
            finally { _loading.value = false }
        }
    }

    // Doctor update (doctor-specific fields stored in Doctor collection)
    fun updateDoctorPartial(doctorId: String, updatesInput: Map<String, Any>) {
        val current = _doctor.value ?: run { _error.value = "No doctor loaded"; return }
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                val updates = mutableMapOf<String, Any>()
                (updatesInput["Description"] as? String)?.let { if (it != current.description) updates["Description"] = it }
                (updatesInput["Specialization"] as? String)?.let { if (it != current.specialization) updates["Specialization"] = it }
                // YearsOfExp might be Int or String; normalize to Int
                val y = when (val v = updatesInput["YearsOfExp"]) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: current.yearsOfExp
                    else -> current.yearsOfExp
                }
                if (y != null && y != current.yearsOfExp) updates["YearsOfExp"] = y

                if (updates.isNotEmpty()) {
                    repo.updateDoctorFields(doctorId, updates)
                    _doctor.value = repo.getDoctor(doctorId)
                    _saveSuccess.emit(Unit)
                }
            } catch (t: Throwable) {
                _error.value = "Failed to update doctor: ${t.message }" }
            finally { _loading.value = false }
        }
    }

    // Upload photo for any of the collections (role selects which collection)
    fun uploadAndSetProfilePhoto(role: String, id: String, fileUri: Uri) {
        viewModelScope.launch {
            _loading.value = true; _error.value = null
            try {
                val url = when (role.lowercase()) {
                    "staff" -> repo.uploadProfilePhoto("ClinicStaff", id, fileUri)
                    "doctor" -> repo.uploadProfilePhoto("ClinicStaff", id, fileUri)
                    else -> repo.uploadProfilePhoto("Patient", id, fileUri)
                }
                if (!url.isNullOrBlank()) {
                    val key = "photoUrl"
                    when (role.lowercase()) {
                        "staff" -> {
                            repo.updateClinicStaffFields(id, mapOf(key to url))
                            _staff.value = repo.getClinicStaff(id)
                        }
                        "doctor" -> {
                            repo.updateDoctorFields(id, mapOf(key to url))
                            _doctor.value = repo.getDoctor(id)
                        }
                        else -> {
                            repo.updatePatientFields(id, mapOf(key to url))
                            _patient.value = repo.getPatient(id)
                        }
                    }
                    _saveSuccess.emit(Unit)
                }
            } catch (t: Throwable) { _error.value = t.message }
            finally { _loading.value = false }
        }
    }

}
