package com.example.qlinic.ui.screen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.qlinic.R
import com.example.qlinic.ui.navigation.Routes
import com.example.qlinic.ui.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.setValue
import com.example.qlinic.ui.navigation.Routes.Home.editProfileRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    role: String = "patient",
    staffId: String? = null, // pass Staff document id for ClinicStaff / Doctor lookups
    viewModel: ProfileViewModel = viewModel()
) {
    val authUid = FirebaseAuth.getInstance().currentUser?.uid
    val context = LocalContext.current

    val roleLower = role.lowercase()

    // Get viewmodel state
    val firstName by viewModel.firstName
    val lastName by viewModel.lastName
    val imageUrl by viewModel.imageUrl
    val description by viewModel.description
    val isDoctor by viewModel.isDoctor
    val isStaff by viewModel.isStaff
    val isLoading by viewModel.isLoading

    // define idToUse at composable scope so UI callbacks and LaunchedEffect can access it
    val idToUse = when (roleLower) {
        "staff", "doctor" -> staffId?.takeIf { it.isNotBlank() }
        else -> staffId?.takeIf { it.isNotBlank() } ?: authUid
    }

    // Handle logout with confirmation dialog
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        // Perform logout
                        viewModel.logout()
                        // Navigate to login screen
                        navController.navigate(Routes.USER_SELECTION) {
                            // Clear back stack so user can't go back
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Logout", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(roleLower, idToUse, authUid) {
        Log.d("PROFILE", "ProfileScreen role=$role idToUse=$idToUse authUid=$authUid")

        if (roleLower == "staff" || roleLower == "doctor") {
            if (idToUse.isNullOrBlank()) {
                Log.d("PROFILE", "ProfileScreen: missing staffId for role=$role - aborting load")
                navController.popBackStack()
                return@LaunchedEffect
            }
            viewModel.loadStaffProfile(idToUse, asDoctor = (roleLower == "doctor"))
        } else {
            viewModel.loadPatient(idToUse)
        }
    }


    val displayName = listOfNotNull(
        firstName?.takeIf { it.isNotBlank() },
        lastName?.takeIf { it.isNotBlank() }
    ).joinToString(" ").ifEmpty { "No name" }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally).padding(bottom = 24.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(100.dp).clip(CircleShape)
                                .border(
                                    width = 3.dp,
                                    color = Color(0xFFBBBBBB),
                                    shape = CircleShape
                                )
                                .align(Alignment.Center)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_profile),
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(100.dp).clip(CircleShape)
                                .border(
                                    width = 3.dp,
                                    color = Color(0xFFBBBBBB),
                                    shape = CircleShape
                                )
                                .align(Alignment.Center)
                        )
                    }
                }

                Text(
                    text = displayName,
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .padding(top = 12.dp, bottom = 8.dp)
                )

                if (isDoctor && !description.isNullOrBlank()) {
                    Text(
                        text = description ?: "", color = Color.DarkGray, fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Edit Profile Menu Item
                ProfileMenuItem(
                    icon = R.drawable.user_edit,
                    text = "Edit Profile",
                    onClick = {
                        val editProfileRoute = when {
                            isDoctor -> editProfileRoute(
                                userId = idToUse ?: authUid ?: "",
                                role = "doctor",
                                staffId = idToUse
                            )
                            isStaff -> editProfileRoute(
                                userId = idToUse ?: authUid ?: "",
                                role = "staff",
                                staffId = idToUse
                            )
                            else -> editProfileRoute(
                                userId = idToUse ?: authUid ?: "",
                                role = "patient"
                            )
                        }
                        navController.navigate(editProfileRoute)
                    }
                )

                Divider(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray
                )

                if (!isStaff && !isDoctor) {
                    ProfileMenuItem(
                        icon = R.drawable.notification,
                        text = "Notifications",
                        onClick = { /* Handle click */ })
                    Divider(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        thickness = 0.5.dp,
                        color = Color.LightGray
                    )
                }

                Divider(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray
                )

                ProfileMenuItem(
                    icon = R.drawable.lock,
                    text = "Change Password",
                    onClick = { navController.navigate(Routes.FORGET_PASSWORD) })

                Divider(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray
                )

                ProfileMenuItem(icon = R.drawable.logout, text = "Log Out", onClick = {
                    viewModel.logout()
                    navController.navigate(Routes.USER_SELECTION) {
                        popUpTo(Routes.USER_SELECTION) { inclusive = true }
                    }
                }, textColor = Color.Red)
            }
        }
    }
}