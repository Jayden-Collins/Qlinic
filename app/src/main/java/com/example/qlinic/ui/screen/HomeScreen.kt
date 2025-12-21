package com.example.qlinic.ui.screen

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.qlinic.R
import com.example.qlinic.data.model.AppointmentStatus
import com.example.qlinic.data.model.UserRole
import com.example.qlinic.ui.theme.*
import com.example.qlinic.ui.ui_state.AppointmentCardUiState
import com.example.qlinic.ui.ui_state.HomeUiState
import com.example.qlinic.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    homeViewModel: HomeViewModel,
    onNavigateToSchedule: () -> Unit
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        HomeScreenContent(
            state = homeUiState,homeViewModel = homeViewModel,
            onTabSelected = { newStatus -> homeViewModel.onTabSelected(newStatus) },
            onAction = { appointmentId, action ->
                homeViewModel.onAppointmentAction(appointmentId, action)
                if (action == "Complete" || action == "NoShow") {
                    scope.launch {
                        val message = if (action == "Complete") "Appointment Completed" else "Marked as No Show"
                        val result = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short // Shows for ~4 seconds
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            homeViewModel.onAppointmentAction(appointmentId, "Undo")
                        }
                    }
                }
            },
            onNavigateToSchedule = onNavigateToSchedule
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (state.selectedTab == AppointmentStatus.UPCOMING) {

                val groupedData = homeViewModel.getGroupedUiItems()

                if (groupedData.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(painterResource(id = R.drawable.ic_schedule), null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No appointments in ${state.currentYearMonth.month}", color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            if (state.userRole == UserRole.PATIENT) {
                                Button(onClick = onNavigateToSchedule) { Text("Book Now") }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        groupedData.forEach { (date, uiItems) ->
                            stickyHeader {
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.getDefault())),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.onPrimary)
                                        .padding(bottom = 8.dp)
                                )
                            }
                            items(uiItems) { uiItem ->
                                TimelineAppointmentRow(
                                    uiItem = uiItem,
                                    currentUserRole = state.userRole,
                                    onActionClick = onAction
                                )
                            }
                        }
                    }
                }
            } else {

                if (state.appointmentItems.isEmpty()) {
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
                        items(items = state.appointmentItems, key = { it.id }) { uiItem ->
                            AppointmentCard(
                                uiItem = uiItem,
                                currentUserRole = state.userRole,
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
    uiItem: AppointmentCardUiState,
    currentUserRole: UserRole,
    onActionClick: (String, String) -> Unit
) {
    val (statusColor, statusText) = when (uiItem.displayStatus) {
        AppointmentStatus.UPCOMING -> Pair(MaterialTheme.colorScheme.primary, "Upcoming")
        AppointmentStatus.ONGOING -> Pair(orange, "On Going") // Use Orange/Amber for active
        AppointmentStatus.COMPLETED -> Pair(teal, "Completed")
        AppointmentStatus.CANCELLED -> Pair(red, "Cancelled")
        AppointmentStatus.NO_SHOW -> Pair(red, "No Show")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End, //
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = statusText.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiItem.displayImageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uiItem.displayImageUrl)
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
                            text = uiItem.displayName.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = uiItem.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = uiItem.displaySubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_room),
                            contentDescription = "Room",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Log.d("ROOM", uiItem.rawAppointment.roomId ?: "TBD")
                        Text(
                            text = uiItem.rawAppointment.roomId ?: "TBD",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            // PATIENT & STAFF BUTTONS (Cancel / Reschedule)
            if ((currentUserRole == UserRole.PATIENT || currentUserRole == UserRole.STAFF) && uiItem.rawAppointment.status == AppointmentStatus.UPCOMING) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onActionClick(uiItem.id, "Cancel") },
                        enabled = uiItem.isActionEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text(
                        text ="Cancel", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp) // Slightly smaller
                    ) }

                    Button(
                        onClick = { onActionClick(uiItem.id, "Reschedule") },
                        enabled = uiItem.isActionEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSecondary,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text(text ="Reschedule", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)) }
                }
            }
            // DOCTOR BUTTONS (Complete / No Show)
            if (currentUserRole == UserRole.DOCTOR && uiItem.rawAppointment.status == AppointmentStatus.UPCOMING) {

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onActionClick(uiItem.id, "Complete") },
                        enabled = !uiItem.isActionEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Complete", color = MaterialTheme.colorScheme.onPrimary)
                    }

                    Button(
                        onClick = { onActionClick(uiItem.id, "NoShow") },
                        enabled = !uiItem.isActionEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("No Show", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            // DOCTOR HISTORY (Undo)
            if (currentUserRole == UserRole.DOCTOR &&
                (uiItem.rawAppointment.status == AppointmentStatus.COMPLETED || uiItem.rawAppointment.status == AppointmentStatus.CANCELLED)) {

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onActionClick(uiItem.id, "Complete") },
                    enabled = uiItem.isActionEnabled, // <--- DISABLED if in future
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        // Optional: Define disabled color explicitly if needed
                        disabledContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Complete", color = MaterialTheme.colorScheme.onPrimary)
                }

                Button(
                    onClick = { onActionClick(uiItem.id, "NoShow") },
                    enabled = uiItem.isActionEnabled, // <--- DISABLED if in future
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        disabledContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("No Show", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            // Doctor: Completed & Cancelled (UNDO)
            if (currentUserRole == UserRole.DOCTOR &&
                (uiItem.rawAppointment.status == AppointmentStatus.COMPLETED || uiItem.rawAppointment.status == AppointmentStatus.CANCELLED)) {

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onActionClick(uiItem.id, "Undo") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.outline // Grey for undo
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Undo", color = MaterialTheme.colorScheme.onBackground)
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
            IconButton(
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

            IconButton(onClick = onNext) {
                Icon(painter = painterResource(id = R.drawable.ic_arrowright), contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TimelineAppointmentRow(
    uiItem: AppointmentCardUiState,
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
            text = uiItem.timeString,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(50.dp)
                .padding(top = 16.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            AppointmentCard(
                uiItem = uiItem,
                currentUserRole = currentUserRole,
                onActionClick = onActionClick
            )
        }
    }
}