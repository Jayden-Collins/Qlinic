package com.example.qlinic.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ForgetPasswordRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val TAG = "ForgetPassRepo"

    companion object {
        const val USERS_COLLECTION = "Patient"
    }

    /**
     * Check registration by querying Firestore collection "Patient" for a document with matching email.
     * Tries both the original trimmed email and its lowercase form (uses whereIn when appropriate)
     * to avoid misses due to case differences in stored documents.
     */
    suspend fun isEmailRegistered(email: String): Boolean = suspendCancellableCoroutine { cont ->
        val trimmed = email.trim()
        val lower = trimmed.lowercase()
        val candidates = listOf(trimmed, lower).distinct()
        Log.d(TAG, "Checking Firestore Patient for candidates=$candidates")

        val query = if (candidates.size == 1) {
            firestore.collection(USERS_COLLECTION)
                .whereEqualTo("Email", candidates[0])
                .limit(1)
        } else {
            firestore.collection(USERS_COLLECTION)
                .whereIn("Email", candidates)
                .limit(1)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                if (!cont.isActive) return@addOnSuccessListener
                val exists = !snapshot.isEmpty
                Log.d(TAG, "Firestore Patient query result exists=$exists, size=${snapshot.size()}")
                cont.resume(exists)
            }
            .addOnFailureListener { ex ->
                if (!cont.isActive) return@addOnFailureListener
                Log.e(TAG, "Firestore Patient query failed", ex)
                // Surface the error to caller so UI can show proper message
                cont.resumeWithException(ex)
            }
    }

    suspend fun sendPasswordResetEmail(email: String): Unit = suspendCancellableCoroutine { cont ->
        val trimmed = email.trim()
        Log.d(TAG, "Sending reset email to '$trimmed'")
        auth.sendPasswordResetEmail(trimmed)
            .addOnSuccessListener {
                if (!cont.isActive) return@addOnSuccessListener
                cont.resume(Unit)
            }
            .addOnFailureListener { ex ->
                if (!cont.isActive) return@addOnFailureListener
                cont.resumeWithException(ex)
            }
    }
}
