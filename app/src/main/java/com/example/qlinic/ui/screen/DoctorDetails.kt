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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.qlinic.R
import com.example.qlinic.data.model.DoctorProfile
import com.example.qlinic.ui.theme.QlinicTheme
import com.example.qlinic.ui.viewmodel.ScheduleViewModel


@Composable
fun DoctorDetailsScreen(
    viewModel: ScheduleViewModel,
    onBackClick: () -> Unit
) {
    val doctor by viewModel.selectedDoctor.collectAsState()

    DoctorDetailsLayout(
        doctor = doctor,
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDetailsLayout(
    doctor: DoctorProfile?,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Doctor Details",
                    style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 64.dp),
                    textAlign = TextAlign.Left
                ) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrowleft),
                            contentDescription = "Back",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.onPrimary)
            )
        },
        bottomBar = {
            Button(
                onClick = { /* TODO: Navigate to Booking */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(64.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Book Appointment", fontSize = 16.sp, color = Color.White)
            }
        }
    ) { padding ->
        if (doctor != null) {
            DoctorDetailsContent(doctor = doctor, padding = padding)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Doctor not found",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun DoctorDetailsContent(doctor: DoctorProfile, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(doctor.imageUrl)
                .crossfade(true)
                .build(),
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
            Text(
                text = doctor.name,
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = doctor.specialty,
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
                        text = doctor.room,
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
        /**Spacer(modifier = Modifier.height(24.dp))

        // Working Time
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(text = "Working Time", style = MaterialTheme.typography.displayLarge, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Monday-Friday, 9am - 2pm", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(text = "Monday-Friday, 3pm - 10pm", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }**/
    }
}

@Preview(showBackground = true)
@Composable
fun DoctorDetailsScreenPreview() {
    val dummyDoctor = DoctorProfile(
        id = "S008",
        name = "Dr. David Patel",
        specialty = "Cardiology",
        room = "R004",
        imageUrl = "",
        description = "Dr. David Patel, a dedicated cardiologist, brings a wealth of experience to Golden Gate Cardiology Center. Head of Cardiology. Graduated from UM with honours.",
        yearsOfExp = 18
    )

    QlinicTheme {
        // Call the LAYOUT, not the Screen
        DoctorDetailsLayout(
            doctor = dummyDoctor,
            onBackClick = {}
        )
    }
}