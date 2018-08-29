package knf.kuma;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.InflateException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import org.cryse.widget.persistentsearch.PersistentSearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import butterknife.BindView;
import butterknife.ButterKnife;
import es.munix.multidisplaycast.CastManager;
import knf.kuma.backup.BUUtils;
import knf.kuma.backup.BackUpActivity;
import knf.kuma.backup.MigrationActivity;
import knf.kuma.changelog.ChangelogActivity;
import knf.kuma.commons.BypassUtil;
import knf.kuma.commons.CastUtil;
import knf.kuma.commons.EAHelper;
import knf.kuma.commons.EAMActivity;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.directory.DirectoryFragment;
import knf.kuma.directory.DirectoryService;
import knf.kuma.emision.EmisionActivity;
import knf.kuma.explorer.ExplorerActivity;
import knf.kuma.favorite.FavoriteFragment;
import knf.kuma.jobscheduler.DirUpdateJob;
import knf.kuma.jobscheduler.RecentsJob;
import knf.kuma.jobscheduler.UpdateJob;
import knf.kuma.preferences.BottomPreferencesFragment;
import knf.kuma.queue.QueueActivity;
import knf.kuma.random.RandomActivity;
import knf.kuma.recents.RecentFragment;
import knf.kuma.recents.RecentsNotReceiver;
import knf.kuma.recommended.RecommendActivity;
import knf.kuma.record.RecordActivity;
import knf.kuma.search.FiltersSuggestion;
import knf.kuma.search.SearchFragment;
import knf.kuma.seeing.SeeingActivity;
import knf.kuma.updater.UpdateActivity;
import knf.kuma.updater.Updatechecker;
import q.rorbin.badgeview.Badge;
import q.rorbin.badgeview.QBadgeView;
import xdroid.toaster.Toaster;

import static knf.kuma.commons.BypassUtil.clearCookies;
import static knf.kuma.commons.BypassUtil.isLoading;
import static knf.kuma.commons.BypassUtil.isNeeded;
import static knf.kuma.commons.BypassUtil.saveCookies;
import static knf.kuma.commons.BypassUtil.userAgent;

