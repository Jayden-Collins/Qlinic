package com.example.qlinic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.qlinic.R
import com.example.qlinic.ui.theme.QlinicTheme
import com.example.qlinic.ui.theme.green
import com.example.qlinic.ui.theme.teal
import java.text.SimpleDateFormat
import java.util.*

// DayStyle used by single-date picker (non-nullable colors)
data class DayStyle(
    val backgroundColor: Color,
    val textColor: Color,
    val hasAppointment: Boolean = false,
    val isOnLeave: Boolean = false
)

// Popup wrapper for single-date reschedule picker. Use this in reschedule flows.
@Composable
fun RescheduleDatePicker(
    show: Boolean,
    onDismiss: () -> Unit,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    disablePastDates: Boolean,
    dateStyleProvider: (Date) -> DayStyle? = { null }
) {
    if (show) {
        Popup(
            alignment = Alignment.Center,
            properties = PopupProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
            onDismissRequest = onDismiss
        ) {
            DatePickerContent(
                onDismiss = onDismiss,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                disablePastDates = disablePastDates,
                dateStyleProvider = dateStyleProvider
            )
        }
    }
}

@Composable
fun CustomDatePicker(
    modifier: Modifier = Modifier,
    selectedDates: List<Date> = emptyList(), // Support multiple selected dates
    appointmentDates: List<Date> = emptyList(), // Dates with appointments
    leaveDates: List<Date> = emptyList(), // leave off dates
    onDateSelected: (Date) -> Unit, // For single selection
    onMultipleSelectionChanged: (List<Date>) -> Unit, // For multi-selection
    enableMultiSelect: Boolean = true, // Toggle multi-select mode
    disablePastDates: Boolean = false, // if true, disallow selecting past dates
    onMonthChanged: (Calendar) -> Unit = {}, // notify parent when visible month changes
) {
    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }

    // Precompute normalized epoch (start-of-day millis) set for fast lookups in the calendar grid
    val appointmentEpochs: Set<Long> = remember(appointmentDates) {
        appointmentDates.map { date ->
            val cal = Calendar.getInstance().apply { time = date }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.toSet()
    }

    Surface(
        modifier = modifier
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 360.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.onPrimary, // The main background color from your theme
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MonthHeader(
                currentDate = currentDate,
                onPreviousMonth = {
                    val newDate = currentDate.clone() as Calendar
                    newDate.add(Calendar.MONTH, -1)
                    currentDate = newDate
                    onMonthChanged(newDate)
                },
                onNextMonth = {
                    val newDate = currentDate.clone() as Calendar
                    newDate.add(Calendar.MONTH, 1)
                    currentDate = newDate
                    onMonthChanged(newDate)
                }
            )

            // Multi-select toggle
            if (enableMultiSelect) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Multi-select: ",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Switch(
                        checked = isMultiSelectMode,
                        onCheckedChange = { isMultiSelectMode = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            DaysOfWeekHeader()
            Spacer(modifier = Modifier.height(8.dp))
            CalendarGrid(
                currentDate = currentDate,
                selectedDates = selectedDates,
                appointmentDates = appointmentDates,
                appointmentEpochs = appointmentEpochs,
                leaveDates = leaveDates,
                onDateSelected = onDateSelected,
                isMultiSelectMode = isMultiSelectMode,
                disablePastDates = disablePastDates,
                onMultipleSelectionChanged = onMultipleSelectionChanged
            )

            // Selection info
            if (isMultiSelectMode && selectedDates.isNotEmpty()) {
                Text(
                    text = "${selectedDates.size} date(s) selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DatePickerContent(
    onDismiss: () -> Unit,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    disablePastDates: Boolean,
    dateStyleProvider: (Date) -> DayStyle? = { null },
    onMonthChanged: (Calendar) -> Unit = {}
){
    var displayedMonth by remember { mutableStateOf(Calendar.getInstance().apply { time = selectedDate }) }

    // When used inside a Popup, CalendarGridSingle used below used to call onDismiss() immediately
    // after calling onDateSelected. Dismissing the popup synchronously can cancel composable-scoped
    // coroutine work and lead to the runtime "The coroutine scope left the composition" error.
    // To avoid that, trigger the dismiss via a small, composition-safe side-effect instead of
    // calling it synchronously inside the click callback.
    var pendingDismissForDate by remember { mutableStateOf<Date?>(null) }
    LaunchedEffect(pendingDismissForDate) {
        val d = pendingDismissForDate
        if (d != null) {
            // allow the selection to settle before dismissing the popup
            // (this avoids cancelling any composable-scoped coroutines started by parent handlers)
            pendingDismissForDate = null
            onDismiss()
        }
    }

    Surface(
        modifier = Modifier
            .width(320.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.onPrimary, // The main background color from your theme
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MonthHeaderSingle(
                currentDate = displayedMonth,
                disablePastDates = disablePastDates,
                onMonthChange = { newDate ->
                    displayedMonth = newDate
                    onMonthChanged(newDate)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DaysOfWeekHeader()

            Spacer(modifier = Modifier.height(8.dp))

            CalendarGridSingle(
                displayedMonth = displayedMonth,
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    onDateSelected(date)
                    // schedule a safe dismiss via side-effect instead of calling onDismiss() immediately
                    pendingDismissForDate = date
                },
                disablePastDates = disablePastDates,
                dateStyleProvider = dateStyleProvider
            )
        }
    }
}


@Composable
private fun MonthHeader(
    currentDate: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.size(36.dp)
        ){
            Icon(
                painter = painterResource(id = R.drawable.ic_arrowleft),
                contentDescription = "Previous Month",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Center the month text while keeping arrows visible
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = monthFormat.format(currentDate.time),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrowright),
                contentDescription = "Next Month",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Single-date month header (used by reschedule popup)
@Composable
private fun MonthHeaderSingle(
    currentDate: Calendar,
    disablePastDates: Boolean = false,
    onMonthChange: (Calendar) -> Unit
) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val today = Calendar.getInstance()

    fun isMonthBefore(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) < b.get(Calendar.YEAR) ||
                (a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.MONTH) < b.get(Calendar.MONTH))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                val newDate = currentDate.clone() as Calendar
                newDate.add(Calendar.MONTH, -1)
                if (!disablePastDates || !isMonthBefore(newDate, today)) onMonthChange(newDate)
            },
            modifier = Modifier.size(36.dp),
            enabled = !disablePastDates || !isMonthBefore(currentDate.clone() as Calendar, today)
        ){
            Icon(
                painter = painterResource(id = R.drawable.ic_arrowleft),
                contentDescription = "Previous Month",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = monthFormat.format(currentDate.time),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        IconButton(
            onClick = {
                val newDate = currentDate.clone() as Calendar
                newDate.add(Calendar.MONTH, 1)
                onMonthChange(newDate)
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrowright),
                contentDescription = "Next Month",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Single-date calendar grid used by reschedule popup. Honors disablePastDates and dateStyleProvider.
@Composable
private fun CalendarGridSingle(
    displayedMonth: Calendar,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    disablePastDates: Boolean = false,
    dateStyleProvider: (Date) -> DayStyle? = { null }
) {
    val cal = displayedMonth.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val dates = mutableListOf<Calendar?>()
    if (firstDayOfWeek > 0) {
        val prev = displayedMonth.clone() as Calendar
        prev.add(Calendar.MONTH, -1)
        val daysInPrev = prev.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 0 until firstDayOfWeek) {
            val d = prev.clone() as Calendar
            d.set(Calendar.DAY_OF_MONTH, daysInPrev - firstDayOfWeek + 1 + i)
            dates.add(d)
        }
    }

    for (d in 1..daysInMonth) {
        val dayCal = displayedMonth.clone() as Calendar
        dayCal.set(Calendar.DAY_OF_MONTH, d)
        dates.add(dayCal)
    }

    // fill next month
    var nextDay = 1
    while (dates.size % 7 != 0) {
        val d = displayedMonth.clone() as Calendar
        d.add(Calendar.MONTH, 1)
        d.set(Calendar.DAY_OF_MONTH, nextDay++)
        dates.add(d)
    }

    val weeks = dates.chunked(7)
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    Column {
        weeks.forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                week.forEach { calDay ->
                    if (calDay == null) {
                        Box(modifier = Modifier.weight(1f).height(40.dp))
                    } else {
                        val date = calDay.time
                        val isInMonth = calDay.get(Calendar.MONTH) == displayedMonth.get(Calendar.MONTH)
                        val isPast = calDay.time.before(today.time)

                        // obtain style unconditionally so we can disable appointment/leave days
                        val style = dateStyleProvider(date)

                        // a day is enabled only if it's in the displayed month, not past when disabled,
                        // and not marked as appointment or on-leave by the provider
                        val enabled = isInMonth && (!disablePastDates || !isPast) && (style?.hasAppointment != true) && (style?.isOnLeave != true)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Day(
                                day = calDay.get(Calendar.DAY_OF_MONTH),
                                hasAppointment = style?.hasAppointment ?: false,
                                isOnLeave = style?.isOnLeave ?: false,
                                isSelected = isSameDay(date, selectedDate),
                                isMultiSelectMode = false,
                                isCurrentMonth = isInMonth,
                                isSelectable = enabled,
                                onClick = {
                                    if (enabled) onDateSelected(date)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaysOfWeekHeader() {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Row(
        modifier = Modifier.fillMaxWidth(),
//            Icon(painter = painterResource(id = R.drawable.ic_arrowright), contentDescription = "Next Month", tint = MaterialTheme.colorScheme.onSurface)
    ) {
        for (day in days) {
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    currentDate: Calendar,
    selectedDates: List<Date>,
    appointmentDates: List<Date>,
    appointmentEpochs: Set<Long>,
    leaveDates: List<Date>,
    onDateSelected: (Date) -> Unit,
    isMultiSelectMode: Boolean,
    disablePastDates: Boolean,
    onMultipleSelectionChanged: (List<Date>) -> Unit,
) {
    val calendar = currentDate.clone() as Calendar
    calendar.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // create list of days
    val days = mutableListOf<Calendar?>()

    // add empty cells
    // use repeat to avoid unused loop variable warning
    if (firstDayOfMonth > 1) repeat(firstDayOfMonth - 1) { days.add(null) }

    // add actual days
    for (day in 1..daysInMonth) {
        val dayCalendar = currentDate.clone() as Calendar
        dayCalendar.set(Calendar.DAY_OF_MONTH, day)
        days.add(dayCalendar)
    }

    // Calculate weeks needed (always show 6 weeks for consistency)
    val totalCellsNeeded = 42 // 6 weeks * 7 days
    while (days.size < totalCellsNeeded) days.add(null)

    val weeks = days.chunked(7)

    // Precompute epoch sets for leave and selected dates to avoid repeated allocation in loop
    val leaveEpochs = remember(leaveDates) {
        leaveDates.map { d ->
            val c = Calendar.getInstance().apply { time = d }
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }.toSet()
    }

    val selectedEpochs = remember(selectedDates) {
        selectedDates.map { d ->
            val c = Calendar.getInstance().apply { time = d }
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }.toSet()
    }

    Column {
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { calendarDay ->
                    if (calendarDay != null) {
                        val dayStartCal = (calendarDay.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        val dayStartEpoch = dayStartCal.timeInMillis
                        val hasAppointment = appointmentEpochs.contains(dayStartEpoch)
                        val isOnLeave = leaveEpochs.contains(dayStartEpoch)
                        val isSelected = selectedEpochs.contains(dayStartEpoch)

                        val date = calendarDay.time

                        // determine if this date is in the past (compare at start of day)
                        val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        val isPast = calendarDay.time.before(todayStart.time)

                        val isSelectable = !isOnLeave && !hasAppointment && (!isPast || !disablePastDates)

                        Day(
                            day = calendarDay.get(Calendar.DAY_OF_MONTH),
                            hasAppointment = hasAppointment,
                            isOnLeave = isOnLeave,
                            isSelected = isSelected,
                            isMultiSelectMode = isMultiSelectMode,
                            isCurrentMonth = calendarDay.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH),
                            isSelectable = isSelectable,

                            onClick = {
                                handleDateClick(
                                    date = date,
                                    selectedDates = selectedDates,
                                    isMultiSelectMode = isMultiSelectMode,
                                    isSelectable = isSelectable,
                                    onDateSelected = onDateSelected,
                                    onMultipleSelectionChanged = onMultipleSelectionChanged
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun handleDateClick(
    date: Date,
    selectedDates: List<Date>,
    isMultiSelectMode: Boolean,
    isSelectable: Boolean,
    onDateSelected: (Date) -> Unit,
    onMultipleSelectionChanged: (List<Date>) -> Unit,
) {
    if (!isSelectable) return

    if (isMultiSelectMode) {
        // Toggle selection in multi-select mode
        val newSelection = if (selectedDates.any { isSameDay(it, date) }) {
            // Deselect if already selected
            selectedDates.filterNot { isSameDay(it, date) }
        } else {
            // Add to selection
            selectedDates + date
        }
        onMultipleSelectionChanged(newSelection)
    } else {
        // Single selection mode - toggle selection when tapping the same date
        if (selectedDates.any { isSameDay(it, date) }) {
            // If already selected, clear selection
            onMultipleSelectionChanged(emptyList())
        } else {
            // Select this date
            onDateSelected(date)
        }
    }
}

@Composable
private fun Day(
    day: Int,
    hasAppointment: Boolean,
    isOnLeave: Boolean,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    isCurrentMonth: Boolean,
    isSelectable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    // Determine background color
    val backgroundColor = when {
        isSelected -> {
            // Make both single and multi-selection use solid black background
            Color.Black
        }
        hasAppointment -> teal.copy(0.8f) // light green blue for appointments
        isOnLeave -> Color.LightGray.copy(alpha = 0.7f) // LightGray for leave days
        else -> Color.Transparent
    }

    // Determine text color
    val textColor = when {
        isSelected -> Color.White
        !isCurrentMonth -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        isOnLeave -> Color.Gray.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick, enabled = isSelectable ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(
                    width = when {
                        isSelected -> 2.dp
                        hasAppointment -> 1.5.dp
                        else -> 0.dp
                    },
                    color = when {
                        isSelected -> Color.Black
                        hasAppointment -> green
                        else -> Color.Transparent
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (!isCurrentMonth) MaterialTheme.typography.bodySmall.fontSize
                else MaterialTheme.typography.bodyMedium.fontSize
            )
        }

        // Show checkmark for multi-select mode
        if (isSelected && isMultiSelectMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SELECT",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper function
fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
            cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
}

@Preview
@Composable
fun PreviewCustomDatePicker() {
    QlinicTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                CustomDatePicker(
                    selectedDates = listOf(Date(), Calendar.getInstance().apply {
                        add(Calendar.DATE, 1)
                    }.time),
                    appointmentDates = listOf(Date()),
                    leaveDates = listOf(
                        Calendar.getInstance().apply {
                            add(Calendar.DATE, 3)
                        }.time
                    ),
                    onDateSelected = { println("Selected: $it") },
                    onMultipleSelectionChanged = { println("Multiple selected: $it") },
                    enableMultiSelect = true,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewCustomDatePickerSingleMode() {
    QlinicTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                CustomDatePicker(
                    selectedDates = listOf(Date()),
                    appointmentDates = listOf(Date()),
                    leaveDates = emptyList(),
                    onDateSelected = { println("Selected: $it") },
                    onMultipleSelectionChanged = { println("Multiple selected: $it") },
                    enableMultiSelect = false,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
