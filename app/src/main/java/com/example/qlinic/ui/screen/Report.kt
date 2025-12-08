package com.example.qlinic.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qlinic.R
import com.example.qlinic.data.model.AppointmentStatistics
import com.example.qlinic.data.model.ReportFilterState
import com.example.qlinic.ui.component.CustomDatePicker
import com.example.qlinic.ui.component.DateInput
import com.example.qlinic.ui.component.FilterDropdown
import com.example.qlinic.ui.component.StatItem
import com.example.qlinic.ui.theme.QlinicTheme
import com.example.qlinic.ui.viewmodel.ReportViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ReportScreen(
    viewModel: ReportViewModel = viewModel(),
    onNavigateHome: () -> Unit
) {
    val filterState by viewModel.filterState.collectAsState()
    val stats by viewModel.stats.collectAsState()

    ReportContent(
        filterState = filterState,
        stats = stats,
        onFilterTypeChange = { type ->
            viewModel.updateFilter(filterState.copy(selectedType = type, isCustomRangeVisible = type == "Custom Date Range"))
        },
        onDepartmentChange = { dept ->
            viewModel.updateFilter(filterState.copy(selectedDepartment = dept))
        },
        onDateChange = { isStart, date ->
            viewModel.updateDate(isStart, date)
        },
        onNavigateHome = onNavigateHome
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportContent(
    filterState: ReportFilterState,
    stats: AppointmentStatistics,
    onFilterTypeChange: (String) -> Unit,
    onDepartmentChange: (String) -> Unit,
    onDateChange: (Boolean, String) -> Unit,
    onNavigateHome: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var isStartDatePicker by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.onPrimary,
        topBar = {
            TopAppBar(
                title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Reports", style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp))
                }},
                navigationIcon = {
                    Box(modifier = Modifier
                        .padding(start = 16.dp)
                        .size(50.dp)
                        .clip(CircleShape), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.ic_logosmall), contentDescription = null, modifier = Modifier.size(50.dp))
                    }
                },

                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notification),
                            contentDescription = "Notifications",
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(containerColor = MaterialTheme.colorScheme.onPrimary) {
                    NavigationBarItem(icon = { Icon(painter = painterResource(id = R.drawable.ic_home), contentDescription = "home", modifier = Modifier.size(24.dp)) }, selected = false, onClick = onNavigateHome)
                    NavigationBarItem(icon = { Icon(painter = painterResource(id = R.drawable.ic_schedule), contentDescription = "schedule", modifier = Modifier.size(24.dp)) }, selected = false, onClick = {})
                    NavigationBarItem(icon = { Icon(painter = painterResource(id = R.drawable.ic_report), contentDescription = "report", modifier = Modifier.size(24.dp)) }, selected = true, onClick = {})
                    NavigationBarItem(icon = { Icon(painter = painterResource(id = R.drawable.ic_profile), contentDescription = "profile", modifier = Modifier.size(24.dp)) }, selected = false, onClick = {})
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Appointments Summary", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(painter = painterResource(id = R.drawable.ic_info), contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Filters
                FilterDropdown("Types", listOf("Weekly", "Monthly", "Yearly", "Custom Date Range"), filterState.selectedType) {
                    onFilterTypeChange(it)
                }

                AnimatedVisibility(visible = filterState.isCustomRangeVisible) {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        DateInput("Start", filterState.startDate, Modifier.weight(1f)) { isStartDatePicker = true; showDatePicker = true }
                        Spacer(modifier = Modifier.width(12.dp))
                        DateInput("End", filterState.endDate, Modifier.weight(1f)) { isStartDatePicker = false; showDatePicker = true }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                FilterDropdown("Department", listOf("All Department", "Cardiology", "Dermatology", "Gastroenterology", "Gynecologist", "Neurology", "Orthopedics"), filterState.selectedDepartment) {
                    onDepartmentChange(it)
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Stats
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("Total", stats.total, pillColor = MaterialTheme.colorScheme.primary, onPillColor = MaterialTheme.colorScheme.onPrimary)
                    StatItem("Completed", stats.completed, stats.completedPercent, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    StatItem("Cancelled", stats.cancelled, stats.cancelledPercent, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }


    CustomDatePicker(
        show = showDatePicker,
        onDismiss = { showDatePicker = false },
        onDateSelected = { date ->
            val formattedDate = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
            onDateChange(isStartDatePicker, formattedDate)
        }
    )
}

@Preview(showBackground = true, name = "Report Screen Default")
@Composable
fun PreviewReportScreen() {
    QlinicTheme {
        ReportContent(
            filterState = ReportFilterState(), // Default state
            stats = AppointmentStatistics(50, 48, 2), // Dummy data
            onFilterTypeChange = {},
            onDepartmentChange = {},
            onDateChange = { _, _ -> },
            onNavigateHome = {}
        )
    }
}

@Preview(showBackground = true, name = "Report Screen Custom Range")
@Composable
fun PreviewReportScreenCustomRange() {
    QlinicTheme {
        ReportContent(
            filterState = ReportFilterState(
                selectedType = "Custom Date Range",
                isCustomRangeVisible = true
            ),
            stats = AppointmentStatistics(500, 488, 12),
            onFilterTypeChange = {},
            onDepartmentChange = {},
            onDateChange = { _, _ -> },
            onNavigateHome = {}
        )
    }
}