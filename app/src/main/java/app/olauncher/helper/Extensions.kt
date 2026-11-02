package app.olauncher.helper

import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.UserHandle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import app.olauncher.BuildConfig
import app.olauncher.R
import app.olauncher.data.Constants
import java.util.Calendar
import java.util.Locale

fun Window.showStatusBar() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        insetsController?.show(WindowInsets.Type.statusBars())
    else
        @Suppress("DEPRECATION", "InlinedApi")
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
}

fun Window.hideStatusBar() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        insetsController?.hide(WindowInsets.Type.statusBars())
    else
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
}

fun View.hideKeyboard() {
    this.clearFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

// Lite: asks Android to show the keyboard for this field on the next frame. Upstream force-opened
// it (a deprecated call) after a 100 ms timer, and a force-opened keyboard can stay open in the
// app you launch next.
fun View.showKeyboard(show: Boolean = true) {
    if (show.not()) return
    if (!requestFocus()) return
    post {
        val window = context.findActivity()?.window
        if (window != null)
            WindowCompat.getInsetsController(window, this).show(WindowInsetsCompat.Type.ime())
        else
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}


@RequiresApi(Build.VERSION_CODES.Q)
fun Activity.showLauncherSelector(requestCode: Int) {
    val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
    if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        startActivityForResult(intent, requestCode)
    } else
        resetDefaultLauncher()
}

fun Context.resetDefaultLauncher() {
    try {
        val componentName = ComponentName(this, FakeHomeActivity::class.java)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        val selector = Intent(Intent.ACTION_MAIN)
        selector.addCategory(Intent.CATEGORY_HOME)
        startActivity(selector)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Context.isDefaultLauncher(): Boolean {
    val launcherPackageName = getDefaultLauncherPackage(this)
    return BuildConfig.APPLICATION_ID == launcherPackageName
}

fun Context.resetLauncherViaFakeActivity() {
    resetDefaultLauncher()
    if (getDefaultLauncherPackage(this).contains("."))
        startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
}

private var isEinkDevice: Boolean? = null

fun Context.isEinkDisplay(): Boolean {
    isEinkDevice?.let { return it }
    return (hasEinkRefreshRate() || isOnyxDevice() || isKnownEinkModel())
        .also { isEinkDevice = it }
}

private fun Context.hasEinkRefreshRate(): Boolean {
    return try {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        // Check max supported refresh rate, not the current one, because adaptive
        // refresh rate displays can drop to 30Hz or lower without being e-ink
        windowManager.defaultDisplay.supportedModes.maxOf { it.refreshRate } <= Constants.MIN_ANIM_REFRESH_RATE
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// Boox devices report 60Hz+ refresh rates, so the refresh rate check misses them
private fun isOnyxDevice(): Boolean {
    if (Build.MANUFACTURER.equals("ONYX", ignoreCase = true)) return true
    return try {
        // Onyx firmware ships its e-ink SDK classes in the boot classpath
        Class.forName("android.onyx.ViewUpdateHelper")
        true
    } catch (ignored: Throwable) {
        false
    }
}

private fun isKnownEinkModel(): Boolean {
    val brand = Build.BRAND.lowercase()
    val manufacturer = Build.MANUFACTURER.lowercase()
    val einkOnlyBrands = listOf("onyx", "boox", "dasung", "bigme", "boyue", "meebook", "mudita")
    if (einkOnlyBrands.any { brand.contains(it) || manufacturer.contains(it) }) return true
    // Hisense also sells LCD phones, so match only their e-ink line
    if (brand.contains("hisense") || manufacturer.contains("hisense"))
        return Regex("\\bA[579]\\b|TOUCH|HI READER").containsMatchIn(Build.MODEL.uppercase())
    return false
}

fun Context.isSystemAnimationsDisabled(): Boolean {
    return try {
        Settings.Global.getFloat(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1f) == 0f
                || Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f
                || Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Context.isPackageInstalled(packageName: String, userHandle: UserHandle = android.os.Process.myUserHandle()): Boolean {
    val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val activityInfo = launcher.getActivityList(packageName, userHandle)
    return activityInfo.isNotEmpty()
}

fun Context.isCountryIn(): Boolean {
    val country = runCatching {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        telephonyManager?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: telephonyManager?.networkCountryIso?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: Locale.getDefault().country
    return country.equals("IN", ignoreCase = true)
}

fun Long.convertEpochToMidnight(): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

fun Long.isDaySince(): Int = ((System.currentTimeMillis().convertEpochToMidnight() - this.convertEpochToMidnight())
        / Constants.ONE_DAY_IN_MILLIS).toInt()

fun Long.hasBeenDays(days: Int): Boolean =
    ((System.currentTimeMillis() - this) / Constants.ONE_DAY_IN_MILLIS) >= days

fun Long.hasBeenHours(hours: Int): Boolean =
    ((System.currentTimeMillis() - this) / Constants.ONE_HOUR_IN_MILLIS) >= hours

fun Long.hasBeenMinutes(minutes: Int): Boolean =
    ((System.currentTimeMillis() - this) / Constants.ONE_MINUTE_IN_MILLIS) >= minutes

fun Int.dpToPx(): Int {
    return (this * Resources.getSystem().displayMetrics.density).toInt()
}
