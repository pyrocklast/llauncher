package app.olauncher.data

import android.content.Context
import androidx.core.content.edit

/**
 * Lite: settings this fork adds. They live in the same preferences file as upstream's [Prefs],
 * but are kept in this separate class so upstream's Prefs.kt merges without conflicts.
 *
 * The app name size itself is upstream's [Prefs.textSizeScale].
 */
class LitePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("app.olauncher", 0)

    /** Full date ("lundi 2 novembre") instead of the short one ("lun. 2 nov."). */
    var longDate: Boolean
        get() = prefs.getBoolean(LONG_DATE, false)
        set(value) = prefs.edit { putBoolean(LONG_DATE, value) }

    /** Size of the clock and date on the home screen, relative to Android's font size. */
    var clockScale: Float
        get() = prefs.getFloat(CLOCK_SCALE, 1.0f)
        set(value) = prefs.edit { putFloat(CLOCK_SCALE, value) }

    private companion object {
        const val LONG_DATE = "LITE_LONG_DATE"
        const val CLOCK_SCALE = "LITE_CLOCK_SCALE"
    }
}
