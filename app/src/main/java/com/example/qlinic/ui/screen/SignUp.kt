package com.example.qlinic.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qlinic.ui.viewmodel.SignupViewModel

@Composable
fun PatientSignUpScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    vm: SignupViewModel = viewModel(),
    onLoginClick: () -> Unit
) {
    val state = vm.uiState
    val context = LocalContext.current

    fun validateFields(): Boolean {
        if (state.firstName.isBlank() || state.lastName.isBlank() || state.nric.isBlank() ||
            state.email.isBlank() || state.password.isBlank() || state.confirmPassword.isBlank() ||
            state.gender.isBlank() || state.phone.isBlank()
        ) {
            Toast.makeText(context, "All fields must be filled", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // Removed outer verticalScroll to avoid nested scrollables/infinite constraints
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(id = com.example.qlinic.R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 24.dp)
                .align(Alignment.CenterHorizontally),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()                       // <-- alternative: full available height
                .clip(RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp))
                .background(colorResource(id = com.example.qlinic.R.color.teal_200))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 45.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "First Name",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FirstNameField(
                            modifier = Modifier.fillMaxWidth(),
                            firstName = state.firstName,
                            onValueChange = { vm.onFirstNameChange(it) }
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Last Name",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LastNameField(
                            modifier = Modifier.fillMaxWidth(),
                            lastName = state.lastName,
                            onValueChange = { vm.onLastNameChange(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "NRIC",
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                IcNumberField(
                    modifier = Modifier.fillMaxWidth(),
                    nric = state.nric,
                    onValueChange = { vm.onNricChange(it) }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Email",
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                EmailField(
                    modifier = Modifier.fillMaxWidth(),
                    email = state.email,
                    onValueChange = { vm.onEmailChange(it) }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Phone Number",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        PhoneNumberField(
                            phoneNumber = state.phone,
                            onValueChange = { vm.onPhoneChange(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gender",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        GenderField(
                            selectedGender = state.gender,
                            onGenderSelected = { vm.onGenderChange(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Password",
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                PasswordField(
                    modifier = Modifier.fillMaxWidth(),
                    password = state.password,
                    onValueChange = { vm.onPasswordChange(it) }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Confirm Password",
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                PasswordField(
                    modifier = Modifier.fillMaxWidth(),
                    password = state.confirmPassword,
                    onValueChange = { vm.onConfirmPasswordChange(it) }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (validateFields()) {
                            vm.signup(
                                onSuccessNavigate = {
                                    Toast.makeText(context, "Signup successful", Toast.LENGTH_SHORT).show()
                                    onLoginClick()
                                },
                                onFailure = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = android.R.color.black)
                    )
                ) {
                    Text(
                        text = "Sign Up",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account?",
                        fontSize = 14.sp,
                        color = colorResource(id = android.R.color.white),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onLoginClick() }
                    )

                    Text(
                        text = "Sign In",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = android.R.color.holo_blue_dark),
                        modifier = Modifier
                            .clickable { onLoginClick() }
                    )
                }

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PatientSignUpScreenPreview() {
    MaterialTheme {
        PatientSignUpScreen(
            navController = rememberNavController(),
            onLoginClick = {}
        )
    }
}