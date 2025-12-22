package com.example.qlinic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.qlinic.data.model.Slot
import com.example.qlinic.ui.component.DatePickerContent
import com.example.qlinic.ui.component.DayStyle
import com.example.qlinic.ui.navigation.Routes
import com.example.qlinic.ui.component.SimpleTopBar
import com.example.qlinic.ui.theme.teal
import com.example.qlinic.ui.theme.white
import com.example.qlinic.ui.viewmodel.ScheduleAppointmentViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import kotlinx.coroutines.CancellationException

@Composable
fun RescheduleAppointmentScreen(
    navController: NavController,
    appointmentId: String,
    doctorId: String,
    initialDateMillis: Long? = null,
    viewModel: ScheduleAppointmentViewModel
) {
    // Collect leave dates state to ensure UI recomposes when they are loaded
    val leaveDates by viewModel.leaveDates.collectAsState()
    val leaveDatesLoaded by viewModel.leaveDatesLoaded.collectAsState()

    // selected date state - initialize from initialDateMillis or today
    var selectedDate by remember {
        mutableStateOf(
            initialDateMillis?.let { Date(it) } ?: Date()
        )
    }

    // slots state
    var availableSlots by remember { mutableStateOf<List<Slot>>(emptyList()) }
    var selectedSlot by remember { mutableStateOf<Slot?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // dialogs
    var showConfirmDialog by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // listen for slots when selectedDate changes
    val normalizedSelectedDate = remember(selectedDate) {
        Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
    }

    // Initial load of leave dates for the month
    LaunchedEffect(doctorId) {
        viewModel.loadLeaveDates(doctorId, Date())
        viewModel.loadAvailabilityExceptions(doctorId)
    }

    LaunchedEffect(normalizedSelectedDate, doctorId) {
        // clear previous selection whenever date changes
        selectedSlot = null
        isLoading = true
        val date = normalizedSelectedDate
        if (date == null) {
            availableSlots = emptyList()
            isLoading = false
            return@LaunchedEffect
        }
        try {
            // Fetch availability for the selected month immediately if not loaded
            viewModel.loadLeaveDates(doctorId, date)
            viewModel.loadAvailabilityExceptions(doctorId)
        } catch (_: Exception) { }
        try {
            viewModel.listenForSlots(doctorId, date).collectLatest { slots ->
                availableSlots = slots
                isLoading = false
                if (selectedSlot != null && slots.none { it.SlotID == selectedSlot?.SlotID }) {
                    selectedSlot = null
                }
            }
        } catch (_: CancellationException) {
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            errorMsg = e.message ?: "Error loading time slots"
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            SimpleTopBar(title = "Reschedule Appointment", onUpClick = { navController.navigateUp() })
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = selectedSlot != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = teal,
                        contentColor = white
                    )
                ) {
                    Text("Confirm", style = MaterialTheme.typography.displayMedium)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Select Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (!leaveDatesLoaded) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading availability...")
                }
            }

            DatePickerContent(
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    if (!leaveDatesLoaded) {
                        errorMsg = "Loading availability, please wait"
                        return@DatePickerContent
                    }

                    if (viewModel.isDateOnLeave(date)) {
                        errorMsg = "Doctor is on leave on selected date"
                        return@DatePickerContent
                    }

                    selectedDate = date
                },
                disablePastDates = true,
                leaveDates = leaveDates,
                dateStyleProvider = { date ->
                    // Use collected leaveDates or ViewModel check
                    when {
                        viewModel.isDateOnLeave(date) -> DayStyle(backgroundColor = Color.LightGray.copy(alpha = 0.9f), textColor = Color.Gray, hasAppointment = false, isOnLeave = true)
                        viewModel.isDateBooked(date) -> DayStyle(backgroundColor = teal.copy(alpha = 0.9f), textColor = Color.White, hasAppointment = true, isOnLeave = false)
                        else -> null
                    }
                },
                onMonthChanged = { cal ->
                    viewModel.refreshLeaveDates(doctorId, cal.time)
                    viewModel.refreshAvailabilityExceptions(doctorId)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Select Time Slot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (availableSlots.isEmpty()) {
                Text("No available time slots for selected date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                TimeSlotGrid(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    slots = availableSlots,
                    selectedSlot = selectedSlot,
                    onSlotSelected = { slot ->
                        // Single select toggle logic: click again to unselect
                        selectedSlot = if (selectedSlot?.SlotID == slot.SlotID) null else slot
                    }
                )
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
                        viewModel.rescheduleAppointment(slot, appointmentId, doctorId, newAppointmentDate) { success, err ->
                            if (success) {
                                val previousEntry = navController.previousBackStackEntry
                                if (previousEntry != null) {
                                    previousEntry.savedStateHandle.set("reschedule_success", true)
                                    navController.popBackStack()
                                } else {
                                    try {
                                        navController.navigate(Routes.DoctorAppointmentSchedule.createRoute(doctorId)) {
                                            launchSingleTop = true
                                        }
                                    } catch (_: Exception) {
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
            val isSelected = selectedSlot?.SlotID == slot.SlotID
            Surface(
                onClick = { onSlotSelected(slot) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) com.example.qlinic.ui.theme.darkblue else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = formatSlotLabel(slot.SlotStartTime),
                        color = if (isSelected) white else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
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
            val patterns = listOf("HH:mm", "H:mm", "hh:mm a", "h:mm a")
            var parsed: Date? = null
            for (pat in patterns) {
                try {
                    val fmt = SimpleDateFormat(pat, Locale.getDefault())
                    parsed = fmt.parse(slotStartTime)
                    if (parsed != null) break
                } catch (_: Exception) { }
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
