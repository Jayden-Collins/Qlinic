package com.example.qlinic.ui.viewmodel


import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.SessionManager
import com.example.qlinic.data.repository.ClinicStaffRepository
import com.example.qlinic.data.repository.PatientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

enum class LoginUserType {
    PATIENT,
    CLINIC_STAFF
}


data class LoginUiState(
    val identifier: String = "", // For patient: email or NRIC, For staff: StaffID
    val password: String = "",
    val userType: LoginUserType = LoginUserType.PATIENT,
    val isLoading: Boolean = false,
    val globalError: String? = null,
    val errorMessage: String? = null,
    val identifierError: String? = null,
    val passwordError: String? = null,
    val loginSuccess: Boolean = false,
    val navigateToPatientHome: Boolean = false,
    val navigateToStaffHome: Boolean = false,
    val navigateToDoctorHome: Boolean = false,
    val passwordVisible: Boolean = false,
    val rememberMe: Boolean = false
)


class LoginViewModel (application: Application): AndroidViewModel(application) {
    private val TAG = "LoginViewModel"
    // Create SessionManager instance
    private val sessionManager = SessionManager(application.applicationContext)

    // Create instances directly (no dependency injection for now)
    private val patientRepository = PatientRepository()
    private val clinicStaffRepository = ClinicStaffRepository()

    var uiState by mutableStateOf(LoginUiState())
        private set

    // Check for saved session on initialization
    init {
        checkSavedSession()
    }


    private fun checkSavedSession() {
        viewModelScope.launch {
            if (sessionManager.shouldRememberMe()) {
                val savedIdentifier = sessionManager.getSavedIdentifier()
                val savedUserType = sessionManager.getSavedUserType()

                if (!savedIdentifier.isNullOrEmpty() && !savedUserType.isNullOrEmpty()) {
                    // Pre-fill the login form
                    uiState = uiState.copy(
                        identifier = savedIdentifier,
                        userType = when (savedUserType) {
                            "PATIENT" -> LoginUserType.PATIENT
                            "CLINIC_STAFF" -> LoginUserType.CLINIC_STAFF
                            else -> LoginUserType.PATIENT
                        }
                    )
                }
            }
        }
    }

    // helper suspend wrappers
    private suspend fun loginPatientSuspend(identifier: String, password: String): String =
        patientRepository.loginPatient(identifier, password)


    private suspend fun loginStaffSuspend(
        identifier: String,
        password: String
    ): Pair<String, String> =
        suspendCoroutine { cont ->
            clinicStaffRepository.loginClinicStaff(
                identifier = identifier,
                password = password,
                onSuccess = { role, staffId -> cont.resume(role to staffId) },
                onFailure = { msg -> cont.resumeWithException(RuntimeException(msg)) }
            )
        }



    fun onUserTypeChange(userType: LoginUserType) {
        uiState = uiState.copy(
            userType = userType,
            globalError = null,
            identifier = "",
            password = "",
            identifierError = null,
            passwordError = null
        )
    }

    fun onIdentifierChange(v: String) {
        uiState = uiState.copy(identifier = v, identifierError = null)
    }

    fun onPasswordChange(v: String) {
        uiState = uiState.copy(password = v, errorMessage = null, passwordError = null)
    }

    fun onPasswordVisibilityChange(visible: Boolean) {
        uiState = uiState.copy(passwordVisible = visible)
    }

    fun onRememberMeChange(remember: Boolean) {
        uiState = uiState.copy(rememberMe = remember)
    }

    private fun setLoading(loading: Boolean) {
        uiState = uiState.copy(isLoading = loading)
    }

    private fun setError(msg: String?) {
        uiState = uiState.copy(globalError = msg, isLoading = false)
    }

    private fun setFieldErrors(
        idErr: String? = null,
        passErr: String? = null,
        globalErr: String? = null
    ) {
        uiState = uiState.copy(
            identifierError = idErr,
            passwordError = passErr,
            errorMessage = globalErr,
            isLoading = false
        )
    }

    private fun setNavigateToPatientHome() {
        uiState = uiState.copy(
            navigateToPatientHome = true,
            loginSuccess = true,
            isLoading = false
        )
    }

    private fun setNavigateToStaffHome() {
        uiState = uiState.copy(
            navigateToStaffHome = true,
            loginSuccess = true,
            isLoading = false
        )
    }

