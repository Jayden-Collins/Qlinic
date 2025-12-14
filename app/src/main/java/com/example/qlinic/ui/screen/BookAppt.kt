package com.example.qlinic.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qlinic.data.model.Slot
import com.example.qlinic.ui.component.DatePickerContent
import com.example.qlinic.ui.component.DayStyle
import com.example.qlinic.ui.component.SimplePageScaffold
import com.example.qlinic.ui.theme.darkblue
import com.example.qlinic.ui.theme.white
import com.example.qlinic.ui.theme.teal
import com.example.qlinic.ui.viewmodel.BookApptViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookAppt(
    onUpClick: () -> Unit,
    viewModel: BookApptViewModel = viewModel()
) {
    val user = Firebase.firestore.collection("Patient").document("v9i0pTJ4KtKUJ77SQwfg")

    val selectedDoctor = "kjQsFb527nDkXwmM1k5W"
    val selectedDate by viewModel.selectedDate.collectAsState()
    val availableSlots by viewModel.availableSlots.collectAsState()
    val selectedSlot by viewModel.selectedSlot.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(selectedDate) {
        viewModel.getDoctorSlots(selectedDoctor, selectedDate)
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
                            text = "No slots available.\nPlease select another date.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            val isButtonEnabled = selectedSlot != null && !isLoading

            Button(
                onClick = { if (isButtonEnabled) {
                    viewModel.bookAppointment(
                        patientId = user.id,
                        slot = selectedSlot!!,
                        date = selectedDate
                    )
                } },
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
        modifier = Modifier
            .clickable(onClick = onClick),
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

private fun formatTime(time24: String): String {
    return try {
        val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val date = inputFormat.parse(time24)
        outputFormat.format(date!!)
    } catch (e: Exception) {
        time24 // Return original string if formatting fails
    }
}
