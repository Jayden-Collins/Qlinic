package com.example.qlinic.data.repository

import com.example.qlinic.data.model.AvailabilityException
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.util.Date

// Get doctor availability exceptions (on leave or off day)
class AvailabilityExceptionRepository {
    suspend fun getDoctorAvailabilityExceptionsForRange(
        startDateTime: Date,
        endDateTime: Date,
        doctorID: String,
    ): List<AvailabilityException> {
        return try {
            val db = Firebase.firestore

            // Find exceptions that start any time before the end of the selected day.
            val snapshot = db.collection("AvailabilityException")
                .whereEqualTo("doctorID", doctorID)
                .whereLessThanOrEqualTo("startDateTime", endDateTime)
                .get()
                .await()

            // Client-side filtering to find true overlaps.
            // An overlap occurs if the exception's end time is after the day's start time.
            // Combined with the query, this gives us all exceptions that overlap with the selected day.
            snapshot.documents.mapNotNull { doc ->
                try {
                    val exceptionEndDateTime = doc.getDate("endDateTime")
                    // If the exception ends before our day begins, there's no overlap.
                    if (exceptionEndDateTime != null && exceptionEndDateTime.before(startDateTime)) {
                        return@mapNotNull null
                    }

                    AvailabilityException(
                        exceptionID = doc.getString("exceptionID") ?: doc.id,
                        startDateTime = doc.getDate("startDateTime") ?: Date(),
                        endDateTime = exceptionEndDateTime ?: Date(),
                        reason = doc.getString("reason") ?: "",
                        doctorID = doc.getString("doctorID") ?: ""
                    )
                } catch (e: Exception) {
                    println("Error parsing availability exception: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching availability exceptions: ${e.message}")
            emptyList()
        }
    }
}