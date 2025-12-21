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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qlinic.data.repository.DoctorSchedule
import com.example.qlinic.ui.component.CustomDatePicker
import com.example.qlinic.ui.navigation.AppNavigation
import com.example.qlinic.ui.theme.QlinicTheme
import com.example.qlinic.ui.viewModels.DoctorScheduleViewModel


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

                // Request notification permission on app start for Android 13+
                NotificationPermissionRequester()

                // Log FCM token for debugging if needed
                logCurrentFcmToken()
            }
        }
    }
}
