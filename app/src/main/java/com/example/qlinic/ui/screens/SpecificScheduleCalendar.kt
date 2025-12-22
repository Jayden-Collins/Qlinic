package com.example.qlinic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.qlinic.R
import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AppointmentStatus
import com.example.qlinic.ui.component.CustomDatePicker
import com.example.qlinic.ui.component.MarkAsLeaveDialog
import com.example.qlinic.ui.navigation.Routes
import com.example.qlinic.ui.theme.teal
import com.example.qlinic.ui.viewmodel.ScheduleAppointmentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecificScheduleCalendar(
    navController: NavController,
    modifier: Modifier = Modifier,
    doctorID: String,
    viewModel: ScheduleAppointmentViewModel,
    onDoctorLoaded: (String) -> Unit = {}
){
    // Collect state
    val doctor by viewModel.doctor.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val leaveDates by viewModel.leaveDates.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDates by viewModel.selectedDates.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val appointmentDates = viewModel.getAppointmentDates()
    val appointmentsForSelectedDate = selectedDate?.let { viewModel.getAppointmentsForDate(it) } ?: emptyList()

    var appointmentToCancel by remember { mutableStateOf<Appointment?>(null) }
    var showDeletedDialog by remember { mutableStateOf(false) }
    var showMarkLeaveDialog by remember { mutableStateOf(false) }
    var showMarkLeaveSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(doctorID) {
        viewModel.loadDoctorSchedule(doctorID)
        viewModel.loadAppointmentDetailsByMonth(doctorID, Date())
    }

    LaunchedEffect(doctor) {
        doctor?.let { onDoctorLoaded(it.fullName) }
    }

    val rescheduleSuccessFlow = navController.currentBackStackEntry?.savedStateHandle?.getStateFlow("reschedule_success", false)
    val rescheduleSuccess by (rescheduleSuccessFlow?.collectAsState() ?: remember { mutableStateOf(false) })

    LaunchedEffect(rescheduleSuccess) {
        if (rescheduleSuccess) {
            viewModel.loadAppointmentDetailsByMonth(doctorID, Date())
            navController.currentBackStackEntry?.savedStateHandle?.set("reschedule_success", false)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(doctor?.fullName ?: "Doctor Schedule") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("book_appointment/${doctorID}")
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Book Appointment")
            }
        }
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(text = errorMessage ?: "Error", color = MaterialTheme.colorScheme.error)
                    }
                }
                doctor == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Doctor not found")
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            CustomDatePicker(
                                selectedDates = selectedDates,
                                appointmentDates = appointmentDates,
                                leaveDates = leaveDates,
                                disablePastDates = true,
                                onDateSelected = { date ->
                                    if (selectedDate == date) {
                                        viewModel.selectDate(null)
                                    } else {
                                        viewModel.selectDate(date)
                                    }
                                },
                                onMultipleSelectionChanged = { viewModel.selectMultipleDates(it) },
                                enableMultiSelect = true,
                                onMonthChanged = { viewModel.loadAppointmentDetailsByMonth(doctorID, it.time) },
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        item { CalendarLegend(modifier = Modifier.padding(horizontal = 16.dp)) }

                        item {
                            SelectedDateInfo(
                                selectedDate = selectedDate,
                                selectedDates = selectedDates,
                                appointments = appointmentsForSelectedDate,
                                isSelectedDateOnLeave = selectedDate?.let { viewModel.isDateOnLeave(it) } ?: false,
                                onMarkLeave = { showMarkLeaveDialog = true },
                                onCancelLeave = {
                                    val normalized = selectedDates.map { d ->
                                        Calendar.getInstance().apply {
                                            time = d
                                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                        }.time
                                    }
                                    viewModel.cancelLeave(doctorID, normalized)
                                },
                                onClearSelection = { viewModel.selectMultipleDates(emptyList()) }
                            )
                        }

                        if (appointments.isNotEmpty()) {
                            item {
                                Text(
                                    text = "All Appointments (${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }

                            val appointmentsByDate = appointments.groupBy { appt ->
                                Calendar.getInstance().apply {
                                    time = appt.appointmentDate
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }.time
                            }

                            appointmentsByDate.keys.sorted().forEach { date ->
                                val dateAppointments = (appointmentsByDate[date] ?: emptyList()).sortedBy { viewModel.getSlotStartTime(it.slotId) }
                                item {
                                    Column {
                                        Text(
                                            text = SimpleDateFormat("d MMMM yyyy (EEEE)", Locale.getDefault()).format(date),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )

                                        if (leaveDates.any { viewModel.isSameDay(it, date) }) {
                                            Text("Doctor is on leave", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                                        } else {
                                            dateAppointments.forEach { appointment ->
                                                AppointmentCard(
                                                    appointment = appointment,
                                                    slotStartTime = viewModel.getSlotStartTime(appointment.slotId),
                                                    onEdit = {
                                                        navController.navigate(Routes.DoctorAppointmentReschedule.createRoute(appointment.appointmentId, doctorID, appointment.appointmentDate.time))
                                                    },
                                                    onCancel = { appointmentToCancel = appointment },
                                                    patientName = viewModel.getPatientDisplayName(appointment.patientId)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    if (appointmentToCancel != null) {
        DeleteAppointmentDialog(
            appointment = appointmentToCancel!!,
            doctorName = doctor?.fullName,
            patientName = viewModel.getPatientDisplayName(appointmentToCancel!!.patientId),
            onConfirm = {
                viewModel.deleteAppointment(doctorID, appointmentToCancel!!.appointmentId)
                appointmentToCancel = null
                showDeletedDialog = true
            },
            onDismiss = { appointmentToCancel = null }
        )
    }

    if (showDeletedDialog) {
        DeletedSuccessDialog(title = "Appointment Deleted", message = "Schedule has been successfully updated.", onDone = { showDeletedDialog = false })
    }

    if (showMarkLeaveDialog) {
        MarkAsLeaveDialog(
            show = true,
            dates = selectedDates,
            onConfirm = { dates ->
                val normalized = dates.map { d ->
                    Calendar.getInstance().apply {
                        time = d
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.time
                }
                viewModel.markAsLeave(doctorID, normalized)
                showMarkLeaveDialog = false
                showMarkLeaveSuccessDialog = true
            },
            onDismiss = { showMarkLeaveDialog = false }
        )
    }

    if (showMarkLeaveSuccessDialog) {
        DeletedSuccessDialog(title = "Leave Marked", message = "Selected date(s) have been marked as leave.", onDone = { showMarkLeaveSuccessDialog = false })
    }
}

@Composable
private fun CalendarLegend(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Calendar Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendItem(color = teal.copy(alpha = 0.8f), label = "Appointment Booked")
            LegendItem(color = Color.Black, label = "Selected")
            LegendItem(color = Color.LightGray.copy(alpha = 0.7f), label = "On Leave")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(16.dp).background(color, RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
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
    if (selectedDates.isEmpty()) {
        Column(Modifier.padding(16.dp)) {
            Text("No date selected", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Tap a date to select it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = if (selectedDates.size > 1) "${selectedDates.size} days selected" else SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(selectedDates.first()),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onClearSelection) { Text("Clear") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        when {
            selectedDates.size > 1 -> StatusCard(Color(0xFFE3F2FD), R.drawable.check, "${selectedDates.size} Days", "Multiple selection", "Mark as Leave", onMarkLeave)
            isSelectedDateOnLeave -> StatusCard(Color(0xFFFFEBEE), R.drawable.check, "On Leave", "No appointments", "Cancel Leave", onCancelLeave)
            appointments.isNotEmpty() -> StatusCard(Color(0xFFE8F5E9), R.drawable.check, "${appointments.size} Appointment(s)", "Scheduled", null)
            else -> StatusCard(Color(0xFFE3F2FD), R.drawable.check, "Available", "No appointments", "Mark as Leave", onMarkLeave)
        }
    }
}

@Composable
private fun StatusCard(color: Color, icon: Int, title: String, message: String, actionText: String?, onAction: (() -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), title, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
            if (actionText != null && onAction != null) {
                Button(onClick = onAction, modifier = Modifier.height(36.dp)) { Text(actionText) }
            }
        }
    }
}

@Composable
private fun AppointmentCard(appointment: Appointment, slotStartTime: String?, onEdit: () -> Unit, onCancel: () -> Unit, patientName: String?) {
    val datePart = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(appointment.appointmentDate)
    val timeString = if (slotStartTime != null) "$datePart, $slotStartTime" else SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault()).format(appointment.appointmentDate)

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(timeString, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Patient: ${patientName ?: appointment.patientId}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = appointment.status.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (appointment.status) {
                        AppointmentStatus.UPCOMING -> MaterialTheme.colorScheme.primary
                        AppointmentStatus.COMPLETED -> Color.Green
                        AppointmentStatus.CANCELLED -> Color.Red
                        else -> Color.Gray
                    }
                )
            }
            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expanded = true }) { Icon(painterResource(R.drawable.vector), "Menu") }
                DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Reschedule") }, onClick = { expanded = false; onEdit() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { expanded = false; onCancel() })
                }
            }
        }
    }
}

@Composable
private fun DeleteAppointmentDialog(appointment: Appointment, doctorName: String?, patientName: String?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onConfirm) { Text("Confirm") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Delete Appointment?") },
        text = {
            Column {
                Text("This action is permanent.")
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F6))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Doctor: ${doctorName ?: "-"}")
                        Text("Patient: ${patientName ?: "-"}")
                    }
                }
            }
        }
    )
}

@Composable
private fun DeletedSuccessDialog(title: String, message: String, onDone: () -> Unit) {
    Dialog(onDone) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(message, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}
