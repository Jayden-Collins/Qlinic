package com.example.qlinic.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.qlinic.R
import com.example.qlinic.data.model.AppointmentInfo
import com.example.qlinic.ui.theme.QlinicTheme

@Composable
fun DeleteConfirmationDialog(
    appointment: AppointmentInfo,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close Icon Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Close",
                        modifier = Modifier
                            .size(12.dp)
                            .clickable { onDismiss() }
                    )
                }

                // Title
                Text(
                    text = stringResource(R.string.delete_appointment),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Image(
                    painter = painterResource(id = R.drawable.ic_alert),
                    contentDescription = "Warning",
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Warning Text
                Text(
                    text = stringResource(R.string.warning_text),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Details Card
                AppointmentDetailsCard(appointment)

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cancel Button (Dark Blue/Black in design)
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface, // Using dark color
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(text = stringResource(R.string.cancel_button), style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Confirm Button (Light Grey in design)
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant, // Light grey/blue
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(text = stringResource(R.string.confirm_button), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentDetailsCard(appointment: AppointmentInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        DetailRow(label = "Doctor", value = appointment.doctorName)
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow(label = "Patient", value = appointment.patientName)
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow(label = "Date", value = appointment.dateTime)
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, // Gray
            modifier = Modifier.width(70.dp),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall, // Regular
            color = MaterialTheme.colorScheme.outline // Slightly lighter grey for value based on image
        )
    }
}

@Composable
fun AppointmentDeletedSuccessDialog(
    doctorName: String,
    onDone: () -> Unit
) {
    Dialog(onDismissRequest = onDone) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_success),
                    contentDescription = "Success",
                    modifier = Modifier.size(160.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.delete_appointment_success),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Schedule for $doctorName\nhas been successfully updated.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.done_button),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp)
                    )
                }
            }
        }
    }
}

// --- 3. Preview & Usage Example ---
@Preview(showBackground = true)
@Composable
fun PreviewDialogs() {
    QlinicTheme {
        // Just for preview visualization (stacking them so you can see both)
        Column(Modifier.padding(16.dp)) {
            // Uncomment one to preview

            DeleteConfirmationDialog(
                appointment = AppointmentInfo("Dr. David Patel", "Joanne, Lee", "5 Nov 2025, 10:00 AM"),
                onDismiss = {},
                onConfirm = {}
            )

            // AppointmentDeletedSuccessDialog(doctorName = "Dr. David Patel", onDone = {})
        }
    }
}

@Preview(showBackground = true, name = "Success Dialog Preview")
@Composable
fun PreviewSuccessDialog() {
    QlinicTheme {
        // We mock the action to do nothing for the preview
        AppointmentDeletedSuccessDialog(
            doctorName = "Dr. David Patel",
            onDone = {}
        )
    }
}