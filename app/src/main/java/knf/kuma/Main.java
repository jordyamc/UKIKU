package knf.kuma;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.cryse.widget.persistentsearch.PersistentSearchView;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.munix.multidisplaycast.CastManager;
import knf.kuma.backup.BUUtils;
import knf.kuma.backup.BackUpActivity;
import knf.kuma.backup.MigrationActivity;
import knf.kuma.changelog.ChangelogActivity;
import knf.kuma.commons.BypassUtil;
import knf.kuma.commons.CastUtil;
import knf.kuma.directory.DirectoryFragment;
import knf.kuma.directory.DirectoryService;
import knf.kuma.emision.EmisionActivity;
import knf.kuma.explorer.ExplorerActivity;
import knf.kuma.favorite.FavoriteFragment;
import knf.kuma.jobscheduler.DirUpdateJob;
import knf.kuma.jobscheduler.RecentsJob;
import knf.kuma.jobscheduler.UpdateJob;
import knf.kuma.preferences.BottomPreferencesFragment;
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
import xdroid.toaster.Toaster;

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
    BottomFragment selectedFragment;
    BottomFragment tmpfragment;

    ImageButton migrate;
    ImageButton info;
    ImageButton login;

    private boolean readyToFinish = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getString(R.string.app_name).equals("UKIKU")) {
            Toaster.toast("Te dije que no lo cambiaras");
            finish();
            return;
        }
        setContentView(R.layout.activity_main_drawer);
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
            setFragment(RecentFragment.get());
        } else {
            returnSelectFragment();
        }
    }

    private void checkServices() {
        checkPermissions();
        ContextCompat.startForegroundService(this, new Intent(this, DirectoryService.class));
        UpdateJob.schedule();
        RecentsJob.schedule(this);
        DirUpdateJob.schedule(this);
        RecentsNotReceiver.removeAll(this);
        Updatechecker.check(this, this);
        ChangelogActivity.check(this);
    }

    private void setNavigationButtons() {
        info = navigationView.getHeaderView(0).findViewById(R.id.action_info);
        login = navigationView.getHeaderView(0).findViewById(R.id.action_login);
        migrate = navigationView.getHeaderView(0).findViewById(R.id.action_migrate);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppInfo.open(Main.this);
            }
        });
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BackUpActivity.start(Main.this);
            }
        });
        migrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MigrationActivity.start(Main.this);
            }
        });
        if (BUUtils.isAnimeflvInstalled(this) &&
                DirectoryService.isDirectoryFinished(this))
            migrate.setVisibility(View.VISIBLE);
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

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 55498);
    }


    @Override
    public void onNeedUpdate(String o_code, final String n_code) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    new MaterialDialog.Builder(Main.this)
                            .title("Actualización")
                            .content("Parece que la versión " + n_code + " está disponible, ¿Quieres actualizar?")
                            .positiveText("si")
                            .negativeText("despues")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    UpdateActivity.start(Main.this);
                                }
                            }).build().show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        readyToFinish = false;
                    }
                }, 2000);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (selectedFragment == null || selectedFragment instanceof RecentFragment) {
            getMenuInflater().inflate(R.menu.main, menu);
        } else if (selectedFragment instanceof FavoriteFragment) {
            getMenuInflater().inflate(R.menu.fav_menu, menu);
            switch (PreferenceManager.getDefaultSharedPreferences(this).getInt("favs_order", 0)) {
                case 0:
                    menu.findItem(R.id.by_name).setChecked(true);
                    break;
                case 1:
                    menu.findItem(R.id.by_id).setChecked(true);
                    break;
            }
        } else if (selectedFragment instanceof DirectoryFragment) {
            getMenuInflater().inflate(R.menu.dir_menu, menu);
            switch (PreferenceManager.getDefaultSharedPreferences(this).getInt("dir_order", 0)) {
                case 0:
                    menu.findItem(R.id.by_name_dir).setChecked(true);
                    break;
                case 1:
                    menu.findItem(R.id.by_votes).setChecked(true);
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
            case R.id.by_name:
                PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("favs_order", 0).apply();
                changeOrder();
                break;
            case R.id.by_name_dir:
                PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("dir_order", 0).apply();
                changeOrder();
                break;
            case R.id.by_votes:
                PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("dir_order", 1).apply();
                changeOrder();
                break;
            case R.id.by_id:
                PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("favs_order", 1).apply();
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
        }
        tmpfragment = null;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void returnSelectFragment() {
        if (tmpfragment != null) {
            if (tmpfragment instanceof FavoriteFragment) {
                bottomNavigationView.setSelectedItemId(R.id.action_bottom_recents);
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
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
            checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BypassUtil.check(this);
        if (navigationView != null)
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
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
}
