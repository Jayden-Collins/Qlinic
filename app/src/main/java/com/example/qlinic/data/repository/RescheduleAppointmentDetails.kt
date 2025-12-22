package com.example.qlinic.data.repository

import com.example.qlinic.data.model.AvailabilityException
import com.example.qlinic.data.model.Slot
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale

class RescheduleAppointmentDetails {
    private val firestore: FirebaseFirestore = Firebase.firestore

    // get doctor availability exceptions (on-leave / unavailable periods)
    suspend fun getDoctorAvailabilityExceptions(doctorID: String): List<AvailabilityException> {
        return try {
            // Query by doctorID only to avoid requiring a composite index for combined queries.
            val snapshot = firestore.collection("AvailabilityException")
                .whereEqualTo("doctorID", doctorID)
                .get()
                .await()

            val nowTs = Timestamp.now()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val endTs = doc.getTimestamp("endDateTime")?.toDate() ?: Date(0)
                    // filter out already expired exceptions on the client side
                    if (endTs.after(nowTs.toDate())) {
                        AvailabilityException(
                            exceptionID = doc.getString("exceptionID") ?: doc.id,
                            startDateTime = doc.getTimestamp("startDateTime")?.toDate() ?: Date(),
                            endDateTime = endTs,
                            reason = doc.getString("reason") ?: "",
                            doctorID = doc.getString("doctorID") ?: "",
                            staffID = doc.getString("staffID") ?: "",
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    println("Error fetching availability exception ${doc.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching availability exceptions: ${e.message}")
            emptyList()
        }
    }

    // fetch the available slots for a doctor on a specific date
    fun listenForDoctorSlotsByDate(doctorID: String, date: Date): Flow<List<Slot>> =
        callbackFlow {
            val producer = this
            val db = FirebaseFirestore.getInstance()

            // compute start and end of the given date (local timezone)
            val startCal = java.util.Calendar.getInstance().apply {
                time = date
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val endCal = java.util.Calendar.getInstance().apply {
                time = startCal.time
                add(java.util.Calendar.DATE, 1)
                add(java.util.Calendar.MILLISECOND, -1)
            }
            val startOfDay: Date = startCal.time
            val endOfDay: Date = endCal.time

            val dayOfWeek =
                java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(date)

            var query: com.google.firebase.firestore.Query = db.collection("Slot")
                .whereEqualTo("DoctorID", doctorID)
                .whereEqualTo("DayOfWeek", dayOfWeek)

            // inline isToday check
            val todayCal = java.util.Calendar.getInstance()
            val isToday =
                todayCal.get(java.util.Calendar.YEAR) == startCal.get(java.util.Calendar.YEAR) &&
                        todayCal.get(java.util.Calendar.DAY_OF_YEAR) == startCal.get(java.util.Calendar.DAY_OF_YEAR)

            if (isToday) {
                val currentTime =
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.ENGLISH).format(Date())
                query = query.whereGreaterThanOrEqualTo("SlotStartTime", currentTime)
            }

            val slotsListener =
                query.addSnapshotListener { slotsSnapshot: com.google.firebase.firestore.QuerySnapshot?, error: com.google.firebase.firestore.FirebaseFirestoreException? ->
                    if (error != null) {
                        android.util.Log.e("SlotRepository", "Error fetching slots", error)
                        producer.close(error)
                        return@addSnapshotListener
                    }

                    if (slotsSnapshot == null) {
                        android.util.Log.w(
                            "SlotRepository",
                            "Slots snapshot is null, but no error reported."
                        )
                        producer.trySend(emptyList<Slot>())
                        return@addSnapshotListener
                    }

                    val allSlots =
                        slotsSnapshot.documents.mapNotNull { it.toObject(Slot::class.java) }
                    android.util.Log.d(
                        "SlotRepository",
                        "Fetched ${allSlots.size} slots for doctor $doctorID on $date"
                    )

                    // launch a coroutine tied to the callbackFlow producer scope so it is cancelled when the flow closes
                    producer.launch {
                        try {
                            // fetch appointments for the day (using Date range)
                            val appointmentSnapshot = db.collection("Appointment")
                                .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                                .whereLessThanOrEqualTo("appointmentDate", endOfDay)
                                .get()
                                .await()

                            val bookedSlotIDs = appointmentSnapshot.documents
                                .filter { doc ->
                                    val status = doc.getString("status")
                                    status != "Cancelled" && status != "No Show"
                                }
                                .mapNotNull { it.getString("slotId") }
                                .toSet()
                            android.util.Log.d(
                                "SlotRepository",
                                "Successfully fetched ${bookedSlotIDs.size} booked appointments (excluding cancelled/no_show)."
                            )

                            // reuse repository method in this class to get exceptions and filter by day range
                            val exceptionsAll = try {
                                getDoctorAvailabilityExceptions(doctorID)
                            } catch (e: Exception) {
                                emptyList<com.example.qlinic.data.model.AvailabilityException>()
                            }

                            val exceptions = exceptionsAll.filter { ex ->
                                val exStart = ex.startDateTime
                                val exEnd = ex.endDateTime
                                // overlap with day
                                exStart.before(endOfDay) && exEnd.after(startOfDay)
                            }
                            android.util.Log.d(
                                "SlotRepository",
                                "Found ${exceptions.size} exceptions for day."
                            )

                            // filter slots
                            val slotTimeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

                            val availableSlots = allSlots.filter { slot ->
                                try {
                                    if (bookedSlotIDs.contains(slot.SlotID)) return@filter false

                                    // Try multiple time formats to tolerate inconsistent stored formats
                                    val parsed = run {
                                        val patterns = listOf("HH:mm", "H:mm", "hh:mm a", "h:mm a")
                                        var p: java.util.Date? = null
                                        for (pat in patterns) {
                                            try {
                                                val fmt = java.text.SimpleDateFormat(pat, java.util.Locale.getDefault())
                                                p = fmt.parse(slot.SlotStartTime)
                                                if (p != null) break
                                            } catch (_: Exception) { }
                                        }
                                        p
                                    }
                                    if (parsed == null) return@filter false

                                    val slotStartCal = java.util.Calendar.getInstance().apply {
                                        time = date
                                        val tmp = java.util.Calendar.getInstance().apply { time = parsed }
                                        set(java.util.Calendar.HOUR_OF_DAY, tmp.get(java.util.Calendar.HOUR_OF_DAY))
                                        set(java.util.Calendar.MINUTE, tmp.get(java.util.Calendar.MINUTE))
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }
                                    val slotEndCal = java.util.Calendar.getInstance().apply { time = slotStartCal.time; add(java.util.Calendar.MINUTE, 30) }

                                    val isUnavailable = exceptions.any { ex ->
                                        val exStart = ex.startDateTime
                                        val exEnd = ex.endDateTime
                                        slotStartCal.time.before(exEnd) && slotEndCal.time.after(exStart)
                                    }

                                    !isUnavailable
                                } catch (e: Exception) {
                                    false
                                }
                            }

                            // Sort available slots by start time
                            val sortedSlots = availableSlots.sortedBy { it.SlotStartTime }
                            android.util.Log.d(
                                "SlotRepository",
                                "Final available slots count: ${sortedSlots.size}. Sending to flow."
                            )
                            producer.trySend(sortedSlots)
                        } catch (e: Exception) {
                            android.util.Log.e("SlotRepository", "Error filtering slots", e)
                            producer.close(e)
                        }
                    }
                }

             awaitClose {
                 android.util.Log.d("SlotRepository", "Closing slots listener.")
                 slotsListener.remove()
             }
         }

    // reschedule appointment by updating its slotID and appointmentDate
    suspend fun rescheduleAppointment(
        slot: Slot,
        appointmentID: String,
        newAppointmentDate: Date
    ){
        try {
            val appointmentRef = firestore.collection("Appointment").document(appointmentID)

            val updates = mapOf(
                "slotId" to slot.SlotID,
                "appointmentDate" to newAppointmentDate
            )

            appointmentRef.update(updates).await()
            println("Successfully rescheduled appointment $appointmentID")
        } catch (e: Exception) {
            println("Error rescheduling appointment $appointmentID: ${e.message}")
            throw e
        }

    }
}
