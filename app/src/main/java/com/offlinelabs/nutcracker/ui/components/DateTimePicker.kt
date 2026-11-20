package com.offlinelabs.nutcracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.components.form.CompactNumberField
import com.offlinelabs.nutcracker.ui.components.form.FieldErrorText
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import java.util.*

/** True if [text] is a whole number from 0 to [max] (used for the hour and minute fields). */
fun isValidTimePart(text: String, max: Int): Boolean {
    val value = text.toIntOrNull() ?: return false
    return value in 0..max
}

/**
 * Material's DatePicker works in UTC midnight. Converts the local calendar day of [date] to that
 * representation so the picker opens on the right day in every time zone.
 */
fun dateToPickerMillis(date: Date): Long {
    val local = Calendar.getInstance().apply { time = date }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/**
 * Applies the day picked in the DatePicker ([pickedUtcMillis], UTC midnight) to [current], keeping
 * its hour and minute (seconds and millis are zeroed).
 */
fun applyPickedDate(current: Date, pickedUtcMillis: Long): Date {
    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickedUtcMillis }
    return Calendar.getInstance().apply {
        time = current
        set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
}

/**
 * Date and time picker styled with the form kit: a date button and compact HH : MM fields with
 * inline errors for out-of-range values.
 *
 * Invalid hour/minute text never changes the selected date; use [onValidityChanged] to disable the
 * confirm action while the typed time is invalid.
 *
 * @param selectedDateTime The currently selected date and time
 * @param onDateTimeChanged Callback when date or time is changed
 * @param modifier Modifier for the component
 * @param onValidityChanged Called when the typed time becomes valid/invalid (initially true)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    selectedDateTime: Date,
    onDateTimeChanged: (Date) -> Unit,
    modifier: Modifier = Modifier,
    onValidityChanged: (Boolean) -> Unit = {}
) {
    val colors = FormDefaults.colors()
    var showDatePicker by remember { mutableStateOf(false) }

    // Extract date and time components
    val calendar = Calendar.getInstance().apply {
        time = selectedDateTime
    }
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)

    var selectedHour by remember { mutableStateOf(currentHour.toString().padStart(2, '0')) }
    var selectedMinute by remember { mutableStateOf(currentMinute.toString().padStart(2, '0')) }

    // Re-sync the text when the date/time is changed from outside (e.g. the date picker), but
    // leave what the user is typing alone when it already matches.
    LaunchedEffect(selectedDateTime) {
        if (selectedHour.toIntOrNull() != currentHour) {
            selectedHour = currentHour.toString().padStart(2, '0')
        }
        if (selectedMinute.toIntOrNull() != currentMinute) {
            selectedMinute = currentMinute.toString().padStart(2, '0')
        }
    }

    val hourValid = isValidTimePart(selectedHour, 23)
    val minuteValid = isValidTimePart(selectedMinute, 59)
    val timeValid = hourValid && minuteValid
    LaunchedEffect(timeValid) { onValidityChanged(timeValid) }

    // Format date for display
    val dateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault())
    val today = Calendar.getInstance()
    val isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                  calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

    val dateDisplayText = if (isToday) {
        stringResource(R.string.today)
    } else {
        dateFormat.format(selectedDateTime)
    }

    // Update date and time helper function
    fun updateDateTime(hour: Int?, minute: Int?) {
        val newCalendar = Calendar.getInstance().apply {
            time = selectedDateTime
            hour?.let { set(Calendar.HOUR_OF_DAY, it) }
            minute?.let { set(Calendar.MINUTE, it) }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        onDateTimeChanged(newCalendar.time)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date button - full width, styled like the kit's fields
        Surface(
            onClick = { showDatePicker = true },
            shape = RoundedCornerShape(10.dp),
            color = colors.field,
            contentColor = colors.content,
            border = BorderStroke(1.dp, colors.fieldBorder),
            modifier = Modifier.fillMaxWidth().heightIn(min = FormDefaults.FieldMinHeight)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = stringResource(R.string.select_date),
                    modifier = Modifier.size(18.dp),
                    tint = colors.accent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateDisplayText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.content,
                    maxLines = 1
                )
            }
        }

        // Time inputs on a separate row: [clock] ........ [HH] : [MM]
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(18.dp).padding(start = 0.dp),
                tint = colors.accent
            )
            Spacer(modifier = Modifier.weight(1f))
            CompactNumberField(
                value = selectedHour,
                onValueChange = { newValue ->
                    // Only allow 2 digits
                    if (newValue.length <= 2 && newValue.all { it.isDigit() }) {
                        selectedHour = newValue
                        val hour = newValue.toIntOrNull()
                        if (hour != null && hour in 0..23) {
                            updateDateTime(hour = hour, minute = null)
                        }
                    }
                },
                modifier = Modifier.width(64.dp),
                accessibilityLabel = stringResource(R.string.hour),
                isError = !hourValid,
                errorText = if (!hourValid) stringResource(R.string.datetime_error_hour) else null,
                placeholder = stringResource(R.string.hour),
                keyboardType = KeyboardType.Number,
                textAlign = TextAlign.Center,
                imeAction = ImeAction.Next,
                colors = colors
            )
            Text(
                text = stringResource(R.string.time_separator),
                style = MaterialTheme.typography.titleLarge,
                color = colors.content,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            CompactNumberField(
                value = selectedMinute,
                onValueChange = { newValue ->
                    // Only allow 2 digits
                    if (newValue.length <= 2 && newValue.all { it.isDigit() }) {
                        selectedMinute = newValue
                        val minute = newValue.toIntOrNull()
                        if (minute != null && minute in 0..59) {
                            updateDateTime(hour = null, minute = minute)
                        }
                    }
                },
                modifier = Modifier.width(64.dp),
                accessibilityLabel = stringResource(R.string.minute),
                isError = !minuteValid,
                errorText = if (!minuteValid) stringResource(R.string.datetime_error_minute) else null,
                placeholder = stringResource(R.string.minute),
                keyboardType = KeyboardType.Number,
                textAlign = TextAlign.Center,
                imeAction = ImeAction.Next,
                colors = colors
            )
        }
        if (!hourValid) FieldErrorText(stringResource(R.string.datetime_error_hour), colors = colors)
        if (!minuteValid) FieldErrorText(stringResource(R.string.datetime_error_minute), colors = colors)
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateToPickerMillis(selectedDateTime)
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Preserves the time from the current selection
                            onDateTimeChanged(applyPickedDate(selectedDateTime, millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
