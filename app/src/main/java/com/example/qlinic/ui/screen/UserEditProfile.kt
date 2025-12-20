package com.example.qlinic.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.qlinic.R
import com.example.qlinic.ui.viewmodel.EditProfileViewModel
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    userId: String,
    role: String = "patient", // "patient" | "staff" | "doctor"
    staffId: String? = null,
    viewModel: EditProfileViewModel = viewModel()
) {

    val context = LocalContext.current

    // correct ID to use for staff/doctor
    val idToUse = when (role.lowercase()) {
        "staff", "doctor" -> staffId ?: userId
        else -> userId
    }

    // Load according to role
    // Load user data safely
    LaunchedEffect(userId, role, staffId) {
        when (role.lowercase()) {
            "staff" -> viewModel.loadStaff(idToUse)
            "doctor" -> {
                viewModel.loadStaff(idToUse)
                viewModel.loadDoctor(idToUse)
            }
            else -> viewModel.loadPatient(idToUse)
        }
    }

    val patientState by viewModel.patient.collectAsState(initial = null)
    val staffState by viewModel.staff.collectAsState(initial = null)
    val doctorState by viewModel.doctor.collectAsState(initial = null)
    val loading by viewModel.loading.collectAsState(initial = false)
    val error by viewModel.error.collectAsState(initial = null)

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var ic by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) }

    var description by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var yearsOfExp by remember { mutableStateOf("") }

    var imageUrl by remember { mutableStateOf<String?>(null) }

    // populate local states when repository emits
    LaunchedEffect(patientState, staffState, doctorState) {
        when (role.lowercase()) {
            "patient" -> patientState?.let { p ->
                firstName = p.firstName ?: ""
                lastName = p.lastName ?: ""
                email = p.email ?: ""
                phone = viewModel.extractPhoneNumberForDisplay(p.phoneNumber)
                ic = p.ic ?: ""
                gender = p.gender ?: ""
                imageUrl = p.photoUrl
            }
            "staff", "doctor" -> {
                staffState?.let { s ->
                    firstName = s.firstName
                    lastName = s.lastName ?: ""
                    email = s.email ?: ""
                    phone = viewModel.extractPhoneNumberForDisplay(s.phoneNumber)
                    gender = s.gender ?: ""
                    isActive = s.isActive
                    imageUrl = s.imageUrl
                }
                if (role.lowercase() == "doctor") {
                    doctorState?.let { d ->
                        description = d.description ?: ""
                        specialization = d.specialization ?: ""
                        yearsOfExp = d.yearsOfExp?.toString() ?: ""
                    }
                }
            }
        }
    }

    // Image picker
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadAndSetProfilePhoto(role, idToUse, it) }
    }

    // Observe save success -> show toast then navigate back
    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            Toast.makeText(context, "Profile successfully updated", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = when (role.lowercase()) {
                        "doctor" -> "Edit Doctor Profile"
                        "staff" -> "Edit Staff Profile"
                        else -> "Edit Profile"
                    })
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .background(colorResource(id = R.color.white))
                .padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Profile image",
                        modifier = Modifier
                            .size(110.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(CircleShape)
                            .border(2.dp, Color.LightGray, CircleShape)
                            .clickable { launcher.launch("image/*") }
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile),
                        contentDescription = "Profile placeholder",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.LightGray, CircleShape)
                            .clickable { launcher.launch("image/*") }
                            .align(Alignment.CenterHorizontally)
                    )
                }

                Text(
                    text = "Change photo",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "First Name",
                            fontSize = 14.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FirstNameField(
                            firstName = firstName,
                            onValueChange = { firstName = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Last Name",
                            fontSize = 14.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LastNameField(
                            lastName = lastName,
                            onValueChange = { lastName = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (role.lowercase() == "patient") {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "NRIC",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    IcNumberField(
                        nric = ic,
                        onValueChange = { ic = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Email",
                    fontSize = 14.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                EmailField(
                    email = email,
                    onValueChange = { /* read-only */ },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Phone Number",
                            fontSize = 14.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        PhoneNumberField(
                            phoneNumber = phone,
                            onValueChange = { phone = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gender",
                            fontSize = 14.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        GenderField(
                            selectedGender = gender,
                            onGenderSelected = { /* read-only */ },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))


                if (role.lowercase() == "doctor") {

                    Text(
                        text = "Description",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    DescriptionField(
                        description = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Years of Experience",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    YearOfExpField(
                        yearsOfExp = yearsOfExp,
                        onValueChange = { yearsOfExp = it.filter { ch -> ch.isDigit() } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Description",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    DescriptionField(
                        description = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Specialization",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SpecializationField(
                        specialization = specialization,
                        onValueChange = { specialization = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                }

                Spacer(modifier = Modifier.height(16.dp))

                if (role.lowercase() == "staff" || role.lowercase() == "doctor") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Active", modifier = Modifier.weight(1f))
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }
                }

                if (!error.isNullOrBlank()) {
                    Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        when (role.lowercase()) {
                            "staff" -> {
                                viewModel.updateStaffPartial(
                                    idToUse,
                                    mapOf(
                                        "FirstName" to firstName,
                                        "LastName" to lastName,
                                        "Email" to email,
                                        "PhoneNumber" to phone,
                                        "isActive" to isActive
                                    )
                                )
                            }
                            "doctor" -> {
                                viewModel.updateStaffPartial(
                                    idToUse,
                                    mapOf(
                                        "FirstName" to firstName,
                                        "LastName" to lastName,
                                        "Email" to email,
                                        "PhoneNumber" to phone,
                                        "isActive" to isActive
                                    )
                                )
                                viewModel.updateDoctorPartial(
                                    idToUse,
                                    mapOf(
                                        "Description" to description,
                                        "Specialization" to specialization,
                                        "YearsOfExp" to (yearsOfExp.toIntOrNull() ?: 0)
                                    )
                                )
                            }
                            else -> {
                                viewModel.updatePatientPartial(
                                    idToUse,
                                    mapOf(
                                        "FirstName" to firstName,
                                        "LastName" to lastName,
                                        "IC" to ic,
                                        "PhoneNumber" to phone,
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.teal_700)
                    ),
                ) {
                    Text(
                        text = if (loading) "Saving..." else "Save Change",
                        color = colorResource(id = R.color.white))

                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (loading) {
                Surface(
                    color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
