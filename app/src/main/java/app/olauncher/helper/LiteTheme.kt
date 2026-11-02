package app.olauncher.helper

import android.app.Activity
import android.content.Context
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import app.olauncher.R
import app.olauncher.data.Prefs

/*
 * Lite: the launcher's look versus the rest of the app.
 *
 * The home screen and app drawer use the theme and bold text chosen in settings. Everything else
 * (the settings screen, menus, dialogs) is a normal Android screen: it follows the phone's dark
 * mode and font, whatever the launcher is set to.
 */

/** Whether the home screen and app drawer use light text (the Dark theme, or System in dark mode). */
fun Context.isLauncherDark(): Boolean = when (Prefs(this).appTheme) {
    AppCompatDelegate.MODE_NIGHT_YES -> true
    AppCompatDelegate.MODE_NIGHT_NO -> false
    else -> isDarkThemeOn()
}

/** This context with the launcher's text colours and font weight, for the home screen and drawer. */
fun Context.withLauncherTheme(): Context = ContextThemeWrapper(
    this,
    if (isLauncherDark()) R.style.ThemeOverlay_Lite_LauncherDark else R.style.ThemeOverlay_Lite_LauncherLight
).withLauncherFont()

/** This context with the launcher's font weight for app names: light, or regular when bold is on. */
fun Context.withLauncherFont(): Context =
    if (Prefs(this).boldFont) ContextThemeWrapper(this, R.style.BoldFontOverlay) else this

/** Status bar shown or hidden as set, with status and navigation bar icons that match the launcher's text. */
fun Activity.applyLauncherSystemBars() {
    if (Prefs(this).showStatusBar) window.showStatusBar() else window.hideStatusBar()
    setLightSystemBars(!isLauncherDark())
}

/** Status bar always shown, with icons following the phone's dark mode, like any settings screen. */
fun Activity.applySettingsSystemBars() {
    window.showStatusBar()
    setLightSystemBars(!isDarkThemeOn())
}

// Dark icons for a light background, light icons for a dark one. Set after showing or hiding the
// status bar: on Android 10 and older that resets these flags.
private fun Activity.setLightSystemBars(light: Boolean) {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = light
        isAppearanceLightNavigationBars = light
    }
}
