package app.olauncher.ui

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreferenceCompat
import app.olauncher.BuildConfig
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.helper.dpToPx
import app.olauncher.helper.hideStatusBar
import app.olauncher.helper.isEinkDisplay
import app.olauncher.helper.isSystemAnimationsDisabled
import app.olauncher.helper.isTablet
import app.olauncher.helper.openAppInfo
import app.olauncher.helper.showRespectingStatusBar
import app.olauncher.helper.showStatusBar
import app.olauncher.helper.showToast
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Lite: settings as a stock Android preference screen.
 *
 * Values still live in [Prefs], exactly as upstream stores them. The preferences here are not
 * persisted themselves; they show the current value and write changes back through [Prefs].
 */
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private var dialog: AlertDialog? = null

    private lateinit var defaultLauncher: Preference
    private lateinit var homeAppsNum: Preference
    private lateinit var dateTime: Preference
    private lateinit var homeAlignment: Preference
    private lateinit var bottomAlignment: SwitchPreferenceCompat
    private lateinit var drawerAlignment: Preference
    private lateinit var appTheme: Preference
    private lateinit var textSize: Preference
    private lateinit var swipeLeftApp: Preference
    private lateinit var swipeRightApp: Preference

    private val alignments = intArrayOf(Gravity.START, Gravity.CENTER, Gravity.END)
    private val dateTimeOptions = intArrayOf(Constants.DateTime.ON, Constants.DateTime.DATE_ONLY, Constants.DateTime.OFF)
    private val themeOptions = intArrayOf(
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        prefs = Prefs(requireContext())
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        val screen = preferenceManager.createPreferenceScreen(preferenceManager.context)

        screen.category(R.string.app_name).apply {
            defaultLauncher = item(R.string.set_as_default_launcher) { viewModel.resetLauncherLiveData.call() }
            item(R.string.hidden_apps) { showHiddenApps() }
            item(R.string.lite_app_info) {
                openAppInfo(requireContext(), Process.myUserHandle(), BuildConfig.APPLICATION_ID)
            }
        }

        screen.category(R.string.home_screen).apply {
            homeAppsNum = item(R.string.apps_on_home_screen) { showHomeAppsNumDialog() }
            dateTime = item(R.string.show_date_time) { showDateTimeDialog() }
            homeAlignment = item(R.string.home_layout_alignment) { showHomeAlignmentDialog() }
            bottomAlignment = switch(R.string.lite_align_bottom, prefs.homeBottomAlignment) { checked ->
                if (viewModel.isOlauncherDefault.value != true) {
                    requireContext().showToast(
                        getString(R.string.please_set_olauncher_as_default_first),
                        Toast.LENGTH_LONG
                    )
                    false
                } else {
                    prefs.homeBottomAlignment = checked
                    viewModel.updateHomeAlignment(prefs.homeAlignment)
                    true
                }
            }
            drawerAlignment = item(R.string.lite_drawer_alignment) { showDrawerAlignmentDialog() }
        }

        screen.category(R.string.appearance).apply {
            appTheme = item(R.string.theme_mode) { showThemeDialog() }
            textSize = item(R.string.text_size) { showTextSizeDialog() }
            switch(R.string.bold_font, prefs.boldFont) { checked ->
                prefs.boldFont = checked
                requireActivity().recreate()
                true
            }
            switch(R.string.notification_bar, prefs.showStatusBar) { checked ->
                prefs.showStatusBar = checked
                if (checked) requireActivity().window.showStatusBar()
                else requireActivity().window.hideStatusBar()
                true
            }
            switch(R.string.auto_show_keyboard, prefs.autoShowKeyboard) { checked ->
                prefs.autoShowKeyboard = checked
                true
            }
        }

        screen.category(R.string.gestures).apply {
            switch(R.string.lite_swipe_left, prefs.swipeLeftEnabled) { checked ->
                prefs.swipeLeftEnabled = checked
                swipeLeftApp.isEnabled = checked
                true
            }
            swipeLeftApp = item(R.string.swipe_left_app) { pickApp(Constants.FLAG_SET_SWIPE_LEFT_APP) }
            swipeLeftApp.isEnabled = prefs.swipeLeftEnabled

            switch(R.string.lite_swipe_right, prefs.swipeRightEnabled) { checked ->
                prefs.swipeRightEnabled = checked
                swipeRightApp.isEnabled = checked
                true
            }
            swipeRightApp = item(R.string.swipe_right_app) { pickApp(Constants.FLAG_SET_SWIPE_RIGHT_APP) }
            swipeRightApp.isEnabled = prefs.swipeRightEnabled
        }

        preferenceScreen = screen
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // The launcher window is transparent so the wallpaper shows through; settings need a background
        view.setBackgroundColor(MaterialColors.getColor(view, android.R.attr.colorBackground))
        // The window draws behind the system bars, so keep the list clear of them
        listView.setPadding(0, 64.dpToPx(), 0, 64.dpToPx())
        listView.clipToPadding = false

        // Upstream uses this flag to hide the first-run tips on the home screen
        prefs.firstSettingsOpen = false
        viewModel.isOlauncherDefault()

        viewModel.isOlauncherDefault.observe(viewLifecycleOwner) { refreshSummaries() }
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) { refreshSummaries() }
        viewModel.updateSwipeApps.observe(viewLifecycleOwner) { refreshSummaries() }
        refreshSummaries()
    }

    override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int): Animator? {
        // Same as BaseFragment: no transitions on e-ink or with system animations off
        if (nextAnim != 0 && (requireContext().isSystemAnimationsDisabled() || requireContext().isEinkDisplay()))
            return ValueAnimator.ofFloat(0f, 1f).setDuration(0)
        return null
    }

    override fun onDestroyView() {
        dialog?.dismiss()
        dialog = null
        super.onDestroyView()
    }

    private fun refreshSummaries() {
        defaultLauncher.setTitle(
            if (viewModel.isOlauncherDefault.value == true) R.string.change_default_launcher
            else R.string.set_as_default_launcher
        )
        homeAppsNum.summary = prefs.homeAppsNum.toString()
        dateTime.summary = getString(dateTimeLabel(prefs.dateTimeVisibility))
        homeAlignment.summary = getString(alignmentLabel(prefs.homeAlignment))
        bottomAlignment.isChecked = prefs.homeBottomAlignment
        drawerAlignment.summary = getString(alignmentLabel(prefs.appLabelAlignment))
        appTheme.summary = getString(themeLabel(prefs.appTheme))
        textSize.summary = formatScale(prefs.textSizeScale)
        swipeLeftApp.summary = prefs.appNameSwipeLeft
        swipeRightApp.summary = prefs.appNameSwipeRight
    }

    // Choice dialogs

    private fun showHomeAppsNumDialog() {
        val labels = Array<CharSequence>(9) { it.toString() }
        showChoiceDialog(R.string.apps_on_home_screen, labels, prefs.homeAppsNum) { which ->
            prefs.homeAppsNum = which
            viewModel.refreshHome(true)
            refreshSummaries()
        }
    }

    private fun showDateTimeDialog() {
        val labels = labelsFor(dateTimeOptions) { dateTimeLabel(it) }
        showChoiceDialog(R.string.show_date_time, labels, dateTimeOptions.indexOf(prefs.dateTimeVisibility)) { which ->
            prefs.dateTimeVisibility = dateTimeOptions[which]
            viewModel.toggleDateTime()
            refreshSummaries()
        }
    }

    private fun showHomeAlignmentDialog() {
        val labels = labelsFor(alignments) { alignmentLabel(it) }
        showChoiceDialog(R.string.home_layout_alignment, labels, alignments.indexOf(prefs.homeAlignment)) { which ->
            viewModel.updateHomeAlignment(alignments[which])
            refreshSummaries()
        }
    }

    private fun showDrawerAlignmentDialog() {
        val labels = labelsFor(alignments) { alignmentLabel(it) }
        showChoiceDialog(R.string.lite_drawer_alignment, labels, alignments.indexOf(prefs.appLabelAlignment)) { which ->
            prefs.appLabelAlignment = alignments[which]
            refreshSummaries()
        }
    }

    private fun showThemeDialog() {
        val labels = labelsFor(themeOptions) { themeLabel(it) }
        showChoiceDialog(R.string.theme_mode, labels, themeOptions.indexOf(prefs.appTheme)) { which ->
            val theme = themeOptions[which]
            prefs.appTheme = theme
            refreshSummaries()
            if (AppCompatDelegate.getDefaultNightMode() != theme)
                requireActivity().recreate()
        }
    }

    private fun showTextSizeDialog() {
        val maxScale = if (isTablet(requireContext())) 2.0f else 1.5f
        val steps = ((maxScale - 0.5f) * 10).roundToInt()
        val scales = FloatArray(steps + 1) { (5 + it) / 10f }
        val labels = Array<CharSequence>(scales.size) { formatScale(scales[it]) }
        val current = scales.indexOfFirst { abs(it - prefs.textSizeScale) < 0.01f }
        showChoiceDialog(R.string.text_size, labels, current) { which ->
            if (abs(scales[which] - prefs.textSizeScale) >= 0.01f) {
                prefs.textSizeScale = scales[which]
                // MainActivity applies the scale as the font scale when it is recreated
                requireActivity().recreate()
            }
        }
    }

    private fun showChoiceDialog(
        @StringRes title: Int,
        labels: Array<CharSequence>,
        checked: Int,
        onChoice: (Int) -> Unit,
    ) {
        dialog?.dismiss()
        dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(labels, checked) { choiceDialog, which ->
                choiceDialog.dismiss()
                onChoice(which)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .showRespectingStatusBar()
    }

    // Navigation

    private fun showHiddenApps() {
        if (prefs.hiddenApps.isEmpty()) {
            requireContext().showToast(getString(R.string.no_hidden_apps))
            return
        }
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf(Constants.Key.FLAG to Constants.FLAG_HIDDEN_APPS)
        )
    }

    private fun pickApp(flag: Int) {
        viewModel.getAppList(true)
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf(Constants.Key.FLAG to flag)
        )
    }

    // Labels

    private fun labelsFor(values: IntArray, label: (Int) -> Int): Array<CharSequence> =
        Array(values.size) { getString(label(values[it])) }

    @StringRes
    private fun alignmentLabel(gravity: Int): Int = when (gravity) {
        Gravity.CENTER -> R.string.center
        Gravity.END -> R.string.right
        else -> R.string.left
    }

    @StringRes
    private fun dateTimeLabel(visibility: Int): Int = when (visibility) {
        Constants.DateTime.ON -> R.string.on
        Constants.DateTime.DATE_ONLY -> R.string.date_only
        else -> R.string.off
    }

    @StringRes
    private fun themeLabel(theme: Int): Int = when (theme) {
        AppCompatDelegate.MODE_NIGHT_YES -> R.string.dark
        AppCompatDelegate.MODE_NIGHT_NO -> R.string.light
        else -> R.string.system_default
    }

    private fun formatScale(scale: Float): String = String.format("%.1f", scale)

    // Preference builders. Every preference is non-persistent: Prefs stays the single source of truth.

    private fun PreferenceGroup.category(@StringRes title: Int): PreferenceCategory {
        val category = PreferenceCategory(context)
        category.setTitle(title)
        category.isIconSpaceReserved = false
        addPreference(category)
        return category
    }

    private fun PreferenceGroup.item(@StringRes title: Int, onClick: () -> Unit): Preference {
        val preference = Preference(context)
        preference.setTitle(title)
        preference.isIconSpaceReserved = false
        preference.isPersistent = false
        preference.setOnPreferenceClickListener {
            onClick()
            true
        }
        addPreference(preference)
        return preference
    }

    private fun PreferenceGroup.switch(
        @StringRes title: Int,
        checked: Boolean,
        onChange: (Boolean) -> Boolean,
    ): SwitchPreferenceCompat {
        val preference = SwitchPreferenceCompat(context)
        preference.setTitle(title)
        preference.isIconSpaceReserved = false
        preference.isPersistent = false
        preference.widgetLayoutResource = R.layout.preference_material_switch
        preference.isChecked = checked
        preference.setOnPreferenceChangeListener { _, newValue -> onChange(newValue as Boolean) }
        addPreference(preference)
        return preference
    }
}
