package com.example.qlinic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.qlinic.R
import com.example.qlinic.ui.theme.QlinicTheme
import com.example.qlinic.ui.theme.darkblue
import com.example.qlinic.ui.theme.teal
import java.text.SimpleDateFormat
import java.util.*

// DayStyle used for coloring/styling specific days
data class DayStyle(
    val backgroundColor: Color,
    val textColor: Color,
    val hasAppointment: Boolean = false,
    val isOnLeave: Boolean = false
)

/**
 * A dialog/popup version of the DatePicker.
 */
@Composable
fun CustomDatePickerDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    disablePastDates: Boolean = false,
    disableFutureDates: Boolean = false,
    dateStyleProvider: (Date) -> DayStyle? = { null }
) {
    if (show) {
        Popup(
            alignment = Alignment.Center,
            properties = PopupProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                focusable = true
            ),
            onDismissRequest = onDismiss
        ) {
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                DatePickerContent(
                    selectedDate = selectedDate,
                    onDateSelected = {
                        onDateSelected(it)
                        onDismiss()
                    },
                    disablePastDates = disablePastDates,
                    disableFutureDates = disableFutureDates,
                    dateStyleProvider = dateStyleProvider
                )
            }
        }
    }
}

/**
 * Inline DatePicker component used in the Schedule screen.
 */
