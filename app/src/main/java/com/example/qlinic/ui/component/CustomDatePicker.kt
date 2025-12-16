package com.example.qlinic.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.qlinic.R
import com.example.qlinic.ui.theme.QlinicTheme
import java.text.SimpleDateFormat
import java.util.*

data class DayStyle(
    val backgroundColor: Color,
    val textColor: Color
)

@Composable
fun CustomDatePicker(
    show: Boolean,
    onDismiss: () -> Unit,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    disablePastDates: Boolean,
    disableFutureDates: Boolean,
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
                disableFutureDates = disableFutureDates,
                dateStyleProvider = dateStyleProvider
            )
        }
    }
}

@Composable
fun DatePickerContent(
    onDismiss: () -> Unit,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    disablePastDates: Boolean,
    disableFutureDates: Boolean,
    dateStyleProvider: (Date) -> DayStyle?
){
    var displayedMonth by remember { mutableStateOf(Calendar.getInstance().apply { time = selectedDate }) }

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
            MonthHeader(
                currentDate = displayedMonth,
                disablePastDates = disablePastDates,
                disableFutureDates = disableFutureDates,
                onMonthChange = { newDate -> displayedMonth = newDate }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DaysOfWeekHeader()

            Spacer(modifier = Modifier.height(8.dp))

            CalendarGrid(
                displayedMonth = displayedMonth,
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

@Composable
private fun MonthHeader(
    currentDate: Calendar,
    disablePastDates: Boolean,
    disableFutureDates: Boolean,
    onMonthChange: (Calendar) -> Unit
) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val today = Calendar.getInstance()
    val isCurrentMonthAndYearForPrev = currentDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            currentDate.get(Calendar.MONTH) == today.get(Calendar.MONTH)
    val isPrevButtonEnabled = !disablePastDates || !isCurrentMonthAndYearForPrev

    val isCurrentMonthAndYearForNext = currentDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            currentDate.get(Calendar.MONTH) == today.get(Calendar.MONTH)
    val isNextButtonEnabled = !disableFutureDates || !isCurrentMonthAndYearForNext

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            enabled = isPrevButtonEnabled,
            onClick = {
                val newDate = currentDate.clone() as Calendar
                newDate.add(Calendar.MONTH, -1)
                onMonthChange(newDate)
            }) {
            Icon(painter = painterResource(id = R.drawable.ic_arrowleft), contentDescription = "Previous Month")
        }

        Text(
            text = monthFormat.format(currentDate.time),
            style = MaterialTheme.typography.displayMedium
        )

        IconButton(
            enabled = isNextButtonEnabled,
            onClick = {
                val newDate = currentDate.clone() as Calendar
                newDate.add(Calendar.MONTH, 1)
                onMonthChange(newDate)
            }) {
            Icon(painter = painterResource(id = R.drawable.ic_arrowright), contentDescription = "Next Month")
        }
    }
}

@Composable
private fun DaysOfWeekHeader() {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
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
    displayedMonth: Calendar,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    disablePastDates: Boolean,
    disableFutureDates: Boolean,
    dateStyleProvider: (Date) -> DayStyle?
) {
    val cal = displayedMonth.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)

    // Get info about the current month
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // Sunday is 1, so adjust to 0
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Create a list to hold all the dates to be displayed
    val dates = mutableListOf<Date>()

    // Get info about the previous month
    val prevMonth = displayedMonth.clone() as Calendar
    prevMonth.add(Calendar.MONTH, -1)
    val daysInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Add days from the previous month
    for (i in 0 until firstDayOfWeek) {
        val day = daysInPrevMonth - firstDayOfWeek + 1 + i
        val dateCal = prevMonth.clone() as Calendar
        dateCal.set(Calendar.DAY_OF_MONTH, day)
        dates.add(dateCal.time)
    }

    // Add days for the current month
    for (i in 1..daysInMonth) {
        val dateCal = displayedMonth.clone() as Calendar
        dateCal.set(Calendar.DAY_OF_MONTH, i)
        dates.add(dateCal.time)
    }

    // Get info about the next month
    val nextMonth = displayedMonth.clone() as Calendar
    nextMonth.add(Calendar.MONTH, 1)
    var nextMonthDay = 1

    // Add empty cells for the next month to fill the last row
    while (dates.size % 7 != 0){
        val dateCal = nextMonth.clone() as Calendar
        dateCal.set(Calendar.DAY_OF_MONTH, nextMonthDay++)
        dates.add(dateCal.time)
    }

    val chunkedDates = dates.chunked(7)

    Column {
        for (week in chunkedDates) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (date in week) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Day(
                            date = date,
                            selectedDate = selectedDate,
                            displayedMonth = displayedMonth,
                            onClick = { onDateSelected(it) },
                            disablePastDates = disablePastDates,
                            disableFutureDates = disableFutureDates,
                            dateStyleProvider = dateStyleProvider
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Day(
    date: Date,
    selectedDate: Date,
    displayedMonth: Calendar,
    onClick: (Date) -> Unit,
    disablePastDates: Boolean,
    disableFutureDates: Boolean,
    dateStyleProvider: (Date) -> DayStyle?
) {
    val cal = Calendar.getInstance().apply{ time = date }
    val day = cal.get(Calendar.DAY_OF_MONTH)

    val isSelected = isSameDay(date, selectedDate)
    val isToday = isSameDay(date, Date())
    val isInDisplayedMonth = cal.get(Calendar.MONTH) == displayedMonth.get(Calendar.MONTH)

    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)
    val isPast = date.before(today.time)
    val isFuture = date.after(today.time)

    val isEnabled = (!disablePastDates || !isPast) && (!disableFutureDates || !isFuture) && isInDisplayedMonth

    val customStyle = if (isEnabled) dateStyleProvider(date) else null

    val backgroundColor = when{
        isSelected -> MaterialTheme.colorScheme.primary
        customStyle != null -> customStyle.backgroundColor
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    val textColor = when{
        isSelected -> MaterialTheme.colorScheme.onPrimary
        customStyle != null -> customStyle.textColor
        isToday -> MaterialTheme.colorScheme.onSecondaryContainer
        isEnabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    val clickableModifier = if (isEnabled) {
        Modifier.clickable { onClick(date) }
    } else {
        Modifier // If not in the current month, disable the click
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

fun isSameDay(date1: Date, date2: Date): Boolean{
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Preview(showBackground = true, name = "Custom Date Picker Preview")
@Composable
private fun PreviewCustomDatePicker() {
    QlinicTheme {
        var selectedDate by remember { mutableStateOf(Date()) }
        DatePickerContent(
            onDismiss = {},
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            disablePastDates = false,
            disableFutureDates = true,
            dateStyleProvider = { date ->
                val cal = Calendar.getInstance().apply { time = date }
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    DayStyle(backgroundColor = Color.Red.copy(alpha = 0.3f), textColor = Color.Red)
                } else {
                    null
                }
            }
        )
    }
}