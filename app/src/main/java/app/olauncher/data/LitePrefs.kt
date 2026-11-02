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

    /**
     * How the home screen shows the date: [DATE_SHORT] ("lun. 2 nov."), [DATE_FULL]
     * ("lundi 2 novembre") or [DATE_NUMERIC] ("lun. 02/11"). Earlier versions only had short or
     * full, saved as a yes/no; that choice carries over.
     */
    var dateStyle: Int
        get() = prefs.getInt(DATE_STYLE, if (prefs.getBoolean(LONG_DATE, false)) DATE_FULL else DATE_SHORT)
        set(value) = prefs.edit { putInt(DATE_STYLE, value) }

    /** Size of the clock and date on the home screen, relative to Android's font size. */
    var clockScale: Float
        get() = prefs.getFloat(CLOCK_SCALE, 1.0f)
        set(value) = prefs.edit { putFloat(CLOCK_SCALE, value) }

    companion object {
        const val DATE_SHORT = 0
        const val DATE_FULL = 1
        const val DATE_NUMERIC = 2

        private const val LONG_DATE = "LITE_LONG_DATE"
        private const val DATE_STYLE = "LITE_DATE_STYLE"
        private const val CLOCK_SCALE = "LITE_CLOCK_SCALE"
    }
}
