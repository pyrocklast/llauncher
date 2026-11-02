package app.olauncher.ui

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import app.olauncher.helper.applyLauncherSystemBars
import app.olauncher.helper.isEinkDisplay
import app.olauncher.helper.isSystemAnimationsDisabled
import app.olauncher.helper.withLauncherTheme

open class BaseFragment : Fragment() {

    // Lite: the home screen and app drawer are drawn with the launcher's theme and bold setting.
    // The rest of the app follows the phone's dark mode, so changing these needs no reload.
    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater =
        super.onGetLayoutInflater(savedInstanceState).cloneInContext(requireContext().withLauncherTheme())

    // Lite: the status bar as set for the launcher (settings always shows it), with matching icons
    override fun onResume() {
        super.onResume()
        requireActivity().applyLauncherSystemBars()
    }

    override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int): Animator? {
        if (nextAnim != 0 && (requireContext().isSystemAnimationsDisabled() || requireContext().isEinkDisplay()))
            return ValueAnimator.ofFloat(0f, 1f).setDuration(0)
        return null
    }
}
