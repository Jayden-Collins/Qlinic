package com.example.qlinic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qlinic.data.model.Notification
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Data class to represent a single notification document
class NotifsViewModel : ViewModel() {
    private val db = Firebase.firestore
    private var snapshotListener: ListenerRegistration? = null

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun fetchNotifications(userId: String, isDoctor: Boolean) {
        val collectionName = if (isDoctor) "Doctor" else "Patient"

        // Clear any existing listener if this is called multiple times
        snapshotListener?.remove()

        _isLoading.value = true
        snapshotListener = db.collection(collectionName).document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    val notificationList = snapshot.documents.map { doc ->
                        val notification = doc.toObject(Notification::class.java)
                        notification?.copy(notifId = doc.id) ?: Notification()
                    }
                    _notifications.value = notificationList
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove()
    }
}
