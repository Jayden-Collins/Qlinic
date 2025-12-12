package com.example.qlinic.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qlinic.R
import com.example.qlinic.data.model.AppointmentStatistics
import com.example.qlinic.data.model.ChartData
import com.example.qlinic.data.model.PeakHoursReportData
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
    paddingValues: PaddingValues,
    viewModel: ReportViewModel = viewModel()
) {
    val filterState by viewModel.filterState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val peakHoursReportData by viewModel.peakHoursReportData.collectAsState()

    ReportContent(
        paddingValues = paddingValues,
        filterState = filterState,
        stats = stats,
        peakHoursReportData = peakHoursReportData,
        onFilterTypeChange = { type ->
            viewModel.updateFilter(
                filterState.copy(
                    selectedType = type,
                    isCustomRangeVisible = type == "Custom Date Range"
                )
            )
        },
        onDepartmentChange = { dept ->
            viewModel.updateFilter(filterState.copy(selectedDepartment = dept))
        },
        onDateChange = { isStart, date ->
            viewModel.updateDate(isStart, date)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportContent(
    paddingValues: PaddingValues,
    filterState: ReportFilterState,
    stats: AppointmentStatistics,
    peakHoursReportData: PeakHoursReportData,
    onFilterTypeChange: (String) -> Unit,
    onDepartmentChange: (String) -> Unit,
    onDateChange: (Boolean, String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var isStartDatePicker by remember { mutableStateOf(true) }

    Column(modifier = Modifier
        .padding(paddingValues)
        .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Appointments Summary", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Filters
        FilterDropdown(
            "Types",
            listOf("Weekly", "Monthly", "Yearly", "Custom Date Range"),
            filterState.selectedType
        ) {
            onFilterTypeChange(it)
        }

        AnimatedVisibility(visible = filterState.isCustomRangeVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DateInput(
                    "Start",
                    filterState.startDate,
                    Modifier.weight(1f)
                ) { isStartDatePicker = true; showDatePicker = true }
                Spacer(modifier = Modifier.width(12.dp))
                DateInput(
                    "End",
                    filterState.endDate,
                    Modifier.weight(1f)
                ) { isStartDatePicker = false; showDatePicker = true }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        FilterDropdown(
            "Department",
            listOf(
                "All Department",
                "Cardiology",
                "Dermatology",
                "Gastroenterology",
                "Gynecologist",
                "Neurology",
                "Orthopedics"
            ),
            filterState.selectedDepartment
        ) {
            onDepartmentChange(it)
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                "Total",
                stats.total,
                pillColor = MaterialTheme.colorScheme.primary,
                onPillColor = MaterialTheme.colorScheme.onPrimary
            )
            StatItem(
                "Completed",
                stats.completed,
                stats.completedPercent,
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer
            )
            StatItem(
                "Cancelled",
                stats.cancelled,
                stats.cancelledPercent,
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        PeakHoursReport(reportData = peakHoursReportData)
    }

    CustomDatePicker(
        show = showDatePicker,
        onDismiss = { showDatePicker = false },
        onDateSelected = {
            date ->
            val formattedDate = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
            onDateChange(isStartDatePicker, formattedDate)
        }
    )
}

@Composable
fun PeakHoursReport(
    reportData: PeakHoursReportData,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Peak Hours Report", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            if (reportData.chartData.isNotEmpty()) {
                Column {
                    // Display Busiest Day/Time text inside the container
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("Busiest Day", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = reportData.busiestDay,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Busiest Time", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = reportData.busiestTime,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Display the new Bar Chart
                    BarChartWithAxis(data = reportData.chartData)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp), contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No peak hours data available.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun BarChartWithAxis(
    data: List<ChartData>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.value } ?: 0f
    // Define Y-axis labels. Let's create 3 labels: 0, max/2, and max.
    val yAxisLabels = listOf(
        "0",
        (maxValue / 2).toInt().toString(),
        maxValue.toInt().toString()
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp) // Height for the chart area
    ) {
        // Y-Axis (Vertical Labels)
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            yAxisLabels.reversed().forEach { label ->
                Text(text = label, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Main Chart Area (Bars and X-Axis)
        Column(modifier = Modifier.weight(1f)) {
            // Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Bars take up most of the space
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { chartData ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp) // Space between bars
                            .fillMaxHeight(if (maxValue > 0) chartData.value / maxValue else 0f)
                            .background(chartData.color)
                    )
                }
            }
            // Divider for the X-Axis line
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            // X-Axis (Horizontal Labels)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { chartData ->
                    Text(
                        text = chartData.label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun BarChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.value } ?: 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp), // Give the chart a fixed height
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom // Align bars to the bottom
    ) {
        data.forEach { chartData ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                // The Bar itself
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f) // Bar width
                        .fillMaxHeight(if (maxValue > 0) chartData.value / maxValue else 0f) // Bar height relative to max value
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(chartData.color)
                )
                Spacer(modifier = Modifier.height(4.dp))
                // The Label below the bar
                Text(
                    text = chartData.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}


@Preview(showBackground = true, name = "Report Screen Default")
@Composable
fun PreviewReportScreen() {
    QlinicTheme {
        ReportContent(
            paddingValues = PaddingValues(),
            filterState = ReportFilterState(), // Default state
            stats = AppointmentStatistics(50, 48, 2), // Dummy data
            peakHoursReportData = PeakHoursReportData(
                chartData = listOf(
                    ChartData(12f, "Mon", Color(0xFFB0C4DE)),
                    ChartData(18f, "Tue", Color(0xFFB0C4DE)),
                    ChartData(25f, "Wed", Color(0xFF4682B4)),
                    ChartData(15f, "Thu", Color(0xFFB0C4DE)),
                    ChartData(22f, "Fri", Color(0xFFB0C4DE)),
                    ChartData(8f, "Sat", Color(0xFFB0C4DE))
                ),
                busiestDay = "Wednesday",
                busiestTime = "10 AM - 11 AM"
            ),
            onFilterTypeChange = {},
            onDepartmentChange = {},
            onDateChange = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Report Screen Custom Range")
@Composable
fun PreviewReportScreenCustomRange() {
    QlinicTheme {
        ReportContent(
            paddingValues = PaddingValues(),
            filterState = ReportFilterState(
                selectedType = "Custom Date Range",
                isCustomRangeVisible = true
            ),
            stats = AppointmentStatistics(500, 488, 12),
            peakHoursReportData = PeakHoursReportData(
                chartData = listOf(
                    ChartData(5f, "Mon", Color(0xFFB0C4DE)),
                    ChartData(30f, "Tue", Color(0xFF4682B4)),
                    ChartData(15f, "Wed", Color(0xFFB0C4DE))
                ),
                busiestDay = "Tuesday",
                busiestTime = "3 PM - 4 PM"
            ),
            onFilterTypeChange = {},
            onDepartmentChange = {},
            onDateChange = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewReportContent() {
    QlinicTheme {
        ReportContent(
            paddingValues = PaddingValues(),
            filterState = ReportFilterState(),
            stats = AppointmentStatistics(50, 48, 2),
            // Provide sample chart data for the preview
            peakHoursReportData = PeakHoursReportData(
                chartData = listOf(
                    ChartData(12f, "Mon", Color(0xFFB0C4DE)),
                    ChartData(18f, "Tue", Color(0xFFB0C4DE)),
                    ChartData(25f, "Wed", Color(0xFF4682B4)),
                    ChartData(15f, "Thu", Color(0xFFB0C4DE)),
                    ChartData(22f, "Fri", Color(0xFFB0C4DE)),
                    ChartData(8f, "Sat", Color(0xFFB0C4DE))
                ),
                busiestDay = "Wednesday",
                busiestTime = "10 AM - 11 AM"
            ),
            onFilterTypeChange = {},
            onDepartmentChange = {},
            onDateChange = { _, _ -> }
        )
    }
}