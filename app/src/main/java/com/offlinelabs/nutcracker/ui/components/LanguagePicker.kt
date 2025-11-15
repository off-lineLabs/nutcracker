package com.offlinelabs.nutcracker.ui.components

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Box
import java.util.Locale
import com.offlinelabs.nutcracker.util.LocaleUtils
import com.offlinelabs.nutcracker.util.logger.AppLogger

@Composable
fun LanguagePicker(
    context: Context,
    currentLocale: Locale,
    onLocaleChanged: (Locale) -> Unit
) {
    val supportedLocales = remember { LocaleUtils.getSupportedLocales(context) }
    var expanded by remember { mutableStateOf(false) }
    var selectedLocale by remember { mutableStateOf(currentLocale) }

    Box {
        Button(onClick = { expanded = true }) {
            Text(selectedLocale.displayName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            supportedLocales.forEach { locale ->
                DropdownMenuItem(
                    text = { Text(locale.displayName) },
                    onClick = {
                        AppLogger.d("LanguagePicker", "User selected locale: ${locale.toLanguageTag()} (${locale.displayName})")
                        selectedLocale = locale
                        expanded = false
                        onLocaleChanged(locale)
                    }
                )
            }
        }
    }
}
