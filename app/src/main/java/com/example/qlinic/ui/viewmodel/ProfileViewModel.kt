package com.example.qlinic.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.SessionManager
import com.example.qlinic.data.repository.PatientProfile
import com.example.qlinic.data.repository.ProfileRepository
import kotlinx.coroutines.launch
import com.example.qlinic.data.repository.*

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    // Create SessionManager instance
    private val sessionManager = SessionManager(application.applicationContext)

    // Pass sessionManager to repository
    private val repo = ProfileRepository(sessionManager)

    // common fields (used by patient/staff/doctor)
    val firstName: MutableState<String?> = mutableStateOf(null)
    val lastName: MutableState<String?> = mutableStateOf(null)
    val imageUrl: MutableState<String?> = mutableStateOf(null)
    val email: MutableState<String?> = mutableStateOf(null)
    val phoneNumber: MutableState<String?> = mutableStateOf(null)
    val gender: MutableState<String?> = mutableStateOf(null)
    val isActive: MutableState<Boolean> = mutableStateOf(false)

    // doctor specific
    val description: MutableState<String?> = mutableStateOf(null)
    val specialization: MutableState<String?> = mutableStateOf(null)
    val yearsOfExp: MutableState<Int?> = mutableStateOf(null)

    val isLoading: MutableState<Boolean> = mutableStateOf(false)

    // role flags
    val isDoctor: MutableState<Boolean> = mutableStateOf(false)
    val isStaff: MutableState<Boolean> = mutableStateOf(false)


    // Store staffId for navigation
    val currentStaffId: MutableState<String?> = mutableStateOf(null)

    fun loadPatient(userId: String? = null) {
        isLoading.value = true
        isDoctor.value = false
        isStaff.value = false
        currentStaffId.value = null

        viewModelScope.launch {
            repo.fetchPatient(userId) { profile ->
                if (profile != null) {
                    firstName.value = profile.firstName
                    lastName.value = profile.lastName
                    imageUrl.value = profile.imageUrl
                    email.value = profile.email
                    phoneNumber.value = profile.phoneNumber
                    gender.value = profile.gender
                } else {
                    firstName.value = null
                    lastName.value = null
                    imageUrl.value = null
                    email.value = null
                    phoneNumber.value = null
                    gender.value = null
                }
                isLoading.value = false
            }
        }
    }
    fun loadStaffProfile(staffId: String? = null, asDoctor: Boolean = false) {
        isLoading.value = true
        isDoctor.value = asDoctor
        isStaff.value = !asDoctor
        currentStaffId.value = staffId

        viewModelScope.launch {
            // Determine expected role string
            val expectedRole = when {
                asDoctor -> "doctor"
                else -> "staff"  // Or "Front_desk" based on your actual data
            }

            repo.fetchClinicStaff(staffId, expectedRole) { staff ->
                if (staff != null) {
                    Log.d("ProfileViewModel", "Loaded staff: ${staff.firstName} (ID: ${staff.staffId})")
                    firstName.value = staff.firstName
                    lastName.value = staff.lastName
                    imageUrl.value = staff.imageUrl
                    email.value = staff.email
                    phoneNumber.value = staff.phoneNumber
                    gender.value = staff.gender
                    isActive.value = staff.isActive

                    // If doctor, load additional details
                    if (asDoctor) {
                        repo.fetchDoctor(staff.staffId) { doctorDetails ->
                            description.value = doctorDetails?.description
                            specialization.value = doctorDetails?.specialization
                            yearsOfExp.value = doctorDetails?.yearsOfExp
                            isLoading.value = false
                        }
                    } else {
                        description.value = null
                        specialization.value = null
                        yearsOfExp.value = null
                        isLoading.value = false
                    }
                } else {
                    Log.e("ProfileViewModel", "Failed to load staff profile for ID: $staffId")
                    resetAllFields()
                    isLoading.value = false
                }
            }
        }
    }

    private fun resetAllFields() {
        firstName.value = null
        lastName.value = null
        imageUrl.value = null
        email.value = null
        phoneNumber.value = null
        gender.value = null
        isActive.value = false
        description.value = null
        specialization.value = null
        yearsOfExp.value = null
    }

    fun logout() {
        // Call repository logout
        repo.signOut()

        // Optionally clear viewmodel state
        resetAllFields()

        Log.d("ProfileViewModel", "Logout completed")
    }
}