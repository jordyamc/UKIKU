package knf.kuma

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.view.*
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.crashlytics.android.Crashlytics
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import es.munix.multidisplaycast.CastManager
import knf.kuma.achievements.AchievementActivity
import knf.kuma.achievements.AchievementManager
import knf.kuma.backup.BUUtils
import knf.kuma.backup.BackUpActivity
import knf.kuma.backup.MigrationActivity
import knf.kuma.changelog.ChangelogActivity
import knf.kuma.commons.*
import knf.kuma.commons.BypassUtil.Companion.clearCookies
import knf.kuma.commons.BypassUtil.Companion.isLoading
import knf.kuma.commons.BypassUtil.Companion.isNeeded
import knf.kuma.commons.BypassUtil.Companion.saveCookies
import knf.kuma.commons.BypassUtil.Companion.userAgent
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirectoryFragment
import knf.kuma.directory.DirectoryService
import knf.kuma.emision.EmisionActivity
import knf.kuma.explorer.ExplorerActivity
import knf.kuma.favorite.FavoriteFragment
import knf.kuma.jobscheduler.DirUpdateJob
import knf.kuma.jobscheduler.RecentsJob
import knf.kuma.jobscheduler.UpdateJob
import knf.kuma.news.NewsActivity
import knf.kuma.preferences.BottomPreferencesFragment
import knf.kuma.queue.QueueActivity
import knf.kuma.random.RandomActivity
import knf.kuma.recents.RecentFragment
import knf.kuma.recents.RecentsNotReceiver
import knf.kuma.recommended.RecommendActivity
import knf.kuma.record.RecordActivity
import knf.kuma.search.FiltersSuggestion
import knf.kuma.search.SearchFragment
import knf.kuma.seeing.SeeingActivity
import knf.kuma.updater.UpdateActivity
import knf.kuma.updater.UpdateChecker
import kotlinx.android.synthetic.main.nav_header_main.view.*
import org.cryse.widget.persistentsearch.PersistentSearchView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.hintTextColor
import org.jetbrains.anko.textColor
import q.rorbin.badgeview.Badge
import q.rorbin.badgeview.QBadgeView
import xdroid.toaster.Toaster

