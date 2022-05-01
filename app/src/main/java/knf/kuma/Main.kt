package knf.kuma

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import knf.kuma.achievements.AchievementActivity
import knf.kuma.achievements.AchievementManager
import knf.kuma.ads.AdsUtils
import knf.kuma.ads.AdsUtilsMob
import knf.kuma.backup.BackUpActivity
import knf.kuma.backup.Backups
import knf.kuma.backup.MigrationActivity
import knf.kuma.backup.firestore.FirestoreManager
import knf.kuma.backup.firestore.syncData
import knf.kuma.changelog.ChangelogActivity
import knf.kuma.commons.*
import knf.kuma.custom.ConnectionState
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirManager
import knf.kuma.directory.DirectoryFragment
import knf.kuma.directory.DirectoryService
import knf.kuma.download.FileAccessHelper
import knf.kuma.emision.EmissionActivity
import knf.kuma.explorer.ExplorerActivity
import knf.kuma.faq.FaqActivity
import knf.kuma.favorite.FavoriteFragment
import knf.kuma.jobscheduler.DirUpdateWork
import knf.kuma.jobscheduler.RecentsWork
import knf.kuma.jobscheduler.UpdateWork
import knf.kuma.news.NewsActivity
import knf.kuma.pojos.migrateSeen
import knf.kuma.preferences.BottomPreferencesFragment
import knf.kuma.preferences.ConfigurationFragment
import knf.kuma.queue.QueueActivity
import knf.kuma.random.RandomActivity
import knf.kuma.recents.RecentFragment
import knf.kuma.recents.RecentsNotReceiver
import knf.kuma.recommended.RecommendActivity
import knf.kuma.record.RecordActivity
import knf.kuma.search.FiltersSuggestion
import knf.kuma.search.SearchFragment
import knf.kuma.seeing.SeeingActivity
import knf.kuma.uagen.randomUA
import knf.kuma.updater.UpdateActivity
import knf.kuma.updater.UpdateChecker
import knh.kuma.commons.cloudflarebypass.CfCallback
import knh.kuma.commons.cloudflarebypass.Cloudflare
import kotlinx.android.synthetic.main.nav_header_main.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cryse.widget.persistentsearch.PersistentSearchView
import org.cryse.widget.persistentsearch.SearchItem
import org.jetbrains.anko.hintTextColor
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.textColor
import q.rorbin.badgeview.Badge
import q.rorbin.badgeview.QBadgeView
import xdroid.toaster.Toaster
import java.net.HttpCookie
import kotlin.contracts.ExperimentalContracts
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Main : GenericActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnNavigationItemReselectedListener,
        UpdateChecker.CheckListener, BypassUtil.BypassListener,
        ConfigurationFragment.UAChangeListener {

    private val toolbar by bind<Toolbar>(R.id.toolbar)
    private val searchView by bind<PersistentSearchView>(R.id.searchview)
    private val drawer by bind<DrawerLayout>(R.id.drawer_layout)
    private val navigationView by bind<NavigationView>(R.id.nav_view)
    private val coordinator by bind<CoordinatorLayout>(R.id.coordinator)
    private val connectionState by bind<ConnectionState>(R.id.connectionState)
    private val bottomNavigationView by bind<BottomNavigationView>(R.id.bottomNavigation)
    internal var selectedFragment: BottomFragment? = null
    private var searchFragment: SearchFragment? = null
    private lateinit var badgeEmission: TextView
    private lateinit var badgeSeeing: TextView
    private lateinit var badgeQueue: TextView
    private var badgeView: Badge? = null
    private var readyToFinish = false
    private var isFirst = true

    @OptIn(ExperimentalContracts::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getThemeNA())
        super.onCreate(savedInstanceState)
        if (getString(R.string.app_name) != "UKIKU") {
            Toaster.toast("Te dije que no lo cambiaras")
            finish()
            return
        }
        MobileAds.initialize(this)
        AdsUtilsMob.setUp()
        FirebaseAnalytics.getInstance(this).setUserProperty("ads_enabled_new", PrefsUtil.isAdsEnabled.toString())
        try {
            setContentView(R.layout.activity_main_drawer)
        } catch (e: Exception) {
            setContentView(R.layout.activity_main_drawer_nwv)
        }
        //setDefaults()
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        toolbar.changeToolbarFont(R.font.audiowide)
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
        //checkBypass()
        migrateSeen()
        FirestoreManager.start()
        DesignUtils.listenDesignChange(this)
    }

    private fun checkServices() {
        lifecycleScope.launch(Dispatchers.IO) {
            BypassUtil.clearCookiesIfNeeded()
            checkPermissions()
            checkDirectoryState()
            UpdateWork.schedule()
            RecentsWork.schedule(this@Main)
            DirUpdateWork.schedule(this@Main)
            RecentsNotReceiver.removeAll(this@Main)
            EAHelper.clear1()
            verifiyFF()
        }
    }

    private suspend fun checkDirectoryState() {
        DirManager.checkPreDir()
        if (PrefsUtil.useDefaultUserAgent && Network.isConnected) {
            val isBrowserOk = noCrashLet(false) {
                jsoupCookiesDir("https://animeflv.net/browse?order=added&page=5", BypassUtil.isCloudflareActive()).execute()
                true
            }
            if (!isBrowserOk) {
                val randomUA = randomUA()
                PrefsUtil.userAgentDir = randomUA
                suspendCoroutine<Boolean> {
                    lifecycleScope.launch(Dispatchers.Main) {
                        noCrash {
                            Cloudflare(this@Main, "https://animeflv.net/browse?order=added&page=5", PrefsUtil.userAgentDir).apply {
                                setCfCallback(object : CfCallback {
                                    override fun onSuccess(cookieList: MutableList<HttpCookie>?, hasNewUrl: Boolean, newUrl: String?) {
                                        PrefsUtil.dirCookies = cookieList ?: emptyList()
                                        noCrash { it.resume(true) }
                                    }

                                    override fun onFail(code: Int, msg: String?) {
                                        Log.e("Dir cookies", "On error, code $code, msg: $msg")
                                        noCrash { it.resume(false) }
                                    }
                                })
                            }.getCookies()
                        }
                    }
                }
            }
        }
        DirectoryService.run(this)
    }

    @SuppressLint("SetTextI18n")
    private fun setNavigationButtons() {
        doOnUI {
            badgeEmission = navigationView.menu.findItem(R.id.drawer_emision).actionView as TextView
            badgeSeeing = navigationView.menu.findItem(R.id.drawer_seeing).actionView as TextView
            badgeQueue = navigationView.menu.findItem(R.id.drawer_queue).actionView as TextView
            navigationView.getHeaderView(0).findViewById<View>(R.id.img).setBackgroundResource(EAHelper.getThemeImg())
            val header = navigationView.getHeaderView(0).img
            ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
                v.apply {
                    if (insets.getInsets(WindowInsetsCompat.Type.systemBars()).top > 0)
                        setPadding(paddingLeft, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, paddingRight, paddingBottom)
                }
                insets
            }
            val actionShare = navigationView.getHeaderView(0).action_share
            val actionInfo = navigationView.getHeaderView(0).action_info
            val actionTrophy = navigationView.getHeaderView(0).action_trophy
            val actionLogin = navigationView.getHeaderView(0).action_login
            val actionMigrate = navigationView.getHeaderView(0).action_migrate
            val actionMap = navigationView.getHeaderView(0).action_map
            actionShare.onClick {
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Hola,\n" +
                            "\n" +
                            "UKIKU es una aplicación rápida y simple que uso para ver mis animes favoritos.\n" +
                            "\n" +
                            "Descárgala gratis desde https://ukiku.app/")
                }, "Compartir UKIKU"))
            }
            actionInfo.onClick { AppInfoActivity.open(this@Main) }
            actionTrophy.onClick { AchievementActivity.open(this@Main) }
            actionLogin.onClick { BackUpActivity.start(this@Main) }
            actionMigrate.onClick { MigrationActivity.start(this@Main) }
            actionMap.onClick { EAMapActivity.start(this@Main) }
            actionMigrate.visibility = if (Backups.isAnimeflvInstalled) View.VISIBLE else View.GONE
            actionMap.visibility = if (EAHelper.phase == 3) View.VISIBLE else View.GONE
            val backupLocation = navigationView.getHeaderView(0).findViewById<TextView>(R.id.backupLocation)
            backupLocation.text = when (Backups.type) {
                Backups.Type.NONE -> "Sin respaldos"
                Backups.Type.DROPBOX -> "Dropbox"
                Backups.Type.FIRESTORE -> "Firestore"
                Backups.Type.LOCAL -> "Local"
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
                        .setBadgeBackgroundColor(ContextCompat.getColor(this, EAHelper.getThemeColorLight()))
                CacheDB.INSTANCE.favsDAO().countLive.observe(this, { integer ->
                    if (badgeView != null && integer != null)
                        if (PrefsUtil.showFavIndicator)
                            badgeView?.badgeNumber = integer
                        else
                            badgeView?.hide(false)
                })
                PrefsUtil.getLiveShowFavIndicator().observe(this, { aBoolean ->
                    if (badgeView != null) {
                        if (aBoolean)
                            lifecycleScope.launch { badgeView?.badgeNumber = withContext(Dispatchers.IO) { CacheDB.INSTANCE.favsDAO().count } }
                        else
                            badgeView?.hide(false)
                    }
                })
                PreferenceManager.getDefaultSharedPreferences(this).stringLiveData("theme_color", "0")
                        .observe(this, {
                            (badgeView as? QBadgeView)?.badgeBackgroundColor = ContextCompat.getColor(this, EAHelper.getThemeColorLight(it))
                            badgeEmission.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor(it)))
                            badgeSeeing.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor(it)))
                            badgeQueue.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor(it)))
                            navigationView.getHeaderView(0).findViewById<View>(R.id.img).setBackgroundResource(EAHelper.getThemeImg(it))
                        })
            }
            badgeEmission.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor()))
            badgeEmission.setTypeface(null, Typeface.BOLD)
            badgeEmission.gravity = Gravity.CENTER_VERTICAL
            badgeSeeing.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor()))
            badgeSeeing.setTypeface(null, Typeface.BOLD)
            badgeSeeing.gravity = Gravity.CENTER_VERTICAL
            badgeQueue.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor()))
            badgeQueue.setTypeface(null, Typeface.BOLD)
            badgeQueue.gravity = Gravity.CENTER_VERTICAL
            PrefsUtil.getLiveEmissionBlackList().observe(this, { strings ->
                CacheDB.INSTANCE.animeDAO().getInEmission(strings).observe(this, { integer ->
                    badgeEmission.text = integer.toString()
                    badgeEmission.visibility = if (integer == 0) View.GONE else View.VISIBLE
                })
            })
            CacheDB.INSTANCE.seeingDAO().countWatchingLive.observe(this, { integer ->
                badgeSeeing.text = integer.toString()
                badgeSeeing.visibility = if (integer == 0) View.GONE else View.VISIBLE
            })
            CacheDB.INSTANCE.queueDAO().countLive.observe(this, { integer ->
                badgeQueue.text = integer.toString()
                badgeQueue.visibility = if (integer == 0) View.GONE else View.VISIBLE
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 55498)
    }

    private fun showRationalPermission(denied: Boolean = false) {
        MaterialDialog(this).safeShow {
            title(text = "Permiso de escritura")
            message(text = "Esta aplicación necesita el permiso obligatoriamente para guardar cache y descargar los episodios")
            positiveButton(text = "Aceptar") {
                if (denied) {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        finish()
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        "Error al mostrar configuración".toast()
                    }
                } else
                    checkPermissions()
            }
            negativeButton(text = "Salir") { finish() }
            cancelable(false)
        }
    }

    override fun onNeedUpdate(o_code: String, n_code: String) {
        runOnUiThread {
            try {
                MaterialDialog(this@Main).safeShow {
                    title(text = "Actualización")
                    if (n_code.toInt() > AdsUtils.remoteConfigs.getLong("min_version").toInt()) {
                        message(text = "Parece que la versión $n_code está disponible, ¿Quieres actualizar?")
                        positiveButton(text = "si") { UpdateActivity.start(this@Main, true) }
                        negativeButton(text = "despues") {
                            checkBypass()
                        }
                    }else {
                        message(text = "Parece que la versión $n_code está disponible, es obligatoria")
                        positiveButton(text = "actualizar") {
                            UpdateActivity.start(
                                this@Main,
                                false
                            )
                        }
                        cancelable(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onUpdateNotRequired() {
        checkBypass()
    }

    private fun setSearch() {
        searchView.searchEditText.apply {
            inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            textColor = ContextCompat.getColor(this@Main, android.R.color.black)
            hintTextColor = ContextCompat.getColor(this@Main, android.R.color.darker_gray)
        }
        searchView.setSuggestionBuilder(FiltersSuggestion(this))
        searchView.setSearchListener(object : PersistentSearchView.SearchListener {
            override fun onSearchCleared() {
                searchFragment?.setSearch("")
            }

            override fun onSearchTermChanged(term: String) {
                EAHelper.checkStart(term)
                AchievementManager.onSearch(term)
                searchFragment?.setSearch(term)
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

            override fun onSuggestion(searchItem: SearchItem?): Boolean = true
        })
    }

    private fun onStateDialog(message: String) {
        MaterialDialog(this).safeShow {
            message(text = message)
            positiveButton()
        }
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
                4 -> menu.findItem(R.id.by_followers).isChecked = true
            }
        } else {
            menuInflater.inflate(R.menu.main, menu)
        }
        searchView.setStartPositionFromMenuItem(findViewById(R.id.action_search))
        CastUtil.registerActivity(this, menu, R.id.castMenu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                searchView.openSearch()
                searchFragment = SearchFragment.get()
                setFragment(searchFragment as BottomFragment)
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
            R.id.by_followers -> {
                PrefsUtil.dirOrder = 4
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
        when (item.itemId) {
            R.id.action_bottom_recents -> setFragment(RecentFragment.get())
            R.id.action_bottom_favorites -> setFragment(FavoriteFragment.get())
            R.id.action_bottom_directory -> setFragment(DirectoryFragment.get())
            R.id.action_bottom_settings -> setFragment(BottomPreferencesFragment.get())
            R.id.drawer_explorer -> ExplorerActivity.open(this)
            R.id.drawer_emision -> EmissionActivity.open(this)
            R.id.drawer_queue -> QueueActivity.open(this)
            R.id.drawer_suggestions -> RecommendActivity.open(this)
            R.id.drawer_news -> NewsActivity.open(this)
            R.id.drawer_records -> RecordActivity.open(this)
            R.id.drawer_seeing -> SeeingActivity.open(this)
            R.id.drawer_random -> RandomActivity.open(this)
            R.id.drawer_faq -> FaqActivity.open(this)
        }
        closeSearchBar()
        closeDrawer()
        return true
    }

    private fun setFragment(fragment: BottomFragment) {
        doOnUI {
            try {
                if (fragment !is SearchFragment)
                    selectedFragment = fragment
                val transaction = supportFragmentManager.beginTransaction()
                //transaction.setCustomAnimations(R.anim.fadein, R.anim.fadeout)
                transaction.replace(R.id.root, fragment)
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
        selectedFragment?.let {
            setFragment(it)
        } ?: let {
            bottomNavigationView.selectedItemId = R.id.action_bottom_recents
            setFragment(RecentFragment.get())
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun returnSelectFragment() {
        if (selectedFragment != null) {
            when (selectedFragment) {
                is FavoriteFragment -> bottomNavigationView.selectedItemId = R.id.action_bottom_favorites
                is DirectoryFragment -> bottomNavigationView.selectedItemId = R.id.action_bottom_directory
                is BottomPreferencesFragment -> bottomNavigationView.selectedItemId = R.id.action_bottom_settings
                else -> bottomNavigationView.selectedItemId = R.id.action_bottom_recents
            }
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun startChange() {
        if (intent.dataString?.let {
                    return@let if ("ukiku.app/search/" in it) {
                        val query = it.substringAfter("ukiku.app/search/")
                        selectedFragment = RecentFragment.get()
                        searchView.openSearch()
                        setFragment(SearchFragment[query])
                        searchView.setSearchString(query, false)
                        true
                    } else
                        false
                } == true) return
        when (intent.getIntExtra("start_position", -1)) {
            0 -> setFragment(RecentFragment.get())
            1 -> bottomNavigationView.selectedItemId = R.id.action_bottom_favorites
            2 -> bottomNavigationView.selectedItemId = R.id.action_bottom_directory
            3 -> bottomNavigationView.selectedItemId = R.id.action_bottom_settings
            4 -> {
                selectedFragment = RecentFragment.get()
                searchView.openSearch()
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
        if (selectedFragment != null && searchView.isSearching) {
            closeSearch()
        } else if (selectedFragment != null) {
            selectedFragment?.onReselect()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                showRationalPermission()
            else
                showRationalPermission(true)
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
        connectionState.setUp(this, ::onStateDialog)
        doOnUI {
            val backupLocation = navigationView.getHeaderView(0).findViewById<TextView>(R.id.backupLocation)
            backupLocation.text = when (Backups.type) {
                Backups.Type.NONE -> "Sin respaldos"
                Backups.Type.DROPBOX -> "Dropbox"
                Backups.Type.FIRESTORE -> "Firestore"
                Backups.Type.LOCAL -> "Local"
            }
        }
        if (isFirst) {
            isFirst = false
        }
    }

    override fun onPause() {
        syncData { achievements() }
        super.onPause()
    }

    override fun onUAChange() {
        checkBypass()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ChangelogActivity.check(this)
        UpdateChecker.check(this, this)
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
        noCrash {
            if (requestCode == FileAccessHelper.SD_REQUEST && resultCode == Activity.RESULT_OK) {
                val validation = FileAccessHelper.isUriValid(data?.data)
                if (!validation.isValid) {
                    Toaster.toast("Directorio invalido: $validation")
                    FileAccessHelper.openTreeChooser(this)
                }
            }
        }
        setNavigationButtons()
    }

    override fun onBypassUpdated() {
        onNeedRecreate()
    }

    override fun getSnackbarAnchor(): View {
        return coordinator
    }
}
