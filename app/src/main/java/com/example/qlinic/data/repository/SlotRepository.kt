package com.example.qlinic.data.repository

import com.example.qlinic.data.model.Slot
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SlotRepository {
    fun listenForDoctorSlotsByDate(doctorId: String, date: Date): Flow<List<Slot>> = callbackFlow {
        val db = Firebase.firestore
        val dayOfWeek = SimpleDateFormat("EEEE", Locale.ENGLISH).format(date)

        val listenerRegistration = db.collection("Slot")
            .whereEqualTo("DoctorID", doctorId)
            .whereEqualTo("DayOfWeek", dayOfWeek)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val slots = snapshot.toObjects(Slot::class.java)
                    // TODO: Filter out already booked slots by checking against an 'Appointments' collection for the given date.
                    trySend(slots)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }
}
