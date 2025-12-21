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

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
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

            // Essential session data should ALWAYS be saved if we are logging in,
            // otherwise the app won't know the current user's role/ID.
            if (isLoggedIn) {
                userType?.let { putString(KEY_USER_TYPE, it) }
                userId?.let { putString(KEY_USER_ID, it) }
                staffId?.let { putString(KEY_STAFF_ID, it) }
                role?.let { putString(KEY_ROLE, it) }
            }

            if (rememberMe) {
                identifier?.let { putString(KEY_IDENTIFIER, it) }
                putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
            } else {
                // If "Remember Me" is false, we clear the identifier (like email) 
                // so it's not pre-filled next time, but we keep the session active.
                remove(KEY_IDENTIFIER)
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

    fun shouldAutoLogout(days: Int = 30): Boolean {
        if (!shouldRememberMe()) return false

        val lastLogin = getLastLogin()
        val daysInMillis = days * 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() - lastLogin > daysInMillis
    }
}
