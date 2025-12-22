package com.example.qlinic.data.repository

import android.util.Log
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
import kotlinx.coroutines.tasks.await
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
            .whereEqualTo("DoctorID", doctorID)
            .whereEqualTo("DayOfWeek", dayOfWeek)

        if (isToday(date)) {
            val currentTime = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date())
            query = query.whereGreaterThanOrEqualTo("SlotStartTime", currentTime)
        }

        // Listen for changes in the doctor's slots
        val slotsListener = query.addSnapshotListener { slotsSnapshot, error ->
                if (error != null) {
                    Log.e("SlotRepository", "Error fetching slots", error)
                    close(error) // Close the flow on error
                    return@addSnapshotListener
                }

                if (slotsSnapshot == null) {
                    Log.w("SlotRepository", "Slots snapshot is null, but no error reported.")
                    trySend(emptyList()) // Send an empty list to avoid hanging
                    return@addSnapshotListener
                }

                val allSlots = slotsSnapshot.documents.mapNotNull { it.toObject(Slot::class.java) }
                Log.d("SlotRepository", "Fetched ${allSlots.size} slots for doctor $doctorID on $date")

                // Log all fetched slots
                Log.d("SlotRepository", "Fetched slots:")
                allSlots.forEach { slot ->
                    Log.d("SlotRepository", "SlotID=${slot.SlotID}, StartTime=${slot.SlotStartTime}, DayOfWeek=${slot.DayOfWeek}")
                }

                // Launch a coroutine to handle asynchronous filtering
                launch {
                    try {
                        // 1. Fetch booked appointments for the day using await()
                        val appointmentSnapshot = db.collection("Appointment")
                            .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                            .whereLessThanOrEqualTo("appointmentDate", endOfDay)
                            .get()
                            .await()

                        val bookedSlotIDs = appointmentSnapshot.documents
                            .filter { doc ->
                                val status = doc.getString("status")
                                Log.d("SlotRepository", "AppointmentID=${doc.id}, Status=$status")
                                status != "Cancelled" && status != "No Show"
                            }
                            .mapNotNull { it.getString("slotId") }
                            .toSet()
                        Log.d("SlotRepository", "Successfully fetched ${bookedSlotIDs.size} booked appointments (excluding cancelled/no_show).")

                        // Log booked slot IDs
                        Log.d("SlotRepository", "Booked slot IDs: $bookedSlotIDs")

                        // 2. Fetch availability exceptions for the day
                        val exceptions = availabilityExceptionRepository
                            .getDoctorAvailabilityExceptionsForRange(startOfDay, endOfDay, doctorID)
                        Log.d("SlotRepository", "Found ${exceptions.size} exceptions.")

                        // 3. Filter slots
                        val availableSlots = allSlots.filter { slot ->
                            val isBooked = bookedSlotIDs.contains(slot.SlotID)
                            if (isBooked) {
                                Log.d("SlotRepository", "SlotID=${slot.SlotID} excluded: booked")
                                return@filter false
                            }

                            try {
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
                                    slotStartCalendar.time.before(exceptionEnd) && slotEndCalendar.time.after(exceptionStart)
                                }

                                if (isUnavailable) {
                                    Log.d("SlotRepository", "SlotID=${slot.SlotID} excluded: unavailable (exception overlap)")
                                } else {
                                    Log.d("SlotRepository", "SlotID=${slot.SlotID} included")
                                }
                                !isUnavailable
                            } catch (e: Exception) {
                                Log.d("SlotRepository", "SlotID=${slot.SlotID} excluded: exception: ${e.message}")
                                false
                            }
                        }

                        // Log available slots after filtering
                        Log.d("SlotRepository", "Available slots after filtering:")
                        availableSlots.forEach { slot ->
                            Log.d("SlotRepository", "SlotID=${slot.SlotID}, StartTime=${slot.SlotStartTime}, DayOfWeek=${slot.DayOfWeek}")
                        }

                        Log.d("SlotRepository", "Final available slots count: ${availableSlots.size}. Sending to flow.")
                        trySend(availableSlots)

                    } catch (e: Exception) {
                        Log.e("SlotRepository", "Error filtering slots", e)
                        close(e) // Close the flow on any exception
                    }
                }
            }

        // This will be called when the Flow is cancelled or closed
        awaitClose {
            Log.d("SlotRepository", "Closing slots listener.")
            slotsListener.remove()
        }
    }
}