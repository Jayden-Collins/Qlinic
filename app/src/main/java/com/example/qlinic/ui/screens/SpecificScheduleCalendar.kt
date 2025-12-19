package com.example.qlinic.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.example.qlinic.R
import com.example.qlinic.data.model.Appointment
import com.example.qlinic.ui.component.CustomDatePicker
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.example.qlinic.ui.navigation.SimpleTopBar
import com.example.qlinic.ui.viewModels.ScheduleAppointmentViewModel
import java.util.Date
import com.example.qlinic.ui.navigation.Routes
import com.example.qlinic.ui.theme.teal
import java.util.Calendar
import java.util.TimeZone

// after staff select the view button on doctor appointment schedule screen
// they will be taken to this screen to see the specific schedule calendar of the doctor

@Composable
fun SpecificScheduleCalendar(
    navController: NavController,
    modifier: Modifier = Modifier,
    doctorID: String,
    viewModel: ScheduleAppointmentViewModel
){
    // Collect state
    val doctor by viewModel.doctor.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val leaveDates by viewModel.leaveDates.collectAsState() // Get leave dates
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDates by viewModel.selectedDates.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Get appointment dates for calendar highlighting
    val appointmentDates = viewModel.getAppointmentDates()

    // Get appointments for selected date
    val appointmentsForSelectedDate = selectedDate?.let { viewModel.getAppointmentsForDate(it) } ?: emptyList()

    var appointmentToCancel by remember { mutableStateOf<Appointment?>(null) }
    var showDeletedDialog by remember { mutableStateOf(false) }

    // Load data on initial composition
    LaunchedEffect(doctorID) {
        viewModel.loadDoctorSchedule(doctorID)
    }

    // Observe reschedule result from savedStateHandle so we can refresh when coming back from reschedule screen
    val rescheduleSuccessFlow = navController.currentBackStackEntry?.savedStateHandle?.getStateFlow("reschedule_success", false)
    val rescheduleSuccess by (rescheduleSuccessFlow?.collectAsState() ?: remember { mutableStateOf(false) })

    LaunchedEffect(rescheduleSuccess) {
        if (rescheduleSuccess) {
            // refresh appointments for the visible month
            viewModel.loadAppointmentDetailsByMonth(doctorID, Date())
            // clear the flag so it doesn't trigger repeatedly
            navController.currentBackStackEntry?.savedStateHandle?.set("reschedule_success", false)
        }
    }

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = doctor?.fullName ?: "Doctor",
                onUpClick = { navController.navigateUp() }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage ?: "Error loading schedule",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            doctor == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Doctor not found")
                }
            }

            else -> {
                LazyColumn(
                    modifier = modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    // Calendar Section
                    item {
                        CustomDatePicker(
                            selectedDates = selectedDates,
                            appointmentDates = appointmentDates,
                            leaveDates = leaveDates,
                            onDateSelected = { date ->
                                viewModel.selectDate(date)
                            },
                            onMultipleSelectionChanged = { dates ->
                                viewModel.selectMultipleDates(dates)
                            },
                            enableMultiSelect = true,
                            onMonthChanged = { newCal ->
                                // Load appointments for the visible month when user navigates months
                                viewModel.loadAppointmentDetailsByMonth(doctorID, newCal.time)
                            },
                            modifier = Modifier
                                .padding(16.dp)
                        )
                    }

                    // Legend Section
                    item {
                        CalendarLegend(modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    // selected date status
                    item {
                        SelectedDateInfo(
                            selectedDate = selectedDate,
                            selectedDates = selectedDates,
                            appointments = appointmentsForSelectedDate,
                            isSelectedDateOnLeave = selectedDate?.let { viewModel.isDateOnLeave(it) } ?: false,
                            onMarkLeave = { viewModel.markAsLeave(doctorID, selectedDates) },
                            onCancelLeave = { viewModel.cancelLeave(doctorID, selectedDates) },
                            onClearSelection = { viewModel.selectMultipleDates(emptyList()) }
                        )
                    }

                    // Appointment List for ALL dates (not just selected date)
                    if (appointments.isNotEmpty()) {
                        item {
                            Text(
                                text = "All Appointments (${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }

                        // Group appointments by date
                        val appointmentsByDate = appointments.groupBy { appointment ->
                            val cal = Calendar.getInstance()
                            cal.time = appointment.appointmentDateTime.toDate()
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            cal.time
                        }

                        // Sort dates
                        val sortedDates = appointmentsByDate.keys.sorted()

                        // Show appointments grouped by date
                        sortedDates.forEach { date ->
                            val dateAppointments = appointmentsByDate[date] ?: emptyList()

                            item {
                                Column {
                                    // Date header
                                    Text(
                                        text = SimpleDateFormat("d MMMM yyyy (EEEE)", Locale.getDefault()).format(date),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )

                                    // Check if doctor is on leave for this date
                                    val isDateOnLeave = leaveDates.any { leaveDate ->
                                        viewModel.isSameDay(leaveDate, date)
                                    }

                                    if (isDateOnLeave) {
                                        Text(
                                            text = "Doctor is on leave",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                        )
                                    } else {
                                        // Show appointments for this date
                                        dateAppointments.forEach { appointment ->
                                            val slotStart = viewModel.getSlotStartTime(appointment.slotID)
                                            AppointmentCard(
                                                appointment = appointment,
                                                slotStartTime = slotStart,
                                                onEdit = {
                                                    // Navigate directly to the reschedule screen, pass appointment date to prefill
                                                    val apptDateMillis = appointment.appointmentDateTime.toDate().time
                                                    navController.navigate(Routes.DoctorAppointmentReschedule.createRoute(appointment.appointmentID, doctorID, apptDateMillis))
                                                },
                                                onCancel = {
                                                    appointmentToCancel = appointment
                                                },
                                                patientName = viewModel.getPatientDisplayName(appointment.patientID) // pass name from ViewModel
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "No appointments for ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs must be called from a composable context (not inside LazyColumn's LazyListScope).
    // Show confirmation dialog when user requests to delete an appointment
    if (appointmentToCancel != null) {
        DeleteAppointmentDialog(
            appointment = appointmentToCancel!!,
            doctorName = doctor?.fullName,
            patientName = viewModel.getPatientDisplayName(appointmentToCancel!!.patientID),
            onConfirm = {
                // Call the ViewModel function directly (it launches on viewModelScope). Avoid launching
                // a coroutine tied to the composable scope which can be cancelled when the composition
                // leaves and cause the runtime error shown.
                val apptId = appointmentToCancel?.appointmentID ?: ""
                viewModel.deleteAppointment(doctorID, apptId)
                appointmentToCancel = null
                showDeletedDialog = true
            },
            onDismiss = { appointmentToCancel = null }
        )
    }

    if (showDeletedDialog) {
        DeletedSuccessDialog(
            title = "Appointment Deleted",
            message = "Schedule has been successfully updated.",
            onDone = { showDeletedDialog = false }
        )
    }
}

@Composable
private fun CalendarLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Calendar Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LegendItem(
                color = teal.copy(alpha = 0.8f),
                label = "Appointment Booked"
            )
            LegendItem(
                color = Color.Black,
                label = "Selected and Multiple-Selected"
            )
            LegendItem(
                color = Color.LightGray.copy(alpha = 0.7f),
                label = "On Leave Date"
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SelectedDateInfo(
    selectedDate: Date?,
    selectedDates: List<Date>,
    appointments: List<Appointment>,
    isSelectedDateOnLeave: Boolean,
    onMarkLeave: () -> Unit,
    onCancelLeave: () -> Unit,
    onClearSelection: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
    val isMultiSelect = selectedDates.size > 1

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // If nothing is selected, show a placeholder and return
        if (selectedDates.isEmpty()) {
            Text(
                text = "No date selected",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap a date to select it. Tap again to unselect.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isMultiSelect) {
                        "${selectedDates.size} days selected"
                    } else {
                        // Prefer selectedDate when available, fallback to first selectedDates
                        selectedDate?.let { dateFormatter.format(it) }
                            ?: selectedDates.firstOrNull()?.let { dateFormatter.format(it) }
                            ?: "No date selected"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (isMultiSelect) {
                    Text(
                        text = selectedDates.joinToString(", ") {
                            SimpleDateFormat("MMM d", Locale.getDefault()).format(it)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (selectedDates.isNotEmpty()) {
                TextButton(onClick = onClearSelection) {
                    Text("Clear Selection")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Status card or multi-select actions
        when {
            isMultiSelect -> {
                MultiSelectActionsCard(
                    count = selectedDates.size,
                    onMarkLeave = onMarkLeave,
                    onClearSelection = onClearSelection
                )
            }
            isSelectedDateOnLeave -> {
                StatusCard(
                    color = Color(0xFFFFEBEE),
                    icon = R.drawable.check,
                    title = "Doctor is on leave",
                    message = "No appointments available",
                    actionText = "Cancel Leave",
                    onAction = onCancelLeave
                )
            }
            appointments.isNotEmpty() -> {
                StatusCard(
                    color = Color(0xFFE8F5E9),
                    icon = R.drawable.check,
                    title = "${appointments.size} Appointments",
                    message = "${appointments.size} appointment(s) scheduled",
                    actionText = null
                )
            }
            else -> {
                StatusCard(
                    color = Color(0xFFE3F2FD),
                    icon = R.drawable.check,
                    title = "Available",
                    message = "No appointments scheduled",
                    actionText = "Mark as Leave",
                    onAction = onMarkLeave
                )
            }
        }
    }
}

@Composable
private fun MultiSelectActionsCard(
    count: Int,
    onMarkLeave: () -> Unit,
    onClearSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "$count days selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "What would you like to do with these dates?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onMarkLeave,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.home_2),
                        contentDescription = "Mark Leave",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark as Leave")
                }

                OutlinedButton(
                    onClick = onClearSelection,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.home_2),
                        contentDescription = "Cancel Leave",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Selection")
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    color: Color,
    icon: Int,
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = title,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (actionText != null && onAction != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAction,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(actionText)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    slotStartTime: String? = null,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    patientName: String? = null // optional patient name passed from ViewModel
) {
    // Use slot start time when available, otherwise use stored timestamp
    val appointmentDate = appointment.appointmentDateTime.toDate()

    // Date part
    val datePart = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(appointmentDate)

    // Format provided slot start time if possible (tolerate HH:mm or hh:mm a)
    val timePart = slotStartTime?.let { raw ->
        if (raw.isBlank()) return@let null
        // Try parsing common time formats and extract hour/minute
        val patterns = listOf("HH:mm", "H:mm", "hh:mm a", "h:mm a", "HH:mm:ss", "H:mm:ss")
        var parsed: java.util.Date? = null
        for (pat in patterns) {
            try {
                val fmt = SimpleDateFormat(pat, Locale.getDefault())
                parsed = fmt.parse(raw)
                if (parsed != null) break
            } catch (_: Exception) { }
        }

        // If we could parse a time, combine it with the appointment date so the date portion stays accurate
        if (parsed != null) {
            val timeCal = Calendar.getInstance().apply { time = parsed }
            val apptCal = Calendar.getInstance().apply { time = appointmentDate }
            apptCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
            apptCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
            apptCal.set(Calendar.SECOND, 0)
            apptCal.set(Calendar.MILLISECOND, 0)
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(apptCal.time)
        } else {
            // Couldn't parse; use raw string as-is
            raw
        }
    }

    val timeString = if (timePart != null) "$datePart, $timePart" else {
        // fallback to appointment timestamp formatted
        SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault()).format(appointmentDate)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Full Date + Time (prefer slot time)
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Patient Name (fall back to ID if name not provided)
                        Text(
                            text = "Patient: ${patientName ?: appointment.patientID}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Symptoms if available
                        if (appointment.symptoms.isNotEmpty()) {
                            Text(
                                text = "Symptoms: ${appointment.symptoms}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Status
                        Text(
                            text = appointment.status.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (appointment.status.lowercase()) {
                                "booked" -> MaterialTheme.colorScheme.primary
                                "completed" -> Color.Green
                                "cancelled" -> Color.Red
                                else -> Color.Gray
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Single-tap action icon: navigate directly to reschedule (onEdit) or cancel
                    Box {
                        // Combined overflow menu for actions (Reschedule / Delete)
                        var menuExpanded by remember { mutableStateOf(false) }

                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                            Icon(painter = painterResource(R.drawable.vector), contentDescription = "Actions")
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("Reschedule") }, onClick = {
                                menuExpanded = false
                                onEdit()
                            })
                            DropdownMenuItem(text = { Text("Delete") }, onClick = {
                                menuExpanded = false
                                onCancel()
                            })
                        }
                    }
                }
            }
        }
    }
}

// Update DeleteAppointmentDialog to use explicit timezone formatting for the shown date
@Composable
private fun DeleteAppointmentDialog(
    appointment: Appointment,
    doctorName: String?,
    patientName: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dialogFormat = SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) { Text("Confirm") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Delete Appointment?", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                // Alert image at the center
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.alert),
                        contentDescription = "Alert",
                        modifier = Modifier.size(96.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Text("This action is non-revertible. By confirming, you will permanently delete this appointment including relevant data.")
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Doctor: ${doctorName ?: "-"}")
                        Text("Patient: ${patientName ?: appointment.patientID}")
                        Text("Date: ${dialogFormat.format(appointment.appointmentDateTime.toDate())}")
                    }
                }
            }
        }
    )
}

@Composable
private fun DeletedSuccessDialog(
    title: String,
    message: String,
    onDone: () -> Unit
) {
    Dialog(onDismissRequest = onDone) {
        Surface(
            modifier = Modifier
                .padding(20.dp)
                .widthIn(min = 280.dp, max = 420.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top illustration
                Image(
                    painter = painterResource(id = R.drawable.illustration_success),
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bold title centered
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle / description
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Done button fills max width
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}
