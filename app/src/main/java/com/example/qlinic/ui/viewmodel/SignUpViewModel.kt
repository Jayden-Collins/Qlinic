package com.example.qlinic.ui.viewmodel

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.Patient
import com.example.qlinic.data.repository.PatientSignUpRepository
import kotlinx.coroutines.launch

data class SignUpUiState(
    val firstName: String = "",
    val lastName: String = "",
    val nric: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val gender: String = "",
    val phone: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signupSuccess: Boolean = false
)

class SignupViewModel(
    private val repository: PatientSignUpRepository = PatientSignUpRepository()
) : ViewModel() {

    var uiState by mutableStateOf(SignUpUiState())
        private set

    fun onFirstNameChange(v: String) { uiState = uiState.copy(firstName = v, errorMessage = null) }
    fun onLastNameChange(v: String) { uiState = uiState.copy(lastName = v, errorMessage = null) }
    fun onNricChange(v: String) { uiState = uiState.copy(nric = v, errorMessage = null) }
    fun onEmailChange(v: String) { uiState = uiState.copy(email = v, errorMessage = null) }
    fun onPasswordChange(v: String) { uiState = uiState.copy(password = v, errorMessage = null) }
    fun onConfirmPasswordChange(v: String) { uiState = uiState.copy(confirmPassword = v, errorMessage = null) }
    fun onGenderChange(v: String) { uiState = uiState.copy(gender = v, errorMessage = null) }
    fun onPhoneChange(v: String) { uiState = uiState.copy(phone = v, errorMessage = null) }

    private fun setLoading(loading: Boolean) { uiState = uiState.copy(isLoading = loading) }
    private fun setError(msg: String?) { uiState = uiState.copy(errorMessage = msg, isLoading = false) }
    private fun setSuccess() { uiState = uiState.copy(signupSuccess = true, isLoading = false) }

    private fun validate(): String? {
        val s = uiState
        if (s.firstName.isBlank() || s.lastName.isBlank() || s.nric.isBlank() || s.email.isBlank() || s.password.isBlank() || s.confirmPassword.isBlank() || s.gender.isBlank() || s.phone.isBlank()) {
            return "Please fill all fields"
        }
        //Names must contain only letters and spaces
        val namePattern = Regex("^[A-Za-z\\s]+$")
        if (!namePattern.matches(s.firstName)) return "First name must contain only letters and spaces"
        if (!namePattern.matches(s.lastName)) return "Last name must contain only letters and spaces"

        if (s.firstName.length > 10) return "First name must not exceed 10 characters"
        if (s.lastName.length > 20) return "Last name must not exceed 20 characters"
        if (!Patterns.EMAIL_ADDRESS.matcher(s.email).matches()) return "Invalid email format"
        if (s.phone.any { !it.isDigit() }) return "Phone number must contain only digits"
        if (!s.phone.startsWith("1")) return "Phone number must start with 01"
        if (s.phone.length !in 9..10) return "Phone number must be 10-11 digits"
        val nricPattern = Regex("^\\d{6}-\\d{2}-\\d{4}$")
        if (!nricPattern.matches(s.nric)) return "NRIC must be in format 000000-00-0000"
        if (s.password.length < 8) return "Password must be at least 8 characters"
        if (s.password != s.confirmPassword) return "Passwords don't match"
        return null
    }

    fun signup(onSuccessNavigate: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        val err = validate()
        if (err != null) {
            setError(err)
            onFailure(err)
            return
        }
        setLoading(true)
        val fullPhone = "+60${uiState.phone}"
        val p = Patient(
            firstName = uiState.firstName,
            lastName = uiState.lastName,
            ic = uiState.nric,
            email = uiState.email,
            phoneNumber = fullPhone,
            gender = uiState.gender
        )
        viewModelScope.launch {
            repository.signupPatient(p, uiState.password, {
                setSuccess()
                onSuccessNavigate()
            }, { msg ->
                setError(msg)
                onFailure(msg)
            })
        }
    }
}
