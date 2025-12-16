package com.example.qlinic.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.qlinic.R
import com.example.qlinic.data.model.Appointment
import com.example.qlinic.data.model.AppointmentStatus
import com.example.qlinic.data.model.TestUsers
import com.example.qlinic.data.model.UserRole
import com.example.qlinic.data.repository.MockAppointmentRepository
import com.example.qlinic.ui.theme.QlinicTheme
import com.example.qlinic.ui.ui_state.HomeUiState
import com.example.qlinic.ui.viewmodel.HomeViewModel
import com.example.qlinic.ui.viewmodel.HomeViewModelFactory
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    homeViewModel: HomeViewModel,
    onNavigateToSchedule: () -> Unit
) {
    val homeUiState by homeViewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        HomeScreenContent(
            state = homeUiState,
            homeViewModel = homeViewModel,
            onTabSelected = { newStatus -> homeViewModel.onTabSelected(newStatus) },
            onAction = { appointmentId, action -> homeViewModel.onAppointmentAction(appointmentId, action) },
            onNavigateToSchedule = onNavigateToSchedule
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenContent(
    state: HomeUiState,
    homeViewModel: HomeViewModel,
    onTabSelected: (AppointmentStatus) -> Unit,
    onAction: (String, String) -> Unit,
    onNavigateToSchedule: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        if (state.showTabs) {
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.onSurface,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab.ordinal]),
                        color = MaterialTheme.colorScheme.onSurface,
                        height = 3.dp)
                }){
                listOf(
                    AppointmentStatus.UPCOMING,
                    AppointmentStatus.COMPLETED,
                    AppointmentStatus.CANCELLED
                ).forEach { status ->
                    Tab(
                        selected = state.selectedTab == status,
                        onClick = { onTabSelected(status) },
                        text = { Text(status.displayName, style = MaterialTheme.typography.displayMedium) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.selectedTab == AppointmentStatus.UPCOMING) {
            MonthSelector(
                currentYearMonth = state.currentYearMonth,
                onPrevious = { homeViewModel.onPreviousMonth() },
                onNext = { homeViewModel.onNextMonth() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Loading indicator
        Spacer(modifier = Modifier.height(16.dp))

        // 3. Grouped List Content
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (state.selectedTab == AppointmentStatus.UPCOMING) {

                val groupedData = homeViewModel.getGroupedAppointments()

                if (groupedData.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(painterResource(id = R.drawable.ic_schedule), null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No appointments in ${state.currentYearMonth.month}", color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            // Only show book button if user is Patient
                            if (state.currentUser?.role == UserRole.PATIENT) {
                                Button(onClick = onNavigateToSchedule) { Text("Book Now") }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        groupedData.forEach { (date, appointments) ->
                            stickyHeader {
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.getDefault())),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(bottom = 8.dp)
                                )
                            }
                            items(appointments) { appointment ->
                                TimelineAppointmentRow(
                                    appointment = appointment,
                                    timeString = homeViewModel.parseTime(appointment.dateTime),
                                    currentUserRole = state.currentUser?.role ?: UserRole.PATIENT,
                                    onActionClick = onAction
                                )
                            }
                        }
                    }
                }

            } else {

                if (state.appointments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (state.selectedTab == AppointmentStatus.COMPLETED) "No completed appointments" else "No cancelled appointments",
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(items = state.appointments, key = { it.id }) { appointment ->
                            AppointmentCard(
                                appointment = appointment,
                                currentUserRole = state.currentUser?.role ?: UserRole.PATIENT,
                                onActionClick = onAction
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    currentUserRole: UserRole,
    onActionClick: (String, String) -> Unit
) {
    val (name, subtitle, imageUrl) = remember(appointment, currentUserRole) {
        if (currentUserRole == UserRole.PATIENT) {
            Triple(
                appointment.doctor.name,
                appointment.doctor.details ?: "",
                appointment.doctor.imageUrl // Get Doctor Image
            )
        } else {
            Triple(
                appointment.patient.name,
                appointment.patient.details ?: "",
                appointment.patient.imageUrl // Get Patient Image
            )
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.onPrimary)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_room),
                            contentDescription = "Location",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = appointment.locationOrRoom,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (currentUserRole == UserRole.PATIENT && appointment.status == AppointmentStatus.UPCOMING) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onActionClick(appointment.id, "Cancel") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }

                    Button(
                        onClick = { onActionClick(appointment.id, "Reschedule") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.outline,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Reschedule") }
                }
            }
        }
    }
}

@Composable
fun MonthSelector(
    currentYearMonth: java.time.YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {

    val canGoBack = currentYearMonth.isAfter(java.time.YearMonth.now())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            androidx.compose.material3.IconButton(
                onClick = onPrevious,
                enabled = canGoBack
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrowleft),
                    contentDescription = "Prev",
                    tint = if (canGoBack) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                )
            }

            Text(
                text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM, yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            androidx.compose.material3.IconButton(onClick = onNext) {
                Icon(painter = painterResource(id = R.drawable.ic_arrowright), contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TimelineAppointmentRow(
    appointment: Appointment,
    timeString: String,
    currentUserRole: UserRole,
    onActionClick: (String, String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),

        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = timeString,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(50.dp)
                .padding(top = 16.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            AppointmentCard(
                appointment = appointment,
                currentUserRole = currentUserRole,
                onActionClick = onActionClick
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenContentPreview() {
    val dummyRepository = MockAppointmentRepository()
    val currentUser = TestUsers.current

    val homeViewModel = HomeViewModelFactory(dummyRepository, currentUser)
        .create(HomeViewModel::class.java)

    QlinicTheme {
        HomeScreen(
            paddingValues = PaddingValues(0.dp),
            homeViewModel = homeViewModel,
            onNavigateToSchedule = {}
        )
    }
}
