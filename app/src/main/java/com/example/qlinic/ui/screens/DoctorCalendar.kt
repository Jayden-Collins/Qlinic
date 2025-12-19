package com.example.qlinic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.qlinic.R
import com.example.qlinic.data.model.SpecificDoctorInfo
import com.example.qlinic.ui.navigation.Routes
import com.example.qlinic.ui.navigation.TopBar
import com.example.qlinic.ui.viewModels.DoctorScheduleViewModel
import com.example.qlinic.ui.navigation.BottomNavBar

@Composable
fun DoctorCalendar(
    navController: NavController,
    viewModel: DoctorScheduleViewModel
) {
    val doctors by viewModel.doctors.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Column {
                TopBar(navController)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Doctor Schedule",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                DoctorList(
                    doctors = doctors,
                    searched = searchText,
                    onSearchTextChange = { searchText = it },
                    onDoctorClick = { doctor ->
                        navController.navigate(Routes.DoctorAppointmentSchedule.createRoute(doctor.id))
                    }
                )
            }
        }
    }
}

@Composable
fun DoctorList(
    doctors: List<SpecificDoctorInfo>,
    searched: String,
    onSearchTextChange: (String) -> Unit,
    onDoctorClick: (SpecificDoctorInfo) -> Unit
) {
    val query = searched.trim()
    val filtered = if (query.isBlank()) doctors else doctors.filter { d ->
        d.displayName.contains(query, ignoreCase = true) ||
                d.fullName.contains(query, ignoreCase = true) ||
                d.specialization.contains(query, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = searched,
                onValueChange = onSearchTextChange,
                label = { Text("Search Doctor") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(painter = painterResource(R.drawable.search), contentDescription = "Search")
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Selected Doctor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
        }

        items(filtered, key = { it.id }) { doctor ->
            DoctorCard(doctor = doctor, onViewClick = { onDoctorClick(doctor) })
        }
    }
}

@Composable
fun DoctorCard(
    doctor: SpecificDoctorInfo,
    onViewClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = doctor.imageUrl,
                    contentDescription = "Doctor image",
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // show the doctor's full name and allow wrapping to up to 2 lines
                Text(
                    text = "Dr. ${doctor.fullName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                Spacer(modifier = Modifier.height(6.dp))

                // allow specialization to wrap up to 2 lines and show ellipsis if still too long
                Text(
                    text = doctor.specialization,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = Color(0xFF607D8B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier
                    .height(120.dp)
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onViewClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EA2B4)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(text = "View", color = Color.White)
                }
            }
        }
    }
}
