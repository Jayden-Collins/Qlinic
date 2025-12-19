package com.example.qlinic.data.repository

import android.util.Log
import com.example.qlinic.data.model.Patient
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class PatientRepository {
    private val db = Firebase.firestore
    private val patientsCollection = db.collection("Patient")

    /**
     * To be called when a user logs in.
     * Fetches the current FCM token and saves it to the patient's record.
     */
    suspend fun onLogin(patientId: String) {
        try {
            // Get the current FCM registration token
            val token = FirebaseMessaging.getInstance().token.await()
            updateFcmToken(patientId, token)
            Log.d("PatientRepository", "FCM token updated for patient $patientId")
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error getting/updating FCM token on login", e)
        }
    }

    /**
     * To be called when a user logs out.
     * Clears the FCM token from the patient's record.
     */
    suspend fun onLogout(patientId: String) {
        try {
            // Clear the fcmToken field by setting it to an empty string
            updateFcmToken(patientId, "")
            Log.d("PatientRepository", "FCM token cleared for patient $patientId")
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error clearing FCM token on logout", e)
        }
    }

    suspend fun getPatient(patientId: String): Patient? {
        val document = patientsCollection.document(patientId).get().await()
        return document.toObject(Patient::class.java)
    }

    suspend fun updateFcmToken(patientId: String, token: String) {
        patientsCollection.document(patientId).update("fcmToken", token).await()
    }

    suspend fun findPatientByIc(ic: String): Patient? {
        val querySnapshot = patientsCollection
            .whereEqualTo("IC", ic) // Ensure field name matches Firestore
            .get()
            .await()

        return if (!querySnapshot.isEmpty) {
            querySnapshot.documents[0].toObject(Patient::class.java)
        } else {
            null
        }
    }

    //TODO: Check for existing/duplicate patientID
    suspend fun addPatient(patient: Patient): String {
        return try {
            val patientId = patientsCollection.document().id
            patient.patientId = patientId
            patientsCollection.document(patientId).set(patient).await()
            patientId
        } catch (e: Exception) {
            ""
        }
    }
}