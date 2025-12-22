package com.example.qlinic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.qlinic.ui.component.SimpleTopBar
import com.example.qlinic.ui.navigation.Routes
import com.example.qlinic.ui.viewmodel.DoctorScheduleViewModel

@Composable
fun DoctorCalendar(
    navController: NavController,
    viewModel: DoctorScheduleViewModel,
    modifier: Modifier = Modifier,
) {
    val doctors by viewModel.doctors.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "Doctor Schedule",
                onUpClick = { navController.navigateUp() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
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
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Added padding above search bar as requested
            Spacer(modifier = Modifier.height(8.dp))
            
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

            Spacer(modifier = Modifier.height(16.dp))
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
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = doctor.imageUrl,
                    contentDescription = "Doctor image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dr. ${doctor.fullName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = doctor.specialization,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = onViewClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EA2B4)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = "View", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}
