package com.example.qlinic.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun MarkAsLeaveDialog(
    show: Boolean,
    dates: List<Date>,
    onConfirm: (List<Date>) -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    val df = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
    val dateListText = dates.joinToString(separator = "\n") { df.format(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onConfirm(dates) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Mark as Leave?", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column {
                Image(
                    painter = painterResource(id = com.example.qlinic.R.drawable.alert),
                    contentDescription = "Alert",
                    modifier = Modifier.size(96.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("This will mark the following date(s) as leave:")

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(dateListText)
                    }
                }
            }
        }
    )
}
