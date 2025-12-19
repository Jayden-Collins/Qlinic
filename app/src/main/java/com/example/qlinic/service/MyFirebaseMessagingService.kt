package com.example.qlinic.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.qlinic.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            val title = it.title
            val body = it.body
            if (title != null && body != null) {
                Log.d(TAG, "Message Notification Body: $body")
                sendNotification(title, body)
            }
        }
    }

    private fun sendRegistrationToServer(token: String?) {
        if (token == null) {
            Log.w(TAG, "FCM token is null. Cannot send to server.")
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "User not logged in. Cannot send FCM token to server.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("Patient").document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener { Log.d(TAG, "FCM token successfully updated for user: $userId") }
            .addOnFailureListener { e -> Log.w(TAG, "Error updating FCM token", e) }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel, but only on API 26+ because
        // NotificationChannel is a new concept and not in the support library
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel for appointment reminders"
        }
        notificationManager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_arrowright) // Replace with your app's notification icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHANNEL_ID = "QLINIC_APPOINTMENT_REMINDERS"
        private const val CHANNEL_NAME = "Appointment Reminders"
    }
}

// Call this function to get and log the current token
fun logCurrentFcmToken() {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            Log.w("FCM_TOKEN_HELPER", "Fetching FCM registration token failed", task.exception)
            return@addOnCompleteListener
        }

        // Get the current FCM registration token
        val token = task.result

        // Log the token so you can copy it from Logcat
        Log.d("FCM_TOKEN_HELPER", "Current FCM Token is: $token")
    }
}