class Main : AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnNavigationItemReselectedListener,
        UpdateChecker.CheckListener, BypassUtil.BypassListener {

    private val toolbar by bind<Toolbar>(R.id.toolbar)
    private val searchView by bind<PersistentSearchView>(R.id.searchview)
    private val drawer by bind<DrawerLayout>(R.id.drawer_layout)
    private val navigationView by bind<NavigationView>(R.id.nav_view)
    private val coordinator by bind<CoordinatorLayout>(R.id.coordinator)
    private val bottomNavigationView by bind<BottomNavigationView>(R.id.bottomNavigation)
    private val webView: WebView? by optionalBind(R.id.webview)
    internal var selectedFragment: BottomFragment? = null
    private var tmpfragment: BottomFragment? = null
    private lateinit var badgeEmission: TextView
    private lateinit var badgeSeeing: TextView
    private lateinit var badgeQueue: TextView
    private var badgeView: Badge? = null
    private var readyToFinish = false
    private var isFirst = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getThemeNA(this))
        super.onCreate(savedInstanceState)
        if (getString(R.string.app_name) != "UKIKU") {
            Toaster.toast("Te dije que no lo cambiaras")
            finish()
            return
        }
        try {
            setContentView(R.layout.activity_main_drawer)
        } catch (e: InflateException) {
            setContentView(R.layout.activity_main_drawer_nwv)
        }
        //setDefaults()
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        toolbar.changeToolbarFont()
        setNavigationButtons()
        navigationView.setNavigationItemSelectedListener(this)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.setOnNavigationItemReselectedListener(this)
        setSearch()
        if (savedInstanceState == null) {
            checkServices()
            startChange()
        } else
            returnSelectFragment()
    }

    private fun checkServices() {
        doAsync {
            checkPermissions()
            DirectoryService.run(this@Main)
            UpdateJob.schedule()
            RecentsJob.schedule(this@Main)
            DirUpdateJob.schedule(this@Main)
            RecentsNotReceiver.removeAll(this@Main)
            EAHelper.clear1()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setNavigationButtons() {
        doOnUI {
            badgeEmission = navigationView.menu.findItem(R.id.drawer_emision).actionView as TextView
            badgeSeeing = navigationView.menu.findItem(R.id.drawer_seeing).actionView as TextView
            badgeQueue = navigationView.menu.findItem(R.id.drawer_queue).actionView as TextView
            navigationView.getHeaderView(0).findViewById<View>(R.id.img).setBackgroundResource(EAHelper.getThemeImg(this@Main))
            val header = navigationView.getHeaderView(0).img
            ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
                v.apply {
                    if (insets.systemWindowInsetTop > 0)
                        setPadding(paddingLeft, insets.systemWindowInsetTop, paddingRight, paddingBottom)
                }
                insets
            }
            val actionInfo = navigationView.getHeaderView(0).action_info
            val actionTrophy = navigationView.getHeaderView(0).action_trophy
            val actionLogin = navigationView.getHeaderView(0).action_login
            val actionMigrate = navigationView.getHeaderView(0).action_migrate
            val actionMap = navigationView.getHeaderView(0).action_map
            actionInfo.setOnClickListener { AppInfo.open(this@Main) }
            actionTrophy.setOnClickListener { AchievementActivity.open(this@Main) }
            actionLogin.setOnClickListener { BackUpActivity.start(this@Main) }
            actionMigrate.setOnClickListener { MigrationActivity.start(this@Main) }
            actionMap.setOnClickListener { EAMActivity.start(this@Main) }
            actionMigrate.visibility = if (BUUtils.isAnimeflvInstalled(this@Main) && DirectoryService.isDirectoryFinished(this@Main)) View.VISIBLE else View.GONE
            actionMap.visibility = if (EAHelper.phase == 3) View.VISIBLE else View.GONE
            val backupLocation = navigationView.getHeaderView(0).findViewById<TextView>(R.id.backupLocation)
            when (BUUtils.getType(this@Main)) {
                BUUtils.BUType.LOCAL -> backupLocation.text = "Almacenamiento local"
                BUUtils.BUType.DROPBOX -> backupLocation.text = "Dropbox"
                BUUtils.BUType.DRIVE -> backupLocation.text = "Google Drive"
            }
            subscribeBadges()
        }
    }

    private fun subscribeBadges() {
        val bottomNavigationMenuView = bottomNavigationView.getChildAt(0) as BottomNavigationMenuView
        try {
            val v = bottomNavigationMenuView.getChildAt(1)
            if (badgeView == null) {
                badgeView = QBadgeView(this)
                        .bindTarget(v)
                        .setExactMode(true)
                        .setShowShadow(false)
                        .setGravityOffset(5f, 5f, true)
                        .setBadgeBackgroundColor(ContextCompat.getColor(this, EAHelper.getThemeColorLight(this)))
                CacheDB.INSTANCE.favsDAO().countLive.observe(this, Observer { integer ->
                    if (badgeView != null && integer != null)
                        if (PrefsUtil.showFavIndicator)
                            badgeView?.badgeNumber = integer
                        else
                            badgeView?.hide(false)
                })
                PrefsUtil.getLiveShowFavIndicator().observe(this, Observer { aBoolean ->
                    if (badgeView != null) {
                        if (aBoolean!!)
                            badgeView?.badgeNumber = CacheDB.INSTANCE.favsDAO().count
                        else
                            badgeView?.hide(false)
                    }
                })
                PreferenceManager.getDefaultSharedPreferences(this).stringLiveData("theme_color", "0")
                        .observe(this, Observer {
                            (badgeView as? QBadgeView)?.badgeBackgroundColor = ContextCompat.getColor(this, EAHelper.getThemeColorLight(it))
                            badgeEmission.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor(it)))
                            badgeSeeing.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor(it)))
                            badgeQueue.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor(it)))
                            navigationView.getHeaderView(0).findViewById<View>(R.id.img).setBackgroundResource(EAHelper.getThemeImg(it))
                        })
            }
            badgeEmission.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor(this)))
            badgeEmission.setTypeface(null, Typeface.BOLD)
            badgeEmission.gravity = Gravity.CENTER_VERTICAL
            badgeSeeing.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor(this)))
            badgeSeeing.setTypeface(null, Typeface.BOLD)
            badgeSeeing.gravity = Gravity.CENTER_VERTICAL
            badgeQueue.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor(this)))
            badgeQueue.setTypeface(null, Typeface.BOLD)
            badgeQueue.gravity = Gravity.CENTER_VERTICAL
            PrefsUtil.getLiveEmissionBlackList().observe(this, Observer { strings ->
                CacheDB.INSTANCE.animeDAO().getInEmission(strings).observe(this, Observer { integer ->
                    badgeEmission.text = integer.toString()
                    badgeEmission.visibility = if (integer == 0) View.GONE else View.VISIBLE
                })
            })
            CacheDB.INSTANCE.seeingDAO().countWatchingLive.observe(this, Observer { integer ->
                badgeSeeing.text = integer.toString()
                badgeSeeing.visibility = if (integer == 0) View.GONE else View.VISIBLE
            })
            CacheDB.INSTANCE.queueDAO().countLive.observe(this, Observer { integer ->
                badgeQueue.text = integer.toString()
                badgeQueue.visibility = if (integer == 0) View.GONE else View.VISIBLE
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 55498)
    }

    override fun onNeedUpdate(o_code: String, n_code: String) {
        runOnUiThread {
            try {
                MaterialDialog(this@Main).safeShow {
                    title(text = "Actualización")
                    message(text = "Parece que la versión $n_code está disponible, ¿Quieres actualizar?")
                    positiveButton(text = "si") { UpdateActivity.start(this@Main) }
                    negativeButton(text = "despues")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setSearch() {
        val searchEdit = searchView.findViewById(R.id.edittext_search) as EditText
        searchEdit.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        searchEdit.textColor = ContextCompat.getColor(this, android.R.color.black)
        searchEdit.hintTextColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        searchView.setSuggestionBuilder(FiltersSuggestion(this))
        searchView.setSearchListener(object : PersistentSearchView.SearchListener {
            override fun onSearchCleared() {
                if (selectedFragment is SearchFragment)
                    (selectedFragment as SearchFragment).setSearch("")
            }

            override fun onSearchTermChanged(term: String) {
                EAHelper.checkStart(term)
                AchievementManager.onSearch(term)
                if (selectedFragment is SearchFragment)
                    (selectedFragment as SearchFragment).setSearch(term)
            }

            override fun onSearch(query: String) {}

            override fun onSearchEditOpened() {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            }

            override fun onSearchEditClosed() {
                /*closeSearch();*/
            }

            override fun onSearchEditBackPressed(): Boolean {
                closeSearch()
                return true
            }

            override fun onSearchExit() {
                closeSearch()
            }
        })
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else if (searchView.isSearching) {
            closeSearch()
        } else {
            if (readyToFinish) {
                super.onBackPressed()
            } else {
                readyToFinish = true
                Toaster.toast("Presione de nuevo para salir")
                Handler().postDelayed({ readyToFinish = false }, 2000)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (selectedFragment == null || selectedFragment is RecentFragment) {
            menuInflater.inflate(R.menu.main, menu)
        } else if (selectedFragment is FavoriteFragment) {
            menuInflater.inflate(R.menu.fav_menu, menu)
            when (PrefsUtil.favsOrder) {
                0 -> menu.findItem(R.id.by_name).isChecked = true
                1 -> menu.findItem(R.id.by_id).isChecked = true
            }
            if (!PrefsUtil.showFavSections())
                menu.findItem(R.id.action_new_category).isVisible = false
        } else if (selectedFragment is DirectoryFragment) {
            menuInflater.inflate(R.menu.dir_menu, menu)
            when (PrefsUtil.dirOrder) {
                0 -> menu.findItem(R.id.by_name_dir).isChecked = true
                1 -> menu.findItem(R.id.by_votes).isChecked = true
                2 -> menu.findItem(R.id.by_id_dir).isChecked = true
                3 -> menu.findItem(R.id.by_added_dir).isChecked = true
            }
        } else {
            menuInflater.inflate(R.menu.main, menu)
        }
        searchView.setStartPositionFromMenuItem(findViewById(R.id.action_search))
        CastManager.getInstance().registerForActivity(this, menu, R.id.castMenu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                searchView.openSearch()
                tmpfragment = selectedFragment
                setFragment(SearchFragment.get())
            }
            R.id.action_new_category -> if (selectedFragment is FavoriteFragment)
                (selectedFragment as FavoriteFragment).showNewCategory(null)
            R.id.by_name -> {
                PrefsUtil.favsOrder = 0
                changeOrder()
            }
            R.id.by_name_dir -> {
                PrefsUtil.dirOrder = 0
                changeOrder()
            }
            R.id.by_votes -> {
                PrefsUtil.dirOrder = 1
                changeOrder()
            }
            R.id.by_id -> {
                PrefsUtil.favsOrder = 1
                changeOrder()
            }
            R.id.by_id_dir -> {
                PrefsUtil.dirOrder = 2
                changeOrder()
            }
            R.id.by_added_dir -> {
                PrefsUtil.dirOrder = 3
                changeOrder()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun changeOrder() {
        if (selectedFragment is FavoriteFragment) {
            (selectedFragment as FavoriteFragment).onChangeOrder()
        } else if (selectedFragment is DirectoryFragment) {
            (selectedFragment as DirectoryFragment).onChangeOrder()
        }
        invalidateOptionsMenu()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        tmpfragment = null
        when (item.itemId) {
            R.id.action_bottom_recents -> setFragment(RecentFragment.get())
            R.id.action_bottom_favorites -> setFragment(FavoriteFragment.get())
            R.id.action_bottom_directory -> setFragment(DirectoryFragment.get())
            R.id.action_bottom_settings -> setFragment(BottomPreferencesFragment.get())
            R.id.drawer_explorer -> ExplorerActivity.open(this)
            R.id.drawer_emision -> EmisionActivity.open(this)
            R.id.drawer_queue -> QueueActivity.open(this)
            R.id.drawer_suggestions -> RecommendActivity.open(this)
            R.id.drawer_news -> NewsActivity.open(this)
            R.id.drawer_records -> RecordActivity.open(this)
            R.id.drawer_seeing -> SeeingActivity.open(this)
            R.id.drawer_random -> RandomActivity.open(this)
        }
        closeSearchBar()
        closeDrawer()
        return true
    }

    private fun setFragment(fragment: BottomFragment) {
        doOnUI {
            try {
                selectedFragment = fragment
                val transaction = supportFragmentManager.beginTransaction()
                transaction.setCustomAnimations(R.anim.fadein, R.anim.fadeout)
                transaction.replace(R.id.root, selectedFragment!!)
                transaction.commit()
                invalidateOptionsMenu()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun closeDrawer() {
        drawer.closeDrawer(GravityCompat.START)
    }

    private fun closeSearch() {
        closeSearchBar()
        returnFragment()
    }

    private fun closeSearchBar() {
        searchView.closeSearch()
    }

    private fun returnFragment() {
        if (tmpfragment != null) {
            setFragment(tmpfragment!!)
            tmpfragment = null
        } else {
            bottomNavigationView.selectedItemId = R.id.action_bottom_recents
            setFragment(RecentFragment.get())
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun returnSelectFragment() {
        if (tmpfragment != null) {
            when (tmpfragment) {
                is FavoriteFragment -> bottomNavigationView.selectedItemId = R.id.action_bottom_favorites
                is DirectoryFragment -> bottomNavigationView.selectedItemId = R.id.action_bottom_directory
                is BottomPreferencesFragment -> bottomNavigationView.selectedItemId = R.id.action_bottom_settings
                else -> bottomNavigationView.selectedItemId = R.id.action_bottom_recents
            }
        }
        tmpfragment = null
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun startChange() {
        when (intent.getIntExtra("start_position", -1)) {
            0 -> setFragment(RecentFragment.get())
            1 -> bottomNavigationView.selectedItemId = R.id.action_bottom_favorites
            2 -> bottomNavigationView.selectedItemId = R.id.action_bottom_directory
            3 -> bottomNavigationView.selectedItemId = R.id.action_bottom_settings
            4 -> {
                selectedFragment = RecentFragment.get()
                searchView.openSearch()
                tmpfragment = selectedFragment
                setFragment(SearchFragment[intent.getStringExtra("search_query") ?: ""])
                searchView.setSearchString(intent.getStringExtra("search_query") ?: "", false)
            }
            else -> setFragment(RecentFragment.get())
        }
    }

    private fun reselectFragment() {
        if (selectedFragment != null) {
            when (selectedFragment) {
                is FavoriteFragment -> bottomNavigationView.selectedItemId = R.id.action_bottom_recents
                is DirectoryFragment -> bottomNavigationView.selectedItemId = R.id.action_bottom_directory
                is BottomPreferencesFragment -> bottomNavigationView.selectedItemId = R.id.action_bottom_settings
                else -> bottomNavigationView.selectedItemId = R.id.action_bottom_recents
            }
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        if (tmpfragment != null) {
            closeSearch()
        } else if (selectedFragment != null) {
            selectedFragment!!.onReselect()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            checkPermissions()
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        Crashlytics.setString("screen", "Main")
        invalidateOptionsMenu()
        checkBypass()
        doOnUI {
            val backupLocation = navigationView.getHeaderView(0).findViewById<TextView>(R.id.backupLocation)
            when (BUUtils.getType(this@Main)) {
                BUUtils.BUType.LOCAL -> backupLocation.text = "Almacenamiento local"
                BUUtils.BUType.DROPBOX -> backupLocation.text = "Dropbox"
                BUUtils.BUType.DRIVE -> backupLocation.text = "Google Drive"
            }
        }
        if (isFirst) {
            isFirst = false
        }
    }

    override fun onPause() {
        super.onPause()
        AchievementManager.backup(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ChangelogActivity.check(this)
        UpdateChecker.check(this, this)
        AchievementManager.restore(this)
    }

    override fun onNeedRecreate() {
        reselectFragment()
    }

    override fun onDestroy() {
        if (!isChangingConfigurations)
            CastUtil.get().onDestroy()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setNavigationButtons()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun checkBypass() {
        if (webView != null)
            doAsync {
                if (isNeeded(this@Main) && !isLoading) {
                    val snack = coordinator.showSnackbar("Creando bypass...", Snackbar.LENGTH_INDEFINITE)
                    isLoading = true
                    Log.e("CloudflareBypass", "is needed")
                    clearCookies()
                    doOnUI {
                        webView?.settings?.javaScriptEnabled = true
                        webView?.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                Log.e("CloudflareBypass", "Override ${request?.url}")
                                snack.safeDismiss()
                                if (request?.url.toString() == "https://animeflv.net/") {
                                    Log.e("CloudflareBypass", "Cookies: " + CookieManager.getInstance().getCookie("https://animeflv.net/"))
                                    if (saveCookies(this@Main))
                                        coordinator.showSnackbar("Bypass actualizado")
                                    PicassoSingle.clear()
                                    onNeedRecreate()
                                }
                                isLoading = false
                                return false
                            }
                        }
                        webView?.settings?.userAgentString = userAgent
                        webView?.loadUrl("https://animeflv.net/")
                    }
                } else {
                    Log.e("CloudflareBypass", "Not needed")
                }
            }
    }
}
