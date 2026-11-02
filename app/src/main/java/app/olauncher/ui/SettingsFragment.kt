package app.olauncher.ui

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Process
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
import app.olauncher.databinding.DialogLiteTextSizeBinding
import app.olauncher.databinding.FragmentLiteSettingsBinding
import app.olauncher.helper.dpToPx
import app.olauncher.helper.hideStatusBar
import app.olauncher.helper.isEinkDisplay
import app.olauncher.helper.isSystemAnimationsDisabled
import app.olauncher.helper.isTablet
import app.olauncher.helper.openAppInfo
import app.olauncher.helper.showRespectingStatusBar
import app.olauncher.helper.showStatusBar
import app.olauncher.helper.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Lite: settings as a stock Android preference screen, under a large title bar.
 *
 * Values still live in [Prefs], exactly as upstream stores them. The preferences here are not
 * persisted themselves; they show the current value and write changes back through [Prefs].
 */
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private var dialog: AlertDialog? = null
    private var frame: FragmentLiteSettingsBinding? = null

    private lateinit var defaultLauncher: Preference
    private lateinit var hiddenApps: Preference
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

        screen.category(R.string.lite_general).apply {
            defaultLauncher = item(R.string.set_as_default_launcher) { viewModel.resetLauncherLiveData.call() }
            hiddenApps = item(R.string.hidden_apps) { showHiddenApps() }
            item(R.string.lite_app_info) {
                openAppInfo(requireContext(), Process.myUserHandle(), BuildConfig.APPLICATION_ID)
            }.summary = getString(R.string.lite_version, BuildConfig.VERSION_NAME)
        }

        screen.category(R.string.home_screen).apply {
            homeAppsNum = item(R.string.apps_on_home_screen) { showHomeAppsNumDialog() }
            dateTime = item(R.string.lite_date_and_time) { showDateTimeDialog() }
            homeAlignment = item(R.string.lite_alignment) { showHomeAlignmentDialog() }
            bottomAlignment = switch(R.string.lite_align_bottom, prefs.homeBottomAlignment) { checked ->
                if (viewModel.isOlauncherDefault.value != true) {
                    requireContext().showToast(
                        getString(R.string.lite_set_as_home_first, getString(R.string.app_name)),
                        Toast.LENGTH_LONG
                    )
                    false
                } else {
                    prefs.homeBottomAlignment = checked
                    viewModel.updateHomeAlignment(prefs.homeAlignment)
                    true
                }
            }
            switch(R.string.lite_show_status_bar, prefs.showStatusBar) { checked ->
                prefs.showStatusBar = checked
                if (checked) requireActivity().window.showStatusBar()
                else requireActivity().window.hideStatusBar()
                true
            }
        }

        screen.category(R.string.lite_app_drawer).apply {
            drawerAlignment = item(R.string.lite_alignment) { showDrawerAlignmentDialog() }
            switch(R.string.auto_show_keyboard, prefs.autoShowKeyboard) { checked ->
                prefs.autoShowKeyboard = checked
                true
            }
        }

        screen.category(R.string.appearance).apply {
            appTheme = item(R.string.lite_theme) { showThemeDialog() }
            textSize = item(R.string.text_size) { showTextSizeDialog() }
            switch(R.string.bold_font, prefs.boldFont) { checked ->
                prefs.boldFont = checked
                requireActivity().recreate()
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

    // Puts the preference list under the title bar from fragment_lite_settings.xml
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val list = super.onCreateView(inflater, container, savedInstanceState)
        val binding = FragmentLiteSettingsBinding.inflate(inflater, container, false)
        binding.settingsContent.addView(
            list,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        frame = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        frame?.let { frame ->
            // Names the app, since an accidental long press on the home screen can land you here
            frame.collapsingToolbar.title = getString(R.string.lite_settings_title, getString(R.string.app_name))
            frame.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
            listView.clipToPadding = false
            keepClearOfSystemBars(frame)
        }

        // Upstream uses this flag to hide the first-run tips on the home screen
        prefs.firstSettingsOpen = false
        viewModel.isOlauncherDefault()
        viewModel.getHiddenApps()

        viewModel.isOlauncherDefault.observe(viewLifecycleOwner) { refreshSummaries() }
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) { refreshSummaries() }
        viewModel.updateSwipeApps.observe(viewLifecycleOwner) { refreshSummaries() }
        viewModel.hiddenApps.observe(viewLifecycleOwner) { refreshSummaries() }
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
        frame = null
        super.onDestroyView()
    }

    // The launcher window draws behind the status and navigation bars, so keep the title bar and the
    // last rows clear of them. Falls back to fixed spacing if the system reports no insets.
    private fun keepClearOfSystemBars(frame: FragmentLiteSettingsBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(frame.root) { _, insets ->
            val top = maxOf(
                insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()).top,
                // Leave room for the status bar even while it's hidden, so nothing jumps when it shows
                insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars()).top,
            )
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            frame.appBar.updatePadding(top = if (top > 0) top else 32.dpToPx())
            listView.updatePadding(bottom = if (bottom > 0) bottom + 16.dpToPx() else 64.dpToPx())
            insets
        }
        ViewCompat.requestApplyInsets(frame.root)
    }

    private fun refreshSummaries() {
        defaultLauncher.setTitle(
            if (viewModel.isOlauncherDefault.value == true) R.string.change_default_launcher
            else R.string.set_as_default_launcher
        )
        hiddenApps.summary = viewModel.hiddenApps.value?.size?.let { count ->
            if (count == 0) getString(R.string.lite_none)
            else resources.getQuantityString(R.plurals.lite_app_count, count, count)
        }
        homeAppsNum.summary = prefs.homeAppsNum.toString()
        dateTime.summary = getString(dateTimeLabel(prefs.dateTimeVisibility))
        homeAlignment.summary = getString(alignmentLabel(prefs.homeAlignment))
        bottomAlignment.isChecked = prefs.homeBottomAlignment
        drawerAlignment.summary = getString(alignmentLabel(prefs.appLabelAlignment))
        appTheme.summary = getString(themeLabel(prefs.appTheme))
        textSize.summary = formatPercent(prefs.textSizeScale)
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
        showChoiceDialog(R.string.lite_date_and_time, labels, dateTimeOptions.indexOf(prefs.dateTimeVisibility)) { which ->
            prefs.dateTimeVisibility = dateTimeOptions[which]
            viewModel.toggleDateTime()
            refreshSummaries()
        }
    }

    private fun showHomeAlignmentDialog() {
        val labels = labelsFor(alignments) { alignmentLabel(it) }
        showChoiceDialog(R.string.lite_home_alignment, labels, alignments.indexOf(prefs.homeAlignment)) { which ->
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
        showChoiceDialog(R.string.lite_theme, labels, themeOptions.indexOf(prefs.appTheme)) { which ->
            val theme = themeOptions[which]
            prefs.appTheme = theme
            refreshSummaries()
            if (AppCompatDelegate.getDefaultNightMode() != theme)
                requireActivity().recreate()
        }
    }

    // Lite: a slider in percent of Android's own font size, with a preview of your home apps.
    // MainActivity multiplies Android's font scale by this when it (re)creates.
    private fun showTextSizeDialog() {
        val sizeBinding = DialogLiteTextSizeBinding.inflate(layoutInflater)
        val preview = sizeBinding.textSizePreview
        val slider = sizeBinding.textSizeSlider
        val maxPercent = if (isTablet(requireContext())) 200 else 150

        preview.text = previewText()
        // The preview starts at the current size; this is its size at 100%
        val previewPxAt100 = preview.textSize / prefs.textSizeScale

        slider.valueTo = maxPercent.toFloat()
        slider.value = ((prefs.textSizeScale * 10).roundToInt() * 10).coerceIn(50, maxPercent).toFloat()
        slider.setLabelFormatter { value -> formatPercent(value / 100f) }
        slider.addOnChangeListener { _, value, _ ->
            preview.setTextSize(TypedValue.COMPLEX_UNIT_PX, previewPxAt100 * value / 100f)
        }
        preview.setTextSize(TypedValue.COMPLEX_UNIT_PX, previewPxAt100 * slider.value / 100f)

        dialog?.dismiss()
        dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.text_size)
            .setView(sizeBinding.root)
            .setPositiveButton(R.string.okay) { _, _ ->
                val scale = slider.value / 100f
                if (abs(scale - prefs.textSizeScale) >= 0.01f) {
                    prefs.textSizeScale = scale
                    requireActivity().recreate()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .showRespectingStatusBar()
    }

    // Up to three of your home apps, or the app's name when none are set
    private fun previewText(): String {
        val names = (1..prefs.homeAppsNum.coerceAtMost(8))
            .map { prefs.getAppName(it) }
            .filter { it.isNotBlank() }
            .take(3)
        return names.ifEmpty { listOf(getString(R.string.app_name)) }.joinToString("\n")
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
        val count = viewModel.hiddenApps.value?.size ?: prefs.hiddenApps.size
        if (count == 0) {
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
        Constants.DateTime.ON -> R.string.lite_time_and_date
        Constants.DateTime.DATE_ONLY -> R.string.date_only
        else -> R.string.off
    }

    @StringRes
    private fun themeLabel(theme: Int): Int = when (theme) {
        AppCompatDelegate.MODE_NIGHT_YES -> R.string.dark
        AppCompatDelegate.MODE_NIGHT_NO -> R.string.light
        else -> R.string.system_default
    }

    // e.g. "110%", in your language's format
    private fun formatPercent(scale: Float): String = NumberFormat.getPercentInstance().format(scale.toDouble())

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
