package com.offlinelabs.nutcracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor
import java.text.SimpleDateFormat
import java.util.*

/**
 * A composable that provides date and time picker UI
 * @param selectedDateTime The currently selected date and time
 * @param onDateTimeChanged Callback when date or time is changed
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    selectedDateTime: Date,
    onDateTimeChanged: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Extract date and time components
    val calendar = Calendar.getInstance().apply {
        time = selectedDateTime
    }
    
    var selectedHour by remember(selectedDateTime) {
        mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0'))
    }
    var selectedMinute by remember(selectedDateTime) {
        mutableStateOf(calendar.get(Calendar.MINUTE).toString().padStart(2, '0'))
    }
    
    // Format date for display
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
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
        // Date picker button - full width
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = stringResource(R.string.select_date),
                modifier = Modifier.size(18.dp),
                tint = getContrastingTextColor(MaterialTheme.colorScheme.surface)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = dateDisplayText,
                color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
            )
        }
        
        // Time inputs on a separate row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
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
                label = { Text(stringResource(R.string.hour)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Text(
                text = stringResource(R.string.time_separator),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            OutlinedTextField(
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
                label = { Text(stringResource(R.string.minute)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateTime.time
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newCalendar = Calendar.getInstance().apply {
                                timeInMillis = millis
                                // Preserve the time from current selection
                                set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onDateTimeChanged(newCalendar.time)
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

