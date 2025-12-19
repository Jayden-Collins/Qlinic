package com.example.qlinic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.example.qlinic.ui.navigation.SimpleTopBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.qlinic.ui.viewModels.ScheduleAppointmentViewModel
import java.util.*
import com.example.qlinic.ui.component.DatePickerContent
import com.example.qlinic.ui.component.DayStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import com.example.qlinic.data.model.Slot
import java.text.SimpleDateFormat
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.collectAsState
import com.example.qlinic.ui.theme.teal
import com.example.qlinic.ui.navigation.Routes
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign

@Composable
fun RescheduleAppointmentScreen(
    navController: NavController,
    appointmentId: String,
    doctorId: String,
    initialDateMillis: Long? = null,
    viewModel: ScheduleAppointmentViewModel
) {
    // selected date state - initialize from initialDateMillis or today
    var selectedDate by remember {
        mutableStateOf(
            initialDateMillis?.let { Date(it) } ?: Date()
        )
    }

    // slots state
    var availableSlots by remember { mutableStateOf<List<Slot>>(emptyList()) }
    var selectedSlot by remember { mutableStateOf<Slot?>(null) }

    // dialogs
    var showConfirmDialog by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // listen for slots when selectedDate changes
    // Normalize selectedDate to start of day for effect key (prevents unnecessary restarts when time parts differ)
    val normalizedSelectedDate = remember(selectedDate) {
        Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
    }

    LaunchedEffect(normalizedSelectedDate, doctorId) {
        // clear previous selection whenever date changes
        selectedSlot = null

        val date = normalizedSelectedDate
        if (date == null) {
            availableSlots = emptyList()
            return@LaunchedEffect
        }

        // Ensure leave dates and exceptions for this month are loaded before we start collecting slots.
        // This reduces a race where slots for an on-leave day are emitted before the UI knows the day is on leave.
        try {
            viewModel.refreshLeaveDates(doctorId, date)
            viewModel.refreshAvailabilityExceptions(doctorId)
        } catch (_: Exception) { /* non-fatal, continue to load slots */ }

        try {
            viewModel.listenForSlots(doctorId, date).collectLatest { slots ->
                availableSlots = slots
                // clear selected slot if it's no longer available
                if (selectedSlot != null && slots.none { it.SlotID == selectedSlot?.SlotID }) {
                    selectedSlot = null
                }
            }
        } catch (_: CancellationException) {
            // ignore cancellation caused by composition changes
        } catch (e: Exception) {
            errorMsg = e.message ?: "Error loading time slots"
        }
    }

    // Ensure leave dates and availability exceptions are loaded for the date/month shown in the reschedule screen.
    // Without loading leave dates the date picker cannot disable on-leave days and user could pick a leave day.
    LaunchedEffect(normalizedSelectedDate, doctorId) {
        try {
            // load leave dates for the visible month (the ViewModel method is suspend)
            viewModel.loadLeaveDates(doctorId, normalizedSelectedDate)

            // also refresh availability exceptions which are used server-side when filtering slots
            viewModel.loadAvailabilityExceptions(doctorId)
        } catch (e: Exception) {
            // non-fatal: surface to UI
            if (e is CancellationException) {
                // ignore cancellation when composition changes
            } else {
                errorMsg = e.message ?: "Error loading availability"
            }
        }
    }

    Scaffold(
        topBar = {
            SimpleTopBar(title = "Reschedule Appointment", onUpClick = { navController.navigateUp() })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Select Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // show inline date picker content (single-date)
            // prevent selection until leave dates are loaded to avoid accidental selection of on-leave days
            val leaveDatesLoaded by viewModel.leaveDatesLoaded.collectAsState()

            if (!leaveDatesLoaded) {
                // show a small inline loading indicator so user knows availability is being fetched
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading availability...")
                }
            }

            DatePickerContent(
                onDismiss = {}, // embedded usage
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    // Block selection until leave data is loaded
                    if (!leaveDatesLoaded) {
                        errorMsg = "Loading availability, please wait"
                        return@DatePickerContent
                    }

                    // Prevent selecting a leave date even if the grid enabled it due to race
                    if (viewModel.isDateOnLeave(date)) {
                        errorMsg = "Doctor is on leave on selected date"
                        return@DatePickerContent
                    }

                    selectedDate = date
                },
                disablePastDates = true,
                dateStyleProvider = { date ->
                    when {
                        viewModel.isDateOnLeave(date) -> DayStyle(backgroundColor = Color.LightGray.copy(alpha = 0.9f), textColor = Color.Gray, hasAppointment = false, isOnLeave = true)
                        viewModel.isDateBooked(date) -> DayStyle(backgroundColor = teal.copy(alpha = 0.9f), textColor = Color.White, hasAppointment = true, isOnLeave = false)
                        else -> null
                    }
                },
                onMonthChanged = { cal ->
                    // load leave dates and availability exceptions for the month visible in the picker
                    viewModel.refreshLeaveDates(doctorId, cal.time)
                    // refresh exceptions used by slot filtering
                    viewModel.refreshAvailabilityExceptions(doctorId)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Select Time Slot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (availableSlots.isEmpty()) {
                Text("No available time slots for selected date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // Use a grid to show slots (3 columns)
                TimeSlotGrid(
                    modifier = Modifier.fillMaxWidth(),
                    slots = availableSlots,
                    selectedSlot = selectedSlot,
                    onSlotSelected = { selectedSlot = it }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showConfirmDialog = true },
                enabled = selectedSlot != null,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Confirm")
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Changes?") },
            text = { Text("By clicking confirm, this appointment details will be updated.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    val slot = selectedSlot
                    if (slot != null) {
                        val newAppointmentDate = buildDateWithSlot(selectedDate, slot.SlotStartTime)
                        viewModel.rescheduleAppointment(slot, appointmentId, newAppointmentDate) { success, err ->
                            if (success) {
                                // Prefer setting the flag on the previous back stack entry (if present)
                                // then pop back. Using previousBackStackEntry is more reliable than
                                // calling currentBackStackEntry after popBackStack because the
                                // current entry may not be updated yet or may be null.
                                val previousEntry = navController.previousBackStackEntry
                                if (previousEntry != null) {
                                    previousEntry.savedStateHandle.set("reschedule_success", true)
                                    navController.popBackStack()
                                } else {
                                    // If no previous entry, navigate directly to the doctor's calendar route
                                    try {
                                        navController.navigate(Routes.DoctorAppointmentSchedule.createRoute(doctorId)) {
                                            launchSingleTop = true
                                        }
                                    } catch (_: Exception) {
                                        // last-resort: navigate up
                                        navController.navigateUp()
                                    }
                                }
                            } else {
                                errorMsg = err ?: "Unknown error"
                            }
                        }
                     }
                 }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") } }
        )
    }

    if (errorMsg != null) {
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = { Text("Error") },
            text = { Text(errorMsg ?: "") },
            confirmButton = { TextButton(onClick = { errorMsg = null }) { Text("OK") } }
        )
    }
}

private fun formatSlotLabel(slotTime: String?): String {
    if (slotTime == null) return ""
    return if (slotTime.contains("AM") || slotTime.contains("PM") || slotTime.contains("am") || slotTime.contains("pm")) {
        slotTime
    } else {
        try {
            val src = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dst = SimpleDateFormat("hh:mm a", Locale.getDefault())
            dst.format(src.parse(slotTime) ?: Date())
        } catch (_: Exception) { slotTime }
    }
}

private fun buildDateWithSlot(date: Date, slotStartTime: String?): Date {
    val cal = Calendar.getInstance().apply { time = date }
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    if (slotStartTime != null) {
        try {
            // Try multiple time formats to tolerate inconsistent stored formats (e.g. "08:30", "8:30", "08:30 AM")
            val patterns = listOf("HH:mm", "H:mm", "hh:mm a", "h:mm a")
            var parsed: Date? = null
            for (pat in patterns) {
                try {
                    val fmt = SimpleDateFormat(pat, Locale.getDefault())
                    parsed = fmt.parse(slotStartTime)
                    if (parsed != null) break
                } catch (_: Exception) { /* try next pattern */ }
            }

            if (parsed != null) {
                val tmp = Calendar.getInstance().apply { time = parsed }
                cal.set(Calendar.HOUR_OF_DAY, tmp.get(Calendar.HOUR_OF_DAY))
                cal.set(Calendar.MINUTE, tmp.get(Calendar.MINUTE))
            }
        } catch (_: Exception) {
        }
    }
    return cal.time
}

// Grid/Item composables for time slots
@Composable
private fun TimeSlotGrid(
    modifier: Modifier = Modifier,
    slots: List<Slot>,
    selectedSlot: Slot?,
    onSlotSelected: (Slot) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(slots) { slot ->
            TimeSlotItem(
                time = formatSlotLabel(slot.SlotStartTime),
                isSelected = selectedSlot?.SlotID == slot.SlotID,
                onClick = { onSlotSelected(slot) }
            )
        }
    }
}

@Composable
private fun TimeSlotItem(
    time: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Define colors (replace undefined variables from your snippet)
    val darkBlue = Color(0xFF0D47A1)
    val white = Color.White
    val backgroundColor = if (isSelected) darkBlue else MaterialTheme.colorScheme.surface
    val textColor = if (isSelected) white else darkBlue

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = time,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
