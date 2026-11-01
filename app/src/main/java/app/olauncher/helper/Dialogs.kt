package app.olauncher.helper

import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.annotation.MenuRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import app.olauncher.data.Prefs

/**
 * Lite: menus and dialogs use the stock Material 3 components, styled by the app theme.
 */

/**
 * Shows a stock popup menu anchored to this view.
 * [configure] can add or tweak items before the menu is shown.
 */
fun View.showPopupMenu(
    @MenuRes menuRes: Int = 0,
    gravity: Int = Gravity.END,
    configure: (Menu) -> Unit = {},
    onItemClick: (MenuItem) -> Unit,
): PopupMenu {
    val popup = PopupMenu(context, this, gravity)
    if (menuRes != 0) popup.menuInflater.inflate(menuRes, popup.menu)
    configure(popup.menu)
    popup.setOnMenuItemClickListener { item ->
        onItemClick(item)
        true
    }
    popup.show()
    return popup
}

/**
 * Shows a stock dialog without bringing back the status bar when the user has hidden it.
 */
fun AlertDialog.showRespectingStatusBar(): AlertDialog {
    val window = window
    if (window == null || Prefs(context).showStatusBar) {
        show()
    } else {
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        show()
        window.hideStatusBar()
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }
    return this
}