@Composable
fun CustomDatePicker(
    modifier: Modifier = Modifier,
    selectedDates: List<Date> = emptyList(),
    appointmentDates: List<Date> = emptyList(),
    leaveDates: List<Date> = emptyList(),
    onDateSelected: (Date) -> Unit = {},
    onMultipleSelectionChanged: (List<Date>) -> Unit = {},
    enableMultiSelect: Boolean = true,
    disablePastDates: Boolean = false,
    onMonthChanged: (Calendar) -> Unit = {},
) {
    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }

    val appointmentEpochs = remember(appointmentDates) {
        appointmentDates.map { normalizeDate(it).timeInMillis }.toSet()
    }
    val leaveEpochs = remember(leaveDates) {
        leaveDates.map { normalizeDate(it).timeInMillis }.toSet()
    }
    val selectedEpochs = remember(selectedDates) {
        selectedDates.map { normalizeDate(it).timeInMillis }.toSet()
    }

    Surface(
        modifier = modifier
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 360.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MonthHeaderRow(
                currentDate = currentDate,
                disablePastDates = disablePastDates,
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

            if (enableMultiSelect) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Multi-select: ", style = MaterialTheme.typography.labelSmall)
                    Switch(
                        checked = isMultiSelectMode,
                        onCheckedChange = { isMultiSelectMode = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            DaysOfWeekHeader()
            Spacer(modifier = Modifier.height(8.dp))

            CalendarGridUnified(
                displayedMonth = currentDate,
                selectedEpochs = selectedEpochs,
                appointmentEpochs = appointmentEpochs,
                leaveEpochs = leaveEpochs,
                isMultiSelectMode = isMultiSelectMode,
                disablePastDates = disablePastDates,
                onDateClick = { date ->
                    if (isMultiSelectMode) {
                        val dateEpoch = normalizeDate(date).timeInMillis
                        val newList = if (selectedEpochs.contains(dateEpoch)) {
                            selectedDates.filterNot { isSameDay(it, date) }
                        } else {
                            selectedDates + date
                        }
                        onMultipleSelectionChanged(newList)
                    } else {
                        onDateSelected(date)
                        onMultipleSelectionChanged(listOf(date))
                    }
                }
            )

            if (isMultiSelectMode && selectedDates.isNotEmpty()) {
                Text(
                    text = "${selectedDates.size} date(s) selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * Standard content for single-selection date pickers.
 */
@Composable
fun DatePickerContent(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    disablePastDates: Boolean = false,
    disableFutureDates: Boolean = false,
    leaveDates: List<Date> = emptyList(),
    dateStyleProvider: (Date) -> DayStyle? = { null },
    onMonthChanged: (Calendar) -> Unit = {}
) {
    var displayedMonth by remember { mutableStateOf(Calendar.getInstance().apply { time = selectedDate }) }

    val leaveEpochs = remember(leaveDates) {
        leaveDates.map { normalizeDate(it).timeInMillis }.toSet()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        MonthHeaderRow(
            currentDate = displayedMonth,
            disablePastDates = disablePastDates,
            onPreviousMonth = {
                val newDate = displayedMonth.clone() as Calendar
                newDate.add(Calendar.MONTH, -1)
                displayedMonth = newDate
                onMonthChanged(newDate)
            },
            onNextMonth = {
                val newDate = displayedMonth.clone() as Calendar
                newDate.add(Calendar.MONTH, 1)
                displayedMonth = newDate
                onMonthChanged(newDate)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        DaysOfWeekHeader()
        Spacer(modifier = Modifier.height(8.dp))

        CalendarGridUnified(
            displayedMonth = displayedMonth,
            selectedEpochs = setOf(normalizeDate(selectedDate).timeInMillis),
            appointmentEpochs = emptySet(),
            leaveEpochs = leaveEpochs,
            isMultiSelectMode = false,
            disablePastDates = disablePastDates,
            disableFutureDates = disableFutureDates,
            dateStyleProvider = dateStyleProvider,
            onDateClick = onDateSelected
        )
    }
}

@Composable
private fun MonthHeaderRow(
    currentDate: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    disablePastDates: Boolean = false
) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val isCurrentMonthOrEarlier = remember(currentDate) {
        val today = Calendar.getInstance()
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        val todayYear = today.get(Calendar.YEAR)
        val todayMonth = today.get(Calendar.MONTH)
        
        year < todayYear || (year == todayYear && month <= todayMonth)
    }
    val prevEnabled = !(disablePastDates && isCurrentMonthOrEarlier)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onPreviousMonth, 
            enabled = prevEnabled,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painterResource(R.drawable.ic_arrowleft), 
                contentDescription = "Prev", 
                tint = if (prevEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
        Text(
            text = monthFormat.format(currentDate.time),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
            Icon(painterResource(R.drawable.ic_arrowright), contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun DaysOfWeekHeader() {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Row(modifier = Modifier.fillMaxWidth()) {
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
private fun CalendarGridUnified(
    displayedMonth: Calendar,
    selectedEpochs: Set<Long>,
    appointmentEpochs: Set<Long>,
    leaveEpochs: Set<Long>,
    isMultiSelectMode: Boolean,
    disablePastDates: Boolean = false,
    disableFutureDates: Boolean = false,
    dateStyleProvider: (Date) -> DayStyle? = { null },
    onDateClick: (Date) -> Unit
) {
    val calendar = displayedMonth.clone() as Calendar
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val dates = mutableListOf<Calendar?>()
    repeat(firstDayOfWeek) { dates.add(null) }
    for (i in 1..daysInMonth) {
        val d = displayedMonth.clone() as Calendar
        d.set(Calendar.DAY_OF_MONTH, i)
        dates.add(d)
    }
    while (dates.size % 7 != 0) { dates.add(null) }

    val today = normalizeDate(Date()).timeInMillis

    Column {
        dates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                week.forEach { cal ->
                    if (cal == null) {
                        Spacer(Modifier.weight(1f).height(40.dp))
                    } else {
                        val date = cal.time
                        val epoch = normalizeDate(date).timeInMillis
                        val isSelected = selectedEpochs.contains(epoch)
                        val hasAppointment = appointmentEpochs.contains(epoch)
                        val isOnLeave = leaveEpochs.contains(epoch)
                        
                        val isPast = epoch < today
                        val isFuture = epoch > today
                        val isSelectable = (!disablePastDates || !isPast) && (!disableFutureDates || !isFuture) && !isOnLeave && !hasAppointment

                        // If style provider gives a style, we use it (e.g. for specific coloring)
                        val customStyle = dateStyleProvider(date)

                        Day(
                            day = cal.get(Calendar.DAY_OF_MONTH).toString(),
                            isSelected = isSelected,
                            isToday = epoch == today,
                            hasAppointment = hasAppointment || (customStyle?.hasAppointment == true),
                            isOnLeave = isOnLeave || (customStyle?.isOnLeave == true),
                            isSelectable = isSelectable,
                            customStyle = customStyle,
                            onClick = { if (isSelectable) onDateClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Day(
    day: String,
    isSelected: Boolean,
    isToday: Boolean,
    hasAppointment: Boolean,
    isOnLeave: Boolean,
    isSelectable: Boolean,
    customStyle: DayStyle?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> Color.Black
        customStyle != null -> customStyle.backgroundColor
        hasAppointment -> teal.copy(0.8f)
        isOnLeave -> Color.LightGray.copy(0.7f)
        isToday -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> Color.White
        !isSelectable -> MaterialTheme.colorScheme.outline.copy(0.4f)
        customStyle != null -> customStyle.textColor
        isOnLeave -> Color.Gray
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Show 'Unavailable' label for leave or appointment days
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(enabled = isSelectable, onClick = onClick)
            .border(
                width = if (hasAppointment) 1.5.dp else 0.dp,
                color = if (hasAppointment) teal else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day,
            color = textColor,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

private fun normalizeDate(date: Date): Calendar {
    return Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = normalizeDate(date1)
    val cal2 = normalizeDate(date2)
    return cal1.timeInMillis == cal2.timeInMillis
}

@Preview(showBackground = true)
@Composable
fun PreviewUnifiedDatePicker() {
    QlinicTheme {
        Column(Modifier.padding(16.dp)) {
            CustomDatePicker(
                selectedDates = listOf(Date()),
                appointmentDates = listOf(Calendar.getInstance().apply { add(Calendar.DATE, 2) }.time),
                leaveDates = listOf(Calendar.getInstance().apply { add(Calendar.DATE, 5) }.time),
                enableMultiSelect = true
            )
        }
    }
}
