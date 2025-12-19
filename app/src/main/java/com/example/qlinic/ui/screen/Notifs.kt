package com.example.qlinic.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qlinic.data.model.Notification
import com.example.qlinic.ui.component.SimplePageScaffold
import com.example.qlinic.ui.viewmodel.NotifsViewModel
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

@Composable
fun Notifs(
    onUpClick: () -> Unit = {},
    isDoctor: Boolean,
    viewModel: NotifsViewModel = viewModel()
){
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val patientId = "X2tLJZX7bTLPoZZSMnjY"

    // Automatically fetch notifications when the screen opens
    LaunchedEffect(isDoctor) {
        //val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val currentUserId = if (isDoctor) "S008" else patientId
        //if (currentUserId != null) {
            viewModel.fetchNotifications(currentUserId, isDoctor)
        //}
    }

    SimplePageScaffold(
        title = "Notifications",
        onUpClick = onUpClick
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No notifications yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Adding a key significantly improves scroll performance for large lists
                items(
                    items = notifications,
                    key = { it.notifId }
                ) { notification ->
                    NotificationCard(notification = notification)
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: Notification) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                notification.timestamp?.let {
                    Text(
                        text = timeAgo(it),
                        modifier = Modifier.align(Alignment.TopEnd),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Text(text = notification.body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

fun timeAgo(timestamp: Timestamp): String {
    val now = Timestamp.now().seconds
    val diff = now - timestamp.seconds

    val minutes = TimeUnit.SECONDS.toMinutes(diff)
    val hours = TimeUnit.SECONDS.toHours(diff)
    val days = TimeUnit.SECONDS.toDays(diff)

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
