package com.example.qlinic.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qlinic.R
import com.example.qlinic.data.model.Slot
import com.example.qlinic.ui.component.DatePickerContent
import com.example.qlinic.ui.component.DayStyle
import com.example.qlinic.ui.component.GenderField
import com.example.qlinic.ui.component.IcField
import com.example.qlinic.ui.component.IcVisualTransformation
import com.example.qlinic.ui.component.NameField
import com.example.qlinic.ui.component.PhoneNumberField
import com.example.qlinic.ui.component.SimplePageScaffold
import com.example.qlinic.ui.theme.darkblue
import com.example.qlinic.ui.theme.teal
import com.example.qlinic.ui.theme.white
import com.example.qlinic.ui.viewmodel.BookApptViewModel
import com.example.qlinic.utils.formatTime

@Composable
fun BookAppt(
    doctorId: String,
    onUpClick: () -> Unit,
    isStaff: Boolean,
    viewModel: BookApptViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val availableSlots by viewModel.availableSlots.collectAsState()
    val selectedSlot by viewModel.selectedSlot.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // States for the popups
    val showSymptomsPopup by viewModel.showSymptomsPopup.collectAsState()
    val showSuccessPopup by viewModel.showSuccessPopup.collectAsState()
    val symptoms by viewModel.symptoms.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    // Staff booking flow states
    val showPatientTypeSelectionPopup by viewModel.showPatientTypeSelectionPopup.collectAsState()
    val showExistingPatientIcPopup by viewModel.showExistingPatientIcPopup.collectAsState()
    val showNewPatientDetailsPopup by viewModel.showNewPatientDetailsPopup.collectAsState()

    LaunchedEffect(doctorId, selectedDate) {
        viewModel.getDoctorSlots(doctorId, selectedDate)
    }

    SimplePageScaffold(
        title = "Book Appointment",
        onUpClick = onUpClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Date",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(Modifier.padding(8.dp))

            DatePickerContent(
                onDismiss = { },
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    viewModel.onDateSelected(date)
                },
                disablePastDates = true,
                dateStyleProvider = { date ->
                    if (date == selectedDate) {
                        DayStyle(
                            backgroundColor = darkblue,
                            textColor = white
                        )
                    } else null
                }
            )

            Spacer(Modifier.padding(16.dp))

            Text(
                text = "Select Time Slot",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(Modifier.padding(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (availableSlots.isNotEmpty()){
                    TimeSlotGrid(
                        modifier = Modifier.weight(1f),
                        slots = availableSlots,
                        selectedSlot = selectedSlot,
                        onSlotSelected = { slot ->
                            viewModel.onSlotSelected(slot)
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No slots available. Please select another date.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            val isButtonEnabled = selectedSlot != null && !isLoading

            Button(
                onClick = { viewModel.onBookAppointmentClick(isStaff) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = teal,
                    contentColor = white,
                    disabledContainerColor = teal.copy(alpha = 0.5f)
                ),
                enabled = isButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp),
            ){
                Text(
                    text = "Book Appointment",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }

    if (showPatientTypeSelectionPopup) {
        PatientTypeSelectionPopup(
            onSelectPatientType = { isNew -> viewModel.onSelectPatientType(isNew) },
            onDismiss = { viewModel.onDismissPatientTypeSelection() }
        )
    }

    if (showExistingPatientIcPopup) {
        val patientIc by viewModel.patientIc.collectAsState()
        val patientIcError by viewModel.patientIcError.collectAsState()
        ExistingPatientIcPopup(
            patientIc = patientIc,
            onPatientIcChanged = { viewModel.onPatientIcChanged(it) },
            error = patientIcError,
            onContinue = { viewModel.onFindExistingPatient() },
            onBack = { viewModel.backToPatientTypeSelection() }
        )
    }

    if (showNewPatientDetailsPopup) {
        val newPatientFirstName by viewModel.newPatientFirstName.collectAsState()
        val newPatientLastName by viewModel.newPatientLastName.collectAsState()
        val newPatientGender by viewModel.newPatientGender.collectAsState()
        val newPatientIc by viewModel.newPatientIc.collectAsState()
        val newPatientPhoneNumber by viewModel.newPatientPhoneNumber.collectAsState()

        NewPatientDetailsPopup(
            firstName = newPatientFirstName,
            onFirstNameChange = { viewModel.onNewPatientInfoChanged(firstName = it) },
            lastName = newPatientLastName,
            onLastNameChange = { viewModel.onNewPatientInfoChanged(lastName = it) },
            gender = newPatientGender,
            onGenderChange = { viewModel.onNewPatientInfoChanged(gender = it) },
            ic = newPatientIc,
            onIcChange = { viewModel.onNewPatientInfoChanged(ic = it) },
            phoneNumber = newPatientPhoneNumber,
            onPhoneNumberChange = { viewModel.onNewPatientInfoChanged(phone = it) },
            onContinue = { viewModel.onCreateNewPatient() },
            onBack = { viewModel.backToPatientTypeSelection() }
        )
    }

    if (showSymptomsPopup) {
        SymptomsDialog(
            symptoms = symptoms,
            onSymptomsChange = { viewModel.onSymptomsChanged(it) },
            onSkip = {
                viewModel.confirmBooking(isStaff = isStaff, doctorId = doctorId, symptoms = "")
            },
            onConfirm = {
                viewModel.confirmBooking(isStaff = isStaff, doctorId = doctorId, symptoms = symptoms)
            },
            isStaff = isStaff
        )
    }

    if (showSuccessPopup) {
        SuccessDialog(
            successMessage = successMessage,
            onDismiss = {
                viewModel.dismissSuccessPopup()
            }
        )
    }
}

@Composable
private fun PatientTypeSelectionPopup(
    onSelectPatientType: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onSelectPatientType(false) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = teal,
                        contentColor = white,
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Existing Patient",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                Button(
                    onClick = { onSelectPatientType(true) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = darkblue,
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "New Patient",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExistingPatientIcPopup(
    patientIc: String,
    onPatientIcChanged: (String) -> Unit,
    error: String?,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = true, dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enter Patient IC:",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = patientIc,
                    onValueChange = onPatientIcChanged,
                    visualTransformation = IcVisualTransformation(),
                    placeholder = { Text("e.g. 010203-07-0001") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                    singleLine = true
                )
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = darkblue,
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    Button(
                        onClick = onContinue,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = teal,
                            contentColor = white,
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewPatientDetailsPopup(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    ic: String,
    onIcChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = true, dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enter Patient Details",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NameField(label = "First Name", placeholder = "e.g. John", value = firstName, onValueChange = onFirstNameChange)
                    NameField(label = "Last Name", placeholder = "e.g. Smith", value = lastName, onValueChange = onLastNameChange)
                    GenderField(selectedGender = gender, onGenderSelected = { gender -> onGenderChange(gender) })
                    IcField(value = ic, onValueChange = onIcChange)
                    PhoneNumberField(phoneNumber = phoneNumber, onPhoneNumberChange = onPhoneNumberChange)
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = darkblue,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    TextButton(
                        onClick = onContinue,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = teal,
                            contentColor = white,
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SymptomsDialog(
    isStaff: Boolean,
    symptoms: String,
    onSymptomsChange: (String) -> Unit,
    onSkip: () -> Unit,
    onConfirm: () -> Unit
) {
    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = true, dismissOnBackPress = false, dismissOnClickOutside = false)
    ){
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enter Symptoms",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = symptoms,
                    onValueChange = onSymptomsChange,
                    placeholder = {
                        if (isStaff) {
                            Text("Optional: Briefly describe the symptoms the patient is experiencing")
                        } else {
                            Text("Optional: Briefly describe the symptoms you are experiencing")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onSkip,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = darkblue,
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    TextButton(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = teal,
                            contentColor = white,
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessDialog(successMessage: String, onDismiss: () -> Unit) {
    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = true, dismissOnBackPress = false, dismissOnClickOutside = false)
    ){
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.booking_success),
                    contentDescription = "Booking Successful Icon",
                    modifier = Modifier.size(96.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Booking Successful",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = successMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = teal,
                        contentColor = white,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                ) {
                    Text(
                        text = "Back to Home",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
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
            TimeSlotItem(
                time = formatTime(slot.SlotStartTime),
                isSelected = slot == selectedSlot,
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
    val backgroundColor = if (isSelected) darkblue else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) white else darkblue

    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = time,
                color = textColor,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
