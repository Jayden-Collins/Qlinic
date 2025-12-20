// Kotlin
package com.example.qlinic.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.qlinic.ui.navigation.Routes

// PatientHomeScreen.kt
@Composable
fun PatientHomeScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Patient Home Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Welcome Patient!", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            navController.navigate(Routes.profileRoute("patient", "")) {
                popUpTo(Routes.PATIENT_HOME) { inclusive = true }
            }
        }) {
            Text("Profile")
        }
        Button(onClick = {
            navController.navigate(Routes.USER_SELECTION) {
                popUpTo(Routes.PATIENT_HOME) { inclusive = true }
            }
        }) {
            Text("Logout")
        }
    }
}

// StaffHomeScreen.kt
@Composable
fun StaffHomeScreen(navController: NavController, staffId: String) {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Staff Home Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Welcome Staff Member!", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            if (staffId.isNotBlank()) {
                navController.navigate(Routes.profileRoute("staff", userId = "", staffId = staffId)) {
                    popUpTo(Routes.STAFF_HOME) { inclusive = true }
                }
            } else {
                Log.d("StaffHomeScreen", "Profile button pressed but staffId is blank")
                Toast.makeText(ctx, "Staff ID not available", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Profile")
        }
        Button(onClick = {
            navController.navigate(Routes.USER_SELECTION) {
                popUpTo(Routes.STAFF_HOME) { inclusive = true }
            }
        }) {
            Text("Logout")
        }
    }
}

// DoctorHomeScreen.kt
@Composable
fun DoctorHomeScreen(navController: NavController, staffId: String) {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Doctor Home Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Welcome Doctor!", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            if (staffId.isNotBlank()) {
                navController.navigate(Routes.profileRoute("doctor", userId = "", staffId = staffId)) {
                    popUpTo(Routes.DOCTOR_HOME) { inclusive = true }
                }
            } else {
                Log.d("DoctorHomeScreen", "Profile button pressed but staffId is blank")
                Toast.makeText(ctx, "Doctor ID not available", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Profile")
        }
        Button(onClick = {
            navController.navigate(Routes.USER_SELECTION) {
                popUpTo(Routes.DOCTOR_HOME) { inclusive = true }
            }
        }) {
            Text("Logout")
        }
    }
}
