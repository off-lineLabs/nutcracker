package com.offlinelabs.nutcracker.util

import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.app.LocaleManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
import org.xmlpull.v1.XmlPullParser
import androidx.annotation.XmlRes
import android.content.res.XmlResourceParser
import android.net.Uri
import android.provider.Settings
import android.content.ActivityNotFoundException
import android.content.Intent

object LocaleUtils {
    /**
     * Returns the list of supported locales for the app.
     * On Android 13+ uses PackageManager, otherwise parses locales_config.xml.
     */
    fun getSupportedLocales(context: Context): List<Locale> = parseLocalesConfig(context, com.offlinelabs.nutcracker.R.xml.locales_config)

    private fun parseLocalesConfig(context: Context, @XmlRes xmlRes: Int): List<Locale> {
        val locales = mutableListOf<Locale>()
        val parser: XmlResourceParser = context.resources.getXml(xmlRes)
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                    val tag = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                    if (!tag.isNullOrBlank()) {
                        locales += Locale.forLanguageTag(tag)
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // Fallback (should not happen): English only
            if (locales.isEmpty()) locales += Locale.ENGLISH
        } finally {
            parser.close()
        }
        return locales.distinct()
    }

    /**
     * Sets the app locale using LocaleManager (API 33+) or AppCompatDelegate for older versions.
     */
    fun setAppLocale(context: Context, locale: Locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager?.setApplicationLocales(LocaleList(locale))
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale.toLanguageTag()))
        }
    }

    fun launchSystemLocaleSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                    .putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                context.startActivity(intent)
            } else {
                // Fallback: open app details where user can change language (system level not available pre-33)
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
                context.startActivity(intent)
            }
        } catch (_: ActivityNotFoundException) {
            // Secondary fallback: generic settings
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
                context.startActivity(intent)
            } catch (_: Exception) { /* swallow */ }
        }
    }
}
