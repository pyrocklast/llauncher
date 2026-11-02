package app.olauncher.helper

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.text.format.DateFormat
import android.util.TypedValue
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Lite: text sizes and the home screen date.
 */

/**
 * Scales this view's text by [scale], on top of the size its layout gives it (which already
 * follows Android's font size). Call it once per view, right after the view is created.
 */
fun TextView.scaleTextSize(scale: Float) {
    if (scale != 1f) setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * scale)
}

/**
 * The language for the date: the one chosen for this app in Android's settings (Android 13+),
 * or the phone's language when none is chosen.
 */
fun Context.dateLocale(): Locale {
    val appLocales = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        getSystemService(LocaleManager::class.java)?.applicationLocales
    else null
    return if (appLocales != null && !appLocales.isEmpty) appLocales[0]
    else Resources.getSystem().configuration.locales[0]
}

/**
 * Today's date in that language's usual format: short ("lun. 2 nov.", "Mon 2 Nov") or
 * [long] ("lundi 2 novembre", "Monday 2 November").
 */
fun Context.formatHomeDate(long: Boolean, date: Date = Date()): String {
    val locale = dateLocale()
    val pattern = DateFormat.getBestDateTimePattern(locale, if (long) "EEEEdMMMM" else "EEEdMMM")
    return SimpleDateFormat(pattern, locale).format(date)
}
