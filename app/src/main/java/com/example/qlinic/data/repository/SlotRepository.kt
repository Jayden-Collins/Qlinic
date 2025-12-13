package com.example.qlinic.data.repository

import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.Slot
import com.example.qlinic.utils.getDayStartAndEnd
import com.example.qlinic.utils.isToday
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// fetch the available slots for a doctor on a specific date
class SlotRepository {
    private val availabilityExceptionRepository = AvailabilityExceptionRepository()

    fun listenForDoctorSlotsByDate(doctorID: String, date: Date): Flow<List<Slot>> = callbackFlow {
        val db = Firebase.firestore

        val (startOfDay, endOfDay) = getDayStartAndEnd(date)

        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
        val dayOfWeek = dayOfWeekFormat.format(date)

        var query: Query = db.collection("Slot")
            .whereEqualTo("doctorID", doctorID)
            .whereEqualTo("dayOfWeek", dayOfWeek)

        if (isToday(date)) {
            val currentTime = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date())
            query = query.whereGreaterThanOrEqualTo("SlotStartTime", currentTime)
        }

        // Listen for changes in the doctor's slots
        val slotsListener = query.addSnapshotListener { slotsSnapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (slotsSnapshot == null) {
                    close(IllegalStateException("Slots snapshot is null"))
                    return@addSnapshotListener
                }

                val allSlots = slotsSnapshot.documents.mapNotNull { it.toObject(Slot::class.java) }

                // After fetching slots, check for booked appointments and availability exceptions
                // Use a coroutine to perform async operations
                launch {
                    // 1. Fetch booked appointments for the day
                    db.collection("Appointment")
                        .whereEqualTo("doctorID", doctorID)
                        .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                        .whereLessThanOrEqualTo("appointmentDate", endOfDay)
                        .get()
                        .addOnSuccessListener { appointmentSnapshot ->
                            val bookedSlotIDs = appointmentSnapshot.documents
                                .mapNotNull { it.toObject(Appointment::class.java)?.slotId }
                                .toSet()

                            launch {
                                // 2. Fetch availability exceptions for the day
                                val exceptions = availabilityExceptionRepository
                                    .getDoctorAvailabilityExceptionsForRange(startOfDay, endOfDay, doctorID)

                                // 3. Filter slots
                                val availableSlots = allSlots.filter { slot ->
                                    val isBooked = bookedSlotIDs.contains(slot.SlotID)
                                    if (isBooked) return@filter false

                                    // Check if the slot falls within any exception period
                                    val slotTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    val slotStartTime = slotTimeFormat.parse(slot.SlotStartTime)

                                    val slotStartCalendar = Calendar.getInstance().apply {
                                        time = date // Base date
                                        val startCal = Calendar.getInstance().apply { time = slotStartTime!! }
                                        set(Calendar.HOUR_OF_DAY, startCal.get(Calendar.HOUR_OF_DAY))
                                        set(Calendar.MINUTE, startCal.get(Calendar.MINUTE))
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }

                                    val slotEndCalendar = Calendar.getInstance().apply {
                                        time = slotStartCalendar.time
                                        add(Calendar.MINUTE, 30) // Assuming 30-minute slots
                                    }

                                    val isUnavailable = exceptions.any { exception ->
                                        val exceptionStart = exception.startDateTime
                                        val exceptionEnd = exception.endDateTime
                                        // Check for overlap: (slotStart < exceptionEnd) and (slotEnd > exceptionStart)
                                        slotStartCalendar.time.before(exceptionEnd) && slotEndCalendar.time.after(exceptionStart)
                                    }

                                    !isUnavailable
                                }
                                trySend(availableSlots).isSuccess
                            }
                        }
                        .addOnFailureListener { e ->
                            close(e)
                        }
                }
            }
        awaitClose { slotsListener.remove() }
    }
}