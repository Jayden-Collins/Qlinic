package com.example.qlinic.data.model

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "user_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_USER_TYPE = "user_type"
        private const val KEY_IDENTIFIER = "identifier"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_STAFF_ID = "staff_id"
        private const val KEY_ROLE = "role"
        private const val KEY_LAST_LOGIN = "last_login"
    }

    // For encrypted preferences (recommended for sensitive data)
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Alternatively, use regular SharedPreferences if you don't want encryption
    // private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // Initialize login state
        _isLoggedIn.value = getIsLoggedIn()
    }

    fun saveLoginSession(
        isLoggedIn: Boolean,
        rememberMe: Boolean,
        userType: String? = null,
        identifier: String? = null,
        userId: String? = null,
        staffId: String? = null,
        role: String? = null
    ) {
        with(encryptedPrefs.edit()) {
            putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            putBoolean(KEY_REMEMBER_ME, rememberMe)

            if (rememberMe) {
                userType?.let { putString(KEY_USER_TYPE, it) }
                identifier?.let { putString(KEY_IDENTIFIER, it) }
                userId?.let { putString(KEY_USER_ID, it) }
                staffId?.let { putString(KEY_STAFF_ID, it) }
                role?.let { putString(KEY_ROLE, it) }
                putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
            } else {
                // Clear saved credentials if "Remember Me" is not checked
                remove(KEY_IDENTIFIER)
                remove(KEY_USER_TYPE)
                remove(KEY_USER_ID)
                remove(KEY_STAFF_ID)
                remove(KEY_ROLE)
            }

            apply()
        }

        _isLoggedIn.value = isLoggedIn
    }

    fun clearSession() {
        with(encryptedPrefs.edit()) {
            clear()
            apply()
        }
        _isLoggedIn.value = false
    }

    fun logout() {
        // Clear session and also clear Firebase auth
        clearSession()
    }

    fun getIsLoggedIn(): Boolean {
        return encryptedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun shouldRememberMe(): Boolean {
        return encryptedPrefs.getBoolean(KEY_REMEMBER_ME, false)
    }

    fun getSavedUserType(): String? {
        return encryptedPrefs.getString(KEY_USER_TYPE, null)
    }

    fun getSavedIdentifier(): String? {
        return encryptedPrefs.getString(KEY_IDENTIFIER, null)
    }

    fun getSavedUserId(): String? {
        return encryptedPrefs.getString(KEY_USER_ID, null)
    }

    fun getSavedStaffId(): String? {
        return encryptedPrefs.getString(KEY_STAFF_ID, null)
    }

    fun getSavedRole(): String? {
        return encryptedPrefs.getString(KEY_ROLE, null)
    }

    fun getLastLogin(): Long {
        return encryptedPrefs.getLong(KEY_LAST_LOGIN, 0)
    }

    // Optional: Auto-logout after certain period
    fun shouldAutoLogout(days: Int = 30): Boolean {
        if (!shouldRememberMe()) return false

        val lastLogin = getLastLogin()
        val daysInMillis = days * 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() - lastLogin > daysInMillis
    }
}