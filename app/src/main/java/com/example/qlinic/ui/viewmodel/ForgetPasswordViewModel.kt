package com.example.qlinic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.repository.ForgetPasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ForgetPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)


class ForgetPasswordViewModel(
    private val repo: ForgetPasswordRepository = ForgetPasswordRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgetPasswordUiState())
    val uiState: StateFlow<ForgetPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(new: String) {
        _uiState.value = _uiState.value.copy(email = new, errorMessage = null, successMessage = null)
    }

    fun sendResetEmail() {
        val email = _uiState.value.email.trim()
        if (email.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Email required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            try {
                val registered = repo.isEmailRegistered(email)
                if (!registered) {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Email is not registered")
                    return@launch
                }

                repo.sendPasswordResetEmail(email)
                _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Reset email sent. Check your inbox.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Failed to send reset email")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}