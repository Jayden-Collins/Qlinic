package com.example.qlinic.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(
    paddingValues: PaddingValues,
    viewModel: ReportViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val peakHoursReportData by viewModel.peakHoursReportData.collectAsState()

    ReportContent(
        paddingValues = paddingValues,
        isLoading = isLoading,
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

@Composable
fun ReportContent(
    paddingValues: PaddingValues,
    isLoading: Boolean,
    filterState: ReportFilterState,
    stats: AppointmentStatistics,
    peakHoursReportData: PeakHoursReportData,
    onFilterTypeChange: (String) -> Unit,
    onDepartmentChange: (String) -> Unit,
    onDateChange: (Boolean, String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var isStartDatePicker by remember { mutableStateOf(true) }
    var showSummaryInfoDialog by remember { mutableStateOf(false) }
    var showPeakHoursInfoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onPrimary)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.appointments_summary_title), style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_info),
                        contentDescription = "More information about Appointments Summary",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { showSummaryInfoDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Filters
                FilterDropdown(
                    stringResource(R.string.types_filter_label),
                    listOf("Weekly", "Monthly", "Yearly", "Custom Date Range"),
                    filterState.selectedType
                ) {
                    onFilterTypeChange(it)
                }

                AnimatedVisibility(visible = filterState.isCustomRangeVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
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
                    stringResource(R.string.department_filter_label),
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
                        stringResource(R.string.total_stat_label),
                        stats.total,
                        pillColor = MaterialTheme.colorScheme.primary,
                        onPillColor = MaterialTheme.colorScheme.onPrimary
                    )
                    StatItem(
                        stringResource(R.string.completed_stat_label),
                        stats.completed,
                        stats.completedPercent,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onPrimary,
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    StatItem(
                        stringResource(R.string.cancelled_stat_label),
                        stats.cancelled,
                        stats.cancelledPercent,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onPrimary,
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                PeakHoursReport(
                    reportData = peakHoursReportData,
                    onInfoClick = { showPeakHoursInfoDialog = true }
                )

                if (showSummaryInfoDialog) {
                    AlertDialog(
                        onDismissRequest = { showSummaryInfoDialog = false },
                        title = { Text(text = stringResource(R.string.appointments_summary_title),  style = MaterialTheme.typography.displayMedium) },
                        text = { Text(stringResource(R.string.more_info_details_appt_sum), style = MaterialTheme.typography.bodySmall) },
                        confirmButton = {
                            TextButton(onClick = { showSummaryInfoDialog = false }) {
                                Text(stringResource(R.string.ok))
                            }
                        }
                    )
                }

                if (showPeakHoursInfoDialog) {
                    AlertDialog(
                        onDismissRequest = { showPeakHoursInfoDialog = false },
                        title = { Text(text = stringResource(R.string.peak_hours_report_title), style = MaterialTheme.typography.displayMedium) },
                        text = { Text(stringResource(R.string.more_info_details_peak_hours), style = MaterialTheme.typography.bodySmall) },
                        confirmButton = {
                            TextButton(onClick = { showPeakHoursInfoDialog = false }) {
                                Text(stringResource(R.string.ok))
                            }
                        }
                    )
                }
            }
        }
    }

    val dateToPass = if (isStartDatePicker) filterState.startDate else filterState.endDate

    CustomDatePicker(
        show = showDatePicker,
        onDismiss = { showDatePicker = false },
        onDateSelected = { date ->
            val formattedDate = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
            onDateChange(isStartDatePicker, formattedDate)
        },
        selectedDate = try {
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).parse(dateToPass)
        } catch (e: Exception) {
            Date()
        },
        disablePastDates = false,
        disableFutureDates = true
    )
}

@Composable
fun PeakHoursReport(
    reportData: PeakHoursReportData,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.peak_hours_report_title), style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_info),
                contentDescription = "More information about Peak Hours Report",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onInfoClick() }
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
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(reportData.busiestCategoryLabel, style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = reportData.busiestDay,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.busiest_time_label), style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = reportData.busiestTime,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    BarChartWithAxis(data = reportData.chartData)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp), contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_peak_hours_data),
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
    val yAxisLabels = listOf(
        "0",
        (maxValue / 2).toInt().toString(),
        maxValue.toInt().toString()
    )

    val busiestBarColor = MaterialTheme.colorScheme.primary
    val defaultBarColor = MaterialTheme.colorScheme.secondaryContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
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

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxVal = data.maxOfOrNull { it.value }
                    data.forEach { chartData ->
                        val barColor = if (chartData.value == maxVal) busiestBarColor else defaultBarColor

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = chartData.value.toInt().toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .fillMaxHeight(if (maxValue > 0) chartData.value / maxValue else 0f)
                                    .background(
                                        color = barColor,
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { chartData ->
                    Text(
                        text = if (data.size > 7) chartData.label.take(1) else chartData.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Report Screen Default")
@Composable
fun PreviewReportScreen() {
    QlinicTheme {
        ReportContent(
            paddingValues = PaddingValues(0.dp),
            isLoading = false,
            filterState = ReportFilterState(),
            stats = AppointmentStatistics(50, 48, 2),
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
