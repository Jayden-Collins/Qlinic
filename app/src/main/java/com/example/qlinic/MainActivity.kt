package com.example.qlinic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.qlinic.ui.navigation.AppNavigation
import com.example.qlinic.ui.theme.QlinicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Use the theme provided in previous steps
            QlinicTheme {
                AppNavigation()
            }
        }
    }
}