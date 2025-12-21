package com.example.qlinic

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.qlinic.service.logCurrentFcmToken
import com.example.qlinic.ui.component.NotificationPermissionRequester
import com.example.qlinic.ui.navigation.AppNavigation
import com.example.qlinic.ui.screen.BookAppt
import com.example.qlinic.ui.screen.Notifs
import com.example.qlinic.ui.theme.QlinicTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            QlinicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
                //BookAppt(isStaff = true, onUpClick = { })
                // Request notification permission on app start
                //logCurrentFcmToken()
                //NotificationPermissionRequester()
                //Notifs(isDoctor = true, onUpClick = {})
            }
        }
    }
}