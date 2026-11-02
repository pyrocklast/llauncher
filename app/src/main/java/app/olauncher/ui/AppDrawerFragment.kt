package app.olauncher.ui

import android.content.Context
import android.content.DialogInterface
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.Spannable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.AppModel
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.databinding.DialogRenameBinding
import app.olauncher.databinding.FragmentAppDrawerBinding
import app.olauncher.helper.deletePinnedShortcut
import app.olauncher.helper.hideKeyboard
import app.olauncher.helper.isEinkDisplay
import app.olauncher.helper.isSystemAnimationsDisabled
import app.olauncher.helper.isSystemApp
import app.olauncher.helper.openAppInfo
import app.olauncher.helper.showKeyboard
import app.olauncher.helper.showPopupMenu
import app.olauncher.helper.showToast
import app.olauncher.helper.uninstall
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AppDrawerFragment : BaseFragment() {

    private lateinit var prefs: Prefs
    private lateinit var adapter: AppDrawerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var searchTextView: TextView? = null
    private var cachedIsCjkKeyboard: Boolean? = null

    private var flag = Constants.FLAG_LAUNCH_APP
    private var canRename = false
    private var currentAppList: List<AppModel>? = null
    private var currentPrivateSpaceApps: List<AppModel>? = null
    private var currentPrivateSpaceLocked: Boolean = true
    private var currentPrivateSpaceAvailable: Boolean = false

    private val viewModel: MainViewModel by activityViewModels()
    private var dialog: AlertDialog? = null
    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        arguments?.let {
            flag = it.getInt(Constants.Key.FLAG, Constants.FLAG_LAUNCH_APP)
            canRename = it.getBoolean(Constants.Key.RENAME, false)
        }

        initViews()
        initSearch()
        initAdapter()
        initObservers()
        initClickListeners()
    }

    private fun initViews() {
        if (flag == Constants.FLAG_HIDDEN_APPS)
            binding.search.queryHint = getString(R.string.hidden_apps)
        else if (flag in Constants.FLAG_SET_HOME_APP_1..Constants.FLAG_SET_CALENDAR_APP)
            binding.search.queryHint = getString(R.string.lite_choose_app)
        try {
            searchTextView = binding.search.findViewById(R.id.search_src_text)
            searchTextView?.gravity = prefs.appLabelAlignment
            searchTextView?.let { describeSearchForTalkBack(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Lite: the search box's visible hint is just underscores. TalkBack reads "Search apps" instead,
    // while real hints like "Hidden apps" are read as they are.
    private fun describeSearchForTalkBack(searchText: TextView) {
        val spokenHint = getString(R.string.lite_search_apps)
        ViewCompat.setAccessibilityDelegate(searchText, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                val visibleHint = (host as? TextView)?.hint?.toString().orEmpty()
                if (visibleHint.isBlank() || visibleHint.trim().all { it == '_' })
                    info.hintText = spokenHint
            }
        })
    }

    private fun initSearch() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // Lite: no web search. Enter launches the first match, or does nothing
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapter.launchFirstInList()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                try {
                    adapter.allowAutoLaunch = !isSearchComposing()
                    adapter.filter.filter(newText)
                    binding.appRename.visibility =
                        if (canRename && newText.isNotBlank()) View.VISIBLE else View.GONE
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
        })
    }

    private fun isSearchComposing(): Boolean {
        val text = searchTextView?.text
        if (text !is Spannable) return false
        val start = BaseInputConnection.getComposingSpanStart(text)
        val end = BaseInputConnection.getComposingSpanEnd(text)
        if (start !in 0 until end) return false
        return isCjkKeyboard()
    }

    private fun isCjkKeyboard(): Boolean {
        cachedIsCjkKeyboard?.let { return it }
        val result = try {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val subtype = imm.currentInputMethodSubtype
            val language = when {
                subtype == null -> ""
                subtype.languageTag.isNotEmpty() -> subtype.languageTag // e.g. "zh-CN", "ja-JP", "en-US"
                else -> subtype.locale // deprecated fallback, e.g. "zh_CN"
            }
            language.startsWith("zh") || language.startsWith("ja") || language.startsWith("ko")
        } catch (e: Exception) {
            false
        }
        cachedIsCjkKeyboard = result
        return result
    }

    private fun initAdapter() {
        adapter = AppDrawerAdapter(
            flag,
            prefs.appLabelAlignment,
            appClickListener = { appModel ->
                viewModel.selectedApp(appModel, flag)
                if (flag == Constants.FLAG_LAUNCH_APP || flag == Constants.FLAG_HIDDEN_APPS)
                    findNavController().popBackStack(R.id.mainFragment, false)
                else
                    closeDrawer()
            },
            appMenuListener = { appModel, anchor -> showAppMenu(appModel, anchor) },
            privateSpaceToggleListener = {
                viewModel.togglePrivateSpaceLock()
            },
            privateSpaceSettingsListener = {
                viewModel.openPrivateSpaceSettings()
                findNavController().popBackStack(R.id.mainFragment, false)
            }
        )

        linearLayoutManager = object : LinearLayoutManager(requireContext()) {
            override fun scrollVerticallyBy(
                dx: Int,
                recycler: Recycler,
                state: RecyclerView.State,
            ): Int {
                val scrollRange = super.scrollVerticallyBy(dx, recycler, state)
                val overScroll = dx - scrollRange
                if (overScroll < -10 && binding.recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING)
                    closeDrawer()
                return scrollRange
            }
        }

        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(getRecyclerViewOnScrollListener())
        binding.recyclerView.itemAnimator = null
        if (requireContext().isEinkDisplay())
            binding.recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        else if (requireContext().isSystemAnimationsDisabled().not())
            binding.recyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
    }

    private fun initObservers() {
        viewModel.firstOpen.observe(viewLifecycleOwner) {
        }
        if (flag == Constants.FLAG_HIDDEN_APPS) {
            viewModel.hiddenApps.observe(viewLifecycleOwner) {
                it?.let {
                    adapter.setAppList(it.toMutableList())
                }
            }
        } else {
            viewModel.appList.observe(viewLifecycleOwner) {
                currentAppList = it
                updateCombinedAppList()
            }
            if (flag == Constants.FLAG_LAUNCH_APP) {
                viewModel.privateSpaceAvailable.observe(viewLifecycleOwner) {
                    currentPrivateSpaceAvailable = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceLocked.observe(viewLifecycleOwner) {
                    currentPrivateSpaceLocked = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceApps.observe(viewLifecycleOwner) {
                    currentPrivateSpaceApps = it
                    updateCombinedAppList()
                }
            }
        }
    }

    private fun updateCombinedAppList() {
        val apps = currentAppList ?: return
        val combined = apps.toMutableList()

        if (flag == Constants.FLAG_LAUNCH_APP && currentPrivateSpaceAvailable) {
            combined.add(AppModel.PrivateSpaceHeader(isLocked = currentPrivateSpaceLocked))
            if (!currentPrivateSpaceLocked) {
                currentPrivateSpaceApps?.let { combined.addAll(it) }
            }
        }

        adapter.setAppList(combined)
        adapter.filter.filter(binding.search.query)
    }

    private fun initClickListeners() {
        binding.appRename.setOnClickListener {
            val name = binding.search.query.toString().trim()
            if (name.isEmpty()) {
                requireContext().showToast(getString(R.string.type_a_new_app_name_first))
                binding.search.showKeyboard()
                return@setOnClickListener
            }

            when (flag) {
                Constants.FLAG_SET_HOME_APP_1 -> prefs.appName1 = name
                Constants.FLAG_SET_HOME_APP_2 -> prefs.appName2 = name
                Constants.FLAG_SET_HOME_APP_3 -> prefs.appName3 = name
                Constants.FLAG_SET_HOME_APP_4 -> prefs.appName4 = name
                Constants.FLAG_SET_HOME_APP_5 -> prefs.appName5 = name
                Constants.FLAG_SET_HOME_APP_6 -> prefs.appName6 = name
                Constants.FLAG_SET_HOME_APP_7 -> prefs.appName7 = name
                Constants.FLAG_SET_HOME_APP_8 -> prefs.appName8 = name
            }
            closeDrawer()
        }
    }

    // Lite: long-press menu for an app or shortcut in the drawer

    private fun showAppMenu(appModel: AppModel, anchor: View) {
        if (appModel is AppModel.PrivateSpaceHeader) return
        val isShortcut = appModel is AppModel.PinnedShortcut
        val isHiddenList = flag == Constants.FLAG_HIDDEN_APPS
        anchor.showPopupMenu(
            gravity = if (prefs.appLabelAlignment == Gravity.END) Gravity.END else Gravity.START,
            configure = { menu ->
                if (!isHiddenList) menu.add(Menu.NONE, MENU_RENAME, 0, R.string.rename)
                if (!isShortcut)
                    menu.add(Menu.NONE, MENU_HIDE, 1, if (isHiddenList) R.string.adapter_show else R.string.adapter_hide)
                menu.add(Menu.NONE, MENU_INFO, 2, R.string.lite_app_info)
                menu.add(Menu.NONE, MENU_DELETE, 3, if (isShortcut) R.string.remove else R.string.delete)
            }
        ) { item ->
            when (item.itemId) {
                MENU_RENAME -> showRenameDialog(appModel)
                MENU_HIDE -> toggleHidden(appModel)
                MENU_INFO -> openInfo(appModel)
                MENU_DELETE -> deleteApp(appModel)
            }
        }
    }

    private fun showRenameDialog(appModel: AppModel) {
        val renameBinding = DialogRenameBinding.inflate(layoutInflater)
        if (appModel is AppModel.App)
            renameBinding.renameInputLayout.hint = getOriginalAppName(appModel)
        renameBinding.renameInput.setText(appModel.appLabel)
        renameBinding.renameInput.setSelectAllOnFocus(true)

        dialog?.dismiss()
        val renameDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rename)
            .setView(renameBinding.root)
            // An empty name restores the original one
            .setPositiveButton(R.string.rename) { _, _ ->
                renameApp(appModel, renameBinding.renameInput.text?.toString()?.trim().orEmpty())
            }
            .setNeutralButton(R.string.lite_original_name) { _, _ -> renameApp(appModel, "") }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        renameBinding.renameInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_DONE) return@setOnEditorActionListener false
            renameDialog.getButton(DialogInterface.BUTTON_POSITIVE)?.performClick()
            true
        }
        // Plain show() here, so the dialog takes focus and the keyboard opens for typing
        renameDialog.show()
        renameBinding.renameInput.showKeyboard()
        dialog = renameDialog
    }

    private fun renameApp(appModel: AppModel, renameLabel: String) {
        when (appModel) {
            is AppModel.PinnedShortcut -> {
                prefs.setAppRenameLabel(appModel.identity, renameLabel)
                // Older versions saved shortcut names under the bare shortcut id
                if (renameLabel.isEmpty()) prefs.setAppRenameLabel(appModel.shortcutId, "")
            }

            is AppModel.App -> prefs.setAppRenameLabel(appModel.appPackage, renameLabel)
            else -> return
        }
        viewModel.refreshAppList()
    }

    private fun toggleHidden(appModel: AppModel) {
        if (appModel !is AppModel.App) return
        // Look the row up now: the list may have refreshed since the menu opened
        val position = adapter.appFilteredList.indexOf(appModel)
        if (position >= 0) {
            adapter.appFilteredList.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
        adapter.appsList.remove(appModel)

        val newSet = mutableSetOf<String>()
        newSet.addAll(prefs.hiddenApps)
        if (flag == Constants.FLAG_HIDDEN_APPS)
            newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
        else
            newSet.add(appModel.appPackage + "|" + appModel.user.toString())

        prefs.hiddenApps = newSet
        if (newSet.isEmpty())
            closeDrawer()
        if (prefs.firstHide) {
            binding.search.hideKeyboard()
            prefs.firstHide = false
            viewModel.showDialog.postValue(Constants.Dialog.HIDDEN)
            findNavController().navigate(R.id.action_appListFragment_to_settingsFragment2)
        }
        viewModel.refreshAppList()
        viewModel.getHiddenApps()
    }

    private fun openInfo(appModel: AppModel) {
        openAppInfo(requireContext(), appModel.user, appModel.appPackage)
        findNavController().popBackStack(R.id.mainFragment, false)
    }

    private fun deleteApp(appModel: AppModel) {
        when (appModel) {
            is AppModel.PrivateSpaceHeader -> {}
            is AppModel.PinnedShortcut ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    requireContext().deletePinnedShortcut(
                        packageName = appModel.appPackage,
                        shortcutIdToDelete = appModel.shortcutId,
                        user = appModel.user,
                    )
                }

            is AppModel.App -> {
                if (appModel.user != Process.myUserHandle()) {
                    openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                } else if (requireContext().isSystemApp(appModel.appPackage, appModel.user)) {
                    requireContext().showToast(getString(R.string.system_app_cannot_delete))
                    openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                } else {
                    requireContext().uninstall(appModel.appPackage)
                }
            }
        }
        // The app list refreshes from the LauncherApps callbacks once the removal goes through
    }

    private fun getOriginalAppName(appModel: AppModel.App): String {
        val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        return try {
            launcherApps.getActivityList(appModel.appPackage, appModel.user).firstOrNull()?.label?.toString().orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun getRecyclerViewOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {

            var onTop = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {

                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop)
                            binding.search.hideKeyboard()
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1))
                            binding.search.hideKeyboard()
                        else if (!recyclerView.canScrollVertically(-1))
                            if (!onTop && isRemoving.not())
                                binding.search.showKeyboard(prefs.autoShowKeyboard)
                    }
                }
            }
        }
    }

    // Pops only the drawer. A plain popBackStack() from a late overscroll frame could pop the
    // home screen too, leaving a drawer that auto-launch and back can't close (#766)
    private fun closeDrawer() = findNavController().popBackStack(R.id.appListFragment, true)

    override fun onStart() {
        super.onStart()
        cachedIsCjkKeyboard = null
        binding.search.showKeyboard(prefs.autoShowKeyboard)
    }

    override fun onStop() {
        binding.search.hideKeyboard()
        super.onStop()
    }

    override fun onDestroyView() {
        dialog?.dismiss()
        dialog = null
        super.onDestroyView()
        searchTextView = null
        _binding = null
    }

    companion object {
        private const val MENU_RENAME = 1
        private const val MENU_HIDE = 2
        private const val MENU_INFO = 3
        private const val MENU_DELETE = 4
    }
}
