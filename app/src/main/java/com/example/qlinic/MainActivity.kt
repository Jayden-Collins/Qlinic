package com.example.qlinic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qlinic.data.repository.DoctorSchedule
import com.example.qlinic.ui.component.CustomDatePicker
import com.example.qlinic.ui.navigation.AppNavigation
import com.example.qlinic.ui.theme.QlinicTheme
import com.example.qlinic.ui.viewModels.DoctorScheduleViewModel


class MainActivity : ComponentActivity() {
    val viewModel = DoctorScheduleViewModel(DoctorSchedule())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QlinicTheme {
                AppNavigation(viewModel)
                }
            }
        }
    }