    private fun setNavigateToDoctorHome() {
        uiState = uiState.copy(
            navigateToDoctorHome = true,
            loginSuccess = true,
            isLoading = false
        )
    }

    fun resetNavigation() {
        uiState = uiState.copy(
            navigateToPatientHome = false,
            navigateToStaffHome = false,
            navigateToDoctorHome = false
        )
    }

    private fun validate(): String? {
        // clear previous field errors
        uiState = uiState.copy(identifierError = null, passwordError = null, errorMessage = null)

        return when (uiState.userType) {
            LoginUserType.PATIENT -> {
                val idBlank = uiState.identifier.isBlank()
                val pwBlank = uiState.password.isBlank()
                if (idBlank || pwBlank) {
                    setFieldErrors(
                        idErr = if (idBlank) "Please enter email or NRIC" else null,
                        passErr = if (pwBlank) "Please enter password" else null,
                        globalErr = "Please enter email/NRIC and password"
                    )
                    return "Please enter email/NRIC and password"
                }
                null
            }

            LoginUserType.CLINIC_STAFF -> {
                val idBlank = uiState.identifier.isBlank()
                val pwBlank = uiState.password.isBlank()
                if (idBlank || pwBlank) {
                    setFieldErrors(
                        idErr = if (idBlank) "Please enter Staff ID" else null,
                        passErr = if (pwBlank) "Please enter password" else null,
                        globalErr = "Please enter Staff ID and password"
                    )
                    return "Please enter Staff ID and password"
                }

                val idUpper = uiState.identifier.uppercase()
                if (!idUpper.startsWith("S") && !idUpper.startsWith("S")) {
                    setFieldErrors(
                        idErr = "ID must start with 'S'",
                        globalErr = "ID must start with 'S'"
                    )
                    return "ID must start with 'S' or 'S'"
                }

                val pattern = Regex("^[DS]\\d{3}$")
                if (!pattern.matches(idUpper)) {
                    setFieldErrors(
                        idErr = "ID format must be:S001",
                        globalErr = "ID format must be:S001"
                    )
                    return "ID format must be:S001 (letter followed by 3 digits)"
                }

                null
            }
        }
    }


    fun login(
        rememberMe: Boolean,
        onSuccessNavigate: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        Log.d(TAG, "login() called with identifier='${uiState.identifier}' userType=${uiState.userType}")
        val err = validate()
        if (err != null) {
            onFailure(err)
            return
        }

        setLoading(true)

        // start a coroutine in the ViewModel scope
        viewModelScope.launch {
            try {
                when (uiState.userType) {
                    LoginUserType.PATIENT -> {
                        Log.d(TAG, "Starting patient login")
                        // call suspend wrapper from inside coroutine and run on IO dispatcher
                        val patientId = withContext(Dispatchers.IO) {
                            loginPatientSuspend(uiState.identifier, uiState.password)
                        }
                        Log.d(TAG, "patient login success id=$patientId")
                        sessionManager.saveLoginSession(
                            isLoggedIn = true,
                            rememberMe = rememberMe,
                            userType = "PATIENT",
                            identifier = uiState.identifier,
                            userId = patientId
                        )

                        setNavigateToPatientHome()
                        onSuccessNavigate()
                    }

                    LoginUserType.CLINIC_STAFF -> {
                        Log.d(TAG, "Starting staff login")
                        val (role, staffId) = withContext(Dispatchers.IO) {
                            loginStaffSuspend(uiState.identifier, uiState.password)
                        }

                        Log.d(TAG, "staff login success role=$role id=$staffId")
                        sessionManager.saveLoginSession(
                            isLoggedIn = true,
                            rememberMe = rememberMe,
                            userType = "CLINIC_STAFF",
                            identifier = uiState.identifier,
                            staffId = staffId,
                            role = role
                        )

                        var recognized = true
                        when (role.uppercase()) {
                            "DOCTOR" -> setNavigateToDoctorHome()
                            "FRONT_DESK", "STAFF" -> setNavigateToStaffHome()
                            else -> {
                                setError("Unknown role: $role")
                                onFailure("Unknown role: $role")
                                recognized = false
                            }
                        }
                        if (recognized) onSuccessNavigate()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "login failed", e)
                val msg = e.message ?: "Login failed"
                setError(msg)
                onFailure(msg)
            } finally {
                // ensure loading cleared if something unexpected happens
                if (uiState.isLoading) setLoading(false)
            }
        }
    }

}