package com.example.qlinic.data.repository

import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.Slot
import com.example.qlinic.utils.getDayStartAndEnd
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SlotRepository {
    fun listenForDoctorSlotsByDate(doctorId: String, date: Date): Flow<List<Slot>> = callbackFlow {
        val db = Firebase.firestore
        val dayOfWeek = SimpleDateFormat("EEEE", Locale.ENGLISH).format(date)

        // Start building the query
        var query = db.collection("Slot")
            .whereEqualTo("DoctorID", doctorId)
            .whereEqualTo("DayOfWeek", dayOfWeek)

        // Only filter by time if the selected date is today
        if (isToday(date)) {
            val currentTime = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date())
            query = query.whereGreaterThanOrEqualTo("SlotStartTime", currentTime)
        }

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val slots = snapshot.toObjects(Slot::class.java)

                    val (startOfDay, endOfDay) = getDayStartAndEnd(date)

                    db.collection("Appointment")
                        .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                        .whereLessThanOrEqualTo("appointmentDate", endOfDay)
                        .get()
                        .addOnSuccessListener { appointmentSnapshot ->
                            val bookedSlotIds = appointmentSnapshot.toObjects(Appointment::class.java).map { it.slotId }
                            val availableSlots = slots.filterNot { it.SlotID in bookedSlotIds }
                            trySend(availableSlots)
                        }
                        .addOnFailureListener { e ->
                            close(e)
                        }
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    private fun isToday(date: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date }
        val cal2 = Calendar.getInstance()
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}