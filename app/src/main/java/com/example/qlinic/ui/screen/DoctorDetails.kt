package com.example.qlinic.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.qlinic.R
import com.example.qlinic.data.model.ClinicStaff
import com.example.qlinic.data.model.Doctor
import com.example.qlinic.ui.viewmodel.ScheduleViewModel


@Composable
fun DoctorDetailsScreen(
    viewModel: ScheduleViewModel,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit
) {
    val doctor by viewModel.selectedDoctor.collectAsState()
    val staff by viewModel.selectedStaff.collectAsState()
    val isLoading by viewModel.isDetailLoading.collectAsState()

    DoctorDetailsLayout(
        doctor = doctor,
        staff = staff,
        isLoading = isLoading,
        onBackClick = onBackClick,
        onBookClick = onBookClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDetailsLayout(
    doctor: Doctor?,
    staff: ClinicStaff?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.onPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Doctor Details",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrowleft),
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (!isLoading && doctor != null) {
                Button(
                    onClick = { onBookClick(doctor.doctorID) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Book Appointment", fontSize = 16.sp, color = Color.White)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = com.example.qlinic.ui.theme.teal
                )
            } else if (doctor != null && staff != null) {
                DoctorDetailsContent(doctor = doctor, staff = staff, padding = PaddingValues(0.dp))
            } else {
                Text(
                    text = "Doctor not found",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun DoctorDetailsContent(doctor: Doctor, staff: ClinicStaff, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Image with Loading Indicator
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(staff.imageUrl)
                .crossfade(true)
                .build(),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp,
                        color = com.example.qlinic.ui.theme.teal
                    )
                }
            },
            error = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_profile),
                    contentDescription = "Error loading image",
                    modifier = Modifier.size(100.dp),
                    tint = Color.Gray
                )
            },
            contentDescription = "Doctor",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(230.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            val doctorName = "Dr. ${staff.firstName} ${staff.lastName}"

            Text(
                text = doctorName,
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = doctor.specialization,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    painter = painterResource(id = R.drawable.ic_room),
                    contentDescription = "Room",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Room Chip
                Surface(
                    color = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = doctor.roomID,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // About Me Section
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(text = "About me", style = MaterialTheme.typography.displayLarge, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = doctor.description,
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Justify
            )
        }
    }
}