public class Main extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnNavigationItemReselectedListener,
        Updatechecker.CheckListener,
        BypassUtil.BypassListener {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.searchview)
    PersistentSearchView searchView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.nav_view)
    NavigationView navigationView;
    @BindView(R.id.bottomNavigation)
    BottomNavigationView bottomNavigationView;
    @BindView(R.id.webview)
    @Nullable
    WebView webView;
    BottomFragment selectedFragment;
    BottomFragment tmpfragment;
    ImageButton map;
    ImageButton migrate;
    ImageButton info;
    ImageButton login;
    TextView badge_emission;
    TextView badge_seeing;
    TextView badge_queue;
    private Badge badgeView;
    private boolean readyToFinish = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(EAHelper.getThemeNA(this));
        super.onCreate(savedInstanceState);
        if (!getString(R.string.app_name).equals("UKIKU")) {
            Toaster.toast("Te dije que no lo cambiaras");
            finish();
            return;
        }
        try {
            setContentView(R.layout.activity_main_drawer);
        } catch (InflateException e) {
            setContentView(R.layout.activity_main_drawer_nwv);
        }
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        setNavigationButtons();
        navigationView.setNavigationItemSelectedListener(this);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        bottomNavigationView.setOnNavigationItemReselectedListener(this);
        setSearch();
        if (savedInstanceState == null) {
            checkServices();
            startChange();
        } else
            returnSelectFragment();
        subscribeBadges();
    }

    private void checkServices() {
        checkPermissions();
        DirectoryService.run(this);
        UpdateJob.schedule();
        RecentsJob.schedule(this);
        DirUpdateJob.schedule(this);
        RecentsNotReceiver.removeAll(this);
        EAHelper.clear1();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Updatechecker.check(this, this);
        ChangelogActivity.check(this);
    }

    private void setNavigationButtons() {
        info = navigationView.getHeaderView(0).findViewById(R.id.action_info);
        login = navigationView.getHeaderView(0).findViewById(R.id.action_login);
        migrate = navigationView.getHeaderView(0).findViewById(R.id.action_migrate);
        map = navigationView.getHeaderView(0).findViewById(R.id.action_map);
        badge_emission = (TextView) navigationView.getMenu().findItem(R.id.drawer_emision).getActionView();
        badge_seeing = (TextView) navigationView.getMenu().findItem(R.id.drawer_seeing).getActionView();
        badge_queue = (TextView) navigationView.getMenu().findItem(R.id.drawer_queue).getActionView();
        navigationView.getHeaderView(0).findViewById(R.id.img).setBackgroundResource(EAHelper.getThemeImg(this));
        info.setOnClickListener(v -> AppInfo.open(Main.this));
        login.setOnClickListener(v -> BackUpActivity.start(Main.this));
        migrate.setOnClickListener(v -> MigrationActivity.start(Main.this));
        map.setOnClickListener(v -> EAMActivity.start(Main.this));
        migrate.setVisibility((BUUtils.isAnimeflvInstalled(this) && DirectoryService.isDirectoryFinished(this)) ? View.VISIBLE : View.GONE);
        map.setVisibility(EAHelper.getPhase() == 3 ? View.VISIBLE : View.GONE);
        TextView backupLocation = navigationView.getHeaderView(0).findViewById(R.id.backupLocation);
        switch (BUUtils.getType(this)) {
            case LOCAL:
                backupLocation.setText("Almacenamiento local");
                break;
            case DROPBOX:
                backupLocation.setText("Dropbox");
                break;
            case DRIVE:
                backupLocation.setText("Google Drive");
                break;
        }
    }

    private void subscribeBadges() {
        BottomNavigationMenuView bottomNavigationMenuView =
                (BottomNavigationMenuView) bottomNavigationView.getChildAt(0);
        try {
            View v = bottomNavigationMenuView.getChildAt(1);
            if (badgeView == null) {
                badgeView = new QBadgeView(this)
                        .bindTarget(v)
                        .setExactMode(true)
                        .setShowShadow(false)
                        .setGravityOffset(5, 5, true)
                        .setBadgeBackgroundColor(ContextCompat.getColor(this, EAHelper.getThemeColorLight(this)));
                CacheDB.INSTANCE.favsDAO().getCountLive().observe(this, integer -> {
                    if (badgeView != null)
                        if (PrefsUtil.INSTANCE.getShowFavIndicator())
                            badgeView.setBadgeNumber(integer);
                        else
                            badgeView.hide(false);
                });
                PrefsUtil.INSTANCE.getLiveShowFavIndicator().observe(this, aBoolean -> {
                    if (badgeView != null) {
                        if (aBoolean)
                            badgeView.setBadgeNumber(CacheDB.INSTANCE.favsDAO().getCount());
                        else
                            badgeView.hide(false);
                    }
                });
            }
            badge_emission.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor(this)));
            badge_emission.setTypeface(null, Typeface.BOLD);
            badge_emission.setGravity(Gravity.CENTER_VERTICAL);
            badge_seeing.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor(this)));
            badge_seeing.setTypeface(null, Typeface.BOLD);
            badge_seeing.setGravity(Gravity.CENTER_VERTICAL);
            badge_queue.setTextColor(ContextCompat.getColor(this, EAHelper.getThemeColor(this)));
            badge_queue.setTypeface(null, Typeface.BOLD);
            badge_queue.setGravity(Gravity.CENTER_VERTICAL);
            PrefsUtil.INSTANCE.getLiveEmissionBlackList().observe(this, strings ->
                    CacheDB.INSTANCE.animeDAO().getInEmission(strings).observe(this, integer -> {
                        badge_emission.setText(String.valueOf(integer));
                        badge_emission.setVisibility(integer == 0 ? View.GONE : View.VISIBLE);
                    }));
            CacheDB.INSTANCE.seeingDAO().getCountLive().observe(this, integer -> {
                badge_seeing.setText(String.valueOf(integer));
                badge_seeing.setVisibility(integer == 0 ? View.GONE : View.VISIBLE);
            });
            CacheDB.INSTANCE.queueDAO().getCountLive().observe(this, integer -> {
                badge_queue.setText(String.valueOf(integer));
                badge_queue.setVisibility(integer == 0 ? View.GONE : View.VISIBLE);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 55498);
    }


    @Override
    public void onNeedUpdate(String o_code, final String n_code) {
        runOnUiThread(() -> {
            try {
                new MaterialDialog.Builder(Main.this)
                        .title("Actualización")
                        .content("Parece que la versión " + n_code + " está disponible, ¿Quieres actualizar?")
                        .positiveText("si")
                        .negativeText("despues")
                        .onPositive((dialog, which) -> UpdateActivity.start(Main.this)).build().show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void setSearch() {
        ((EditText) searchView.findViewById(R.id.edittext_search)).setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        searchView.setSuggestionBuilder(new FiltersSuggestion(this));
        searchView.setSearchListener(new PersistentSearchView.SearchListener() {
            @Override
            public void onSearchCleared() {
                if (selectedFragment instanceof SearchFragment)
                    ((SearchFragment) selectedFragment).setSearch("");
            }

            @Override
            public void onSearchTermChanged(String term) {
                EAHelper.checkStart(term);
                if (selectedFragment instanceof SearchFragment)
                    ((SearchFragment) selectedFragment).setSearch(term);
            }

            @Override
            public void onSearch(String query) {
            }

            @Override
            public void onSearchEditOpened() {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            }

            @Override
            public void onSearchEditClosed() {
                /*closeSearch();*/
            }

            @Override
            public boolean onSearchEditBackPressed() {
                closeSearch();
                return true;
            }

            @Override
            public void onSearchExit() {
                closeSearch();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (searchView.isSearching()) {
            closeSearch();
        } else {
            if (readyToFinish) {
                super.onBackPressed();
            } else {
                readyToFinish = true;
                Toaster.toast("Presione de nuevo para salir");
                new Handler().postDelayed(() -> readyToFinish = false, 2000);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (selectedFragment == null || selectedFragment instanceof RecentFragment) {
            getMenuInflater().inflate(R.menu.main, menu);
        } else if (selectedFragment instanceof FavoriteFragment) {
            getMenuInflater().inflate(R.menu.fav_menu, menu);
            switch (PrefsUtil.INSTANCE.getFavsOrder()) {
                case 0:
                    menu.findItem(R.id.by_name).setChecked(true);
                    break;
                case 1:
                    menu.findItem(R.id.by_id).setChecked(true);
                    break;
            }
            if (!PrefsUtil.INSTANCE.showFavSections())
                menu.findItem(R.id.action_new_category).setVisible(false);
        } else if (selectedFragment instanceof DirectoryFragment) {
            getMenuInflater().inflate(R.menu.dir_menu, menu);
            switch (PrefsUtil.INSTANCE.getDirOrder()) {
                case 0:
                    menu.findItem(R.id.by_name_dir).setChecked(true);
                    break;
                case 1:
                    menu.findItem(R.id.by_votes).setChecked(true);
                    break;
                case 2:
                    menu.findItem(R.id.by_id_dir).setChecked(true);
                    break;

            }
        } else {
            getMenuInflater().inflate(R.menu.main, menu);
        }
        searchView.setStartPositionFromMenuItem(findViewById(R.id.action_search));
        CastManager.getInstance().registerForActivity(this, menu, R.id.castMenu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                searchView.openSearch();
                tmpfragment = selectedFragment;
                setFragment(SearchFragment.get());
                break;
            case R.id.action_new_category:
                if (selectedFragment instanceof FavoriteFragment)
                    ((FavoriteFragment) selectedFragment).showNewCategoryDialog(null);
                break;
            case R.id.by_name:
                PrefsUtil.INSTANCE.setFavsOrder(0);
                changeOrder();
                break;
            case R.id.by_name_dir:
                PrefsUtil.INSTANCE.setDirOrder(0);
                changeOrder();
                break;
            case R.id.by_votes:
                PrefsUtil.INSTANCE.setDirOrder(1);
                changeOrder();
                break;
            case R.id.by_id:
                PrefsUtil.INSTANCE.setFavsOrder(1);
                changeOrder();
                break;
            case R.id.by_id_dir:
                PrefsUtil.INSTANCE.setDirOrder(2);
                changeOrder();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void changeOrder() {
        if (selectedFragment instanceof FavoriteFragment) {
            ((FavoriteFragment) selectedFragment).onChangeOrder();
        } else if (selectedFragment instanceof DirectoryFragment) {
            ((DirectoryFragment) selectedFragment).onChangeOrder();
        }
        supportInvalidateOptionsMenu();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        tmpfragment = null;
        switch (item.getItemId()) {
            case R.id.action_bottom_recents:
                setFragment(RecentFragment.get());
                break;
            case R.id.action_bottom_favorites:
                setFragment(FavoriteFragment.get());
                break;
            case R.id.action_bottom_directory:
                setFragment(DirectoryFragment.get());
                break;
            case R.id.action_bottom_settings:
                setFragment(BottomPreferencesFragment.get());
                break;
            case R.id.drawer_explorer:
                ExplorerActivity.open(this);
                break;
            case R.id.drawer_emision:
                EmisionActivity.open(this);
                break;
            case R.id.drawer_queue:
                QueueActivity.open(this);
                break;
            case R.id.drawer_suggestions:
                RecommendActivity.open(this);
                break;
            case R.id.drawer_records:
                RecordActivity.open(this);
                break;
            case R.id.drawer_seeing:
                SeeingActivity.open(this);
                break;
            case R.id.drawer_random:
                RandomActivity.open(this);
                break;
        }
        closeSearchBar();
        supportInvalidateOptionsMenu();
        closeDrawer();
        return true;
    }

    private void setFragment(BottomFragment fragment) {
        try {
            selectedFragment = fragment;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.fadein, R.anim.fadeout);
            transaction.replace(R.id.root, selectedFragment);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeDrawer() {
        drawer.closeDrawer(GravityCompat.START);
    }

    private void closeSearch() {
        closeSearchBar();
        returnFragment();
    }

    private void closeSearchBar() {
        searchView.closeSearch();
    }

    private void returnFragment() {
        if (tmpfragment != null) {
            setFragment(tmpfragment);
            tmpfragment = null;
        } else if (bottomNavigationView != null)
            bottomNavigationView.setSelectedItemId(R.id.action_bottom_recents);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void returnSelectFragment() {
        if (tmpfragment != null) {
            if (tmpfragment instanceof FavoriteFragment) {
                bottomNavigationView.setSelectedItemId(R.id.action_bottom_favorites);
            } else if (tmpfragment instanceof DirectoryFragment) {
                bottomNavigationView.setSelectedItemId(R.id.action_bottom_directory);
            } else if (tmpfragment instanceof BottomPreferencesFragment) {
                bottomNavigationView.setSelectedItemId(R.id.action_bottom_settings);
            } else {
                bottomNavigationView.setSelectedItemId(R.id.action_bottom_recents);
            }
        }
        tmpfragment = null;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void startChange() {
        switch (getIntent().getIntExtra("start_position", -1)) {
            default:
            case 0:
                setFragment(RecentFragment.get());
                break;
            case 1:
                bottomNavigationView.setSelectedItemId(R.id.action_bottom_favorites);
                break;
            case 2:
                bottomNavigationView.setSelectedItemId(R.id.action_bottom_directory);
                break;
            case 3:
                bottomNavigationView.setSelectedItemId(R.id.action_bottom_settings);
                break;
            case 4:
                selectedFragment = RecentFragment.get();
                searchView.openSearch();
                tmpfragment = selectedFragment;
                setFragment(SearchFragment.get(getIntent().getStringExtra("search_query")));
                searchView.setSearchString(getIntent().getStringExtra("search_query"), false);
                break;
        }
    }

    private void reselectFragment() {
        if (selectedFragment != null) {
            if (selectedFragment instanceof FavoriteFragment) {
                bottomNavigationView.setSelectedItemId(R.id.action_bottom_recents);
            } else if (selectedFragment instanceof DirectoryFragment) {
                bottomNavigationView.setSelectedItemId(R.id.action_bottom_directory);
            } else if (selectedFragment instanceof BottomPreferencesFragment) {
                bottomNavigationView.setSelectedItemId(R.id.action_bottom_settings);
            } else {
                bottomNavigationView.setSelectedItemId(R.id.action_bottom_recents);
            }
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onNavigationItemReselected(@NonNull MenuItem item) {
        if (tmpfragment != null) {
            closeSearch();
        } else if (selectedFragment != null) {
            selectedFragment.onReselect();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Crashlytics.setString("screen", "Main");
        supportInvalidateOptionsMenu();
        checkBypass();
        if (navigationView != null)
            new Handler(Looper.getMainLooper()).post(() -> {
                TextView backupLocation = navigationView.getHeaderView(0).findViewById(R.id.backupLocation);
                switch (BUUtils.getType(Main.this)) {
                    case LOCAL:
                        backupLocation.setText("Almacenamiento local");
                        break;
                    case DROPBOX:
                        backupLocation.setText("Dropbox");
                        break;
                    case DRIVE:
                        backupLocation.setText("Google Drive");
                        break;
                }
            });
    }

    @Override
    public void onNeedRecreate() {
        reselectFragment();
    }

    @Override
    protected void onDestroy() {
        CastUtil.get().onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setNavigationButtons();
    }

    private void checkBypass() {
        if (webView != null)
            AsyncTask.execute(() -> {
                if (isNeeded(this) && !isLoading) {
                    isLoading = true;
                    Log.e("CloudflareBypass", "is needed");
                    clearCookies();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        webView.getSettings().setJavaScriptEnabled(true);
                        webView.setWebViewClient(new WebViewClient() {
                            @Override
                            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                                Log.e("CloudflareBypass", "Override " + url);
                                if (url.equals("https://animeflv.net/")) {
                                    Log.e("CloudflareBypass", "Cookies: " + CookieManager.getInstance().getCookie("https://animeflv.net/"));
                                    saveCookies(Main.this);
                                    Toaster.toast("Bypass actualizado");
                                    PicassoSingle.clear();
                                    onNeedRecreate();
                                }
                                isLoading = false;
                                return false;
                            }
                        });
                        webView.getSettings().setUserAgentString(userAgent);
                        webView.loadUrl("https://animeflv.net/");
                    });
                } else {
                    Log.e("CloudflareBypass", "Not needed");
                }
            });
    }
}
