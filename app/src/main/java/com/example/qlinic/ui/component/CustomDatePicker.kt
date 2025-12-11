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

@Composable
fun CustomDatePicker(
    show: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (Date) -> Unit
) {
    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }

    if (show) {
        Popup(
            alignment = Alignment.Center,
            properties = PopupProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
            onDismissRequest = onDismiss
        ) {
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
                        currentDate = currentDate,
                        onMonthChange = { newDate -> currentDate = newDate }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DaysOfWeekHeader()
                    Spacer(modifier = Modifier.height(8.dp))
                    CalendarGrid(
                        currentDate = currentDate,
                        onDateSelected = { day ->
                            val selectedCal = currentDate.clone() as Calendar
                            selectedCal.set(Calendar.DAY_OF_MONTH, day)
                            onDateSelected(selectedCal.time)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(currentDate: Calendar, onMonthChange: (Calendar) -> Unit) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
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

        IconButton(onClick = {
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
private fun CalendarGrid(currentDate: Calendar, onDateSelected: (Int) -> Unit) {
    val cal = currentDate.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)

    // Get info about the current month
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // Sunday is 1, so adjust to 0
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Get info about the previous month
    val prevMonth = cal.clone() as Calendar
    prevMonth.add(Calendar.MONTH, -1)
    val daysInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

    val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    val isCurrentMonthAndYear = cal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)

    // Create a list to hold all the dates to be displayed
    val dates = mutableListOf<Pair<Int, Boolean>>()

    // 1. Add days from the previous month
    for (i in 0 until firstDayOfWeek) {
        val day = daysInPrevMonth - firstDayOfWeek + 1 + i
        dates.add(Pair(day, false)) // false indicates it's not in the current month
    }

    // 2. Add days from the current month
    for (i in 1..daysInMonth) {
        dates.add(Pair(i, true)) // true indicates it's in the current month
    }

    // 3. Add days from the next month to fill the grid
    val remaining = 42 - dates.size // A 6-week grid has 42 cells
    for (i in 1..remaining) {
        dates.add(Pair(i, false)) // false indicates it's not in the current month
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
                for ((day, isInCurrentMonth) in week) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        val isToday = day == today && isCurrentMonthAndYear && isInCurrentMonth
                        Day(
                            day = day,
                            isToday = isToday,
                            isInCurrentMonth = isInCurrentMonth,
                            onClick = { if (isInCurrentMonth) onDateSelected(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Day(day: Int, isToday: Boolean, isInCurrentMonth: Boolean, onClick: (Int) -> Unit) {
    val clickableModifier = if (isInCurrentMonth) {
        Modifier.clickable { onClick(day) }
    } else {
        Modifier // If not in the current month, disable the click
    }

    val backgroundColor = if (isToday) MaterialTheme.colorScheme.scrim else Color.Transparent

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        // NEW: Use MaterialTheme colors for the text
        val textColor = when {
            isToday -> MaterialTheme.colorScheme.onPrimary // Text color on the selected day
            isInCurrentMonth -> MaterialTheme.colorScheme.onSurface // Normal day text
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // Disabled (faded) color
        }

        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}


@Preview(showBackground = true, name = "Custom Date Picker Preview")
@Composable
private fun PreviewCustomDatePicker() {
    QlinicTheme {
        Surface(
            modifier = Modifier
                .width(300.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                var currentDate by remember { mutableStateOf(Calendar.getInstance()) }
                MonthHeader(
                    currentDate = currentDate,
                    onMonthChange = { newDate -> currentDate = newDate }
                )
                Spacer(modifier = Modifier.height(16.dp))
                DaysOfWeekHeader()
                Spacer(modifier = Modifier.height(8.dp))
                CalendarGrid(
                    currentDate = currentDate,
                    onDateSelected = { }
                )
            }
        }
    }
}