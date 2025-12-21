package com.example.qlinic.ui.screen

import com.example.qlinic.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.qlinic.ui.navigation.Routes
import com.example.qlinic.ui.viewmodel.LoginUserType
import com.example.qlinic.ui.viewmodel.LoginViewModel
import com.example.qlinic.ui.viewmodel.LoginViewModelFactory

enum class UserType {
    PATIENT,
    STAFF
}

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {

    // Auto-navigation based on saved session
    val context = LocalContext.current.applicationContext as android.app.Application
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(context)
    )

    val uiState = loginViewModel.uiState
    var rememberMe by remember { mutableStateOf(uiState.rememberMe) }

    val selectedUserType =
        if (uiState.userType == LoginUserType.PATIENT) UserType.PATIENT else UserType.STAFF


    // React to navigation flags and consume them
    LaunchedEffect(uiState.navigateToPatientHome, uiState.navigateToStaffHome, uiState.navigateToDoctorHome) {
        if (uiState.navigateToPatientHome || uiState.navigateToStaffHome || uiState.navigateToDoctorHome) {
            // Navigate to the unified HomeScreen
            navController.navigate(Routes.Home.route) {
                popUpTo(Routes.USER_SELECTION) { inclusive = true }
            }
            loginViewModel.resetNavigation()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Fullscreen background image
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Optional semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(250.dp) // Reduced size to prevent overlap
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Foreground sheet aligned to bottom
        Box(
            modifier = Modifier
                .height(550.dp)
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(topStart = 80.dp))
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Show loading indicator
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Image(
                        painter = painterResource(
                            id = if (selectedUserType == UserType.PATIENT)
                                R.drawable.patient_icon
                            else
                                R.drawable.staff_icon
                        ),
                        contentDescription = "User Type Icon",
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(100.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error message
                uiState.globalError?.let { error ->
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                // User type selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp)
                ) {
                    // Patient Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp))
                            .border(
                                width = 1.dp,
                                color = colorResource(R.color.teal_200),
                                shape = RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp)
                            )
                            .background(
                                if (selectedUserType == UserType.PATIENT)
                                    colorResource(R.color.teal_200)
                                else
                                    Color.White
                            )
                            .padding(vertical = 12.dp)
                            .clickable {
                                loginViewModel.onUserTypeChange(LoginUserType.PATIENT)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.patient),
                            color = if (selectedUserType == UserType.PATIENT)
                                Color.White
                            else
                                colorResource(R.color.teal_200),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Staff Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp))
                            .border(
                                width = 1.dp,
                                color = colorResource(R.color.teal_200),
                                shape = RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp)
                            )
                            .background(
                                if (selectedUserType == UserType.STAFF)
                                    colorResource(R.color.teal_200)
                                else
                                    Color.White
                            )
                            .padding(vertical = 12.dp)
                            .clickable {
                                loginViewModel.onUserTypeChange(LoginUserType.CLINIC_STAFF)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Clinic Staff",
                            color = if (selectedUserType == UserType.STAFF)
                                Color.White
                            else
                                colorResource(R.color.teal_200),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dynamic input fields based on user type
                when (selectedUserType) {
                    UserType.PATIENT -> {
                        PatientLoginFields(
                            email = uiState.identifier,
                            onEmailChange = { loginViewModel.onIdentifierChange(it) },
                            password = uiState.password,
                            onPasswordChange = { loginViewModel.onPasswordChange(it) },
                            passwordVisible = uiState.passwordVisible,
                            onPasswordVisibilityChange = { loginViewModel.onPasswordVisibilityChange(it) },
                            identifierError = uiState.identifierError,
                            passwordError = uiState.passwordError
                        )
                    }
                    UserType.STAFF -> {
                        StaffLoginFields(
                            staffId = uiState.identifier,
                            onStaffIdChange = { loginViewModel.onIdentifierChange(it) },
                            password = uiState.password,
                            onPasswordChange = { loginViewModel.onPasswordChange(it) },
                            passwordVisible = uiState.passwordVisible,
                            onPasswordVisibilityChange = { loginViewModel.onPasswordVisibilityChange(it) },
                            identifierError = uiState.identifierError,
                            passwordError = uiState.passwordError
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colorResource(R.color.teal_200)
                            )
                        )
                        Text(
                            text = "Remember Me",
                            color = colorResource(R.color.teal_200),
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                        )
                    }

                    Text(
                        text = "Forgot Password?",
                        color = colorResource(R.color.teal_200),
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { navController.navigate(Routes.FORGET_PASSWORD) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Login Button
                Button(
                    onClick = {
                        loginViewModel.login(
                            rememberMe = rememberMe,
                            onSuccessNavigate = { /* handled by LaunchedEffect */ },
                            onFailure = { /* UI handled */ }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.black)),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text(text = "Log In", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }


                Spacer(modifier = Modifier.height(8.dp))

                // Sign up link (only for patients)
                if (selectedUserType == UserType.PATIENT) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Don't have an account? ",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Sign Up",
                            color = colorResource(R.color.teal_200),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                navController.navigate(Routes.SIGNUP)
                            }
                        )
                    }
                }
            }
        }
    }
}