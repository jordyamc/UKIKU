package knf.kuma.preferences;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.View;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import knf.kuma.BuildConfig;
import knf.kuma.Main;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.directory.DirectoryService;
import knf.kuma.directory.DirectoryUpdateService;
import knf.kuma.download.DownloadManager;
import knf.kuma.download.FileAccessHelper;
import knf.kuma.jobscheduler.DirUpdateJob;
import knf.kuma.jobscheduler.RecentsJob;
import knf.kuma.widgets.emision.WEmisionProvider;
import xdroid.toaster.Toaster;

public class ConfigurationFragment extends PreferenceFragment {
    public ConfigurationFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        getPreferenceManager().getSharedPreferences().edit().putBoolean("daynigth_permission", Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)).apply();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED))
            getPreferenceScreen().findPreference("daynigth_permission").setEnabled(false);
        getPreferenceScreen().findPreference("daynigth_permission").setOnPreferenceChangeListener((preference, o) -> {
            boolean check = (Boolean) o;
            if (check && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 5587);
                } else if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    getPreferenceManager().getSharedPreferences().edit().putBoolean("daynigth_permission", true).apply();
                    getPreferenceScreen().findPreference("daynigth_permission").setEnabled(false);
                }
            return true;
        });
        getPreferenceScreen().findPreference("download_type").setOnPreferenceChangeListener((preference, o) -> {
            if (o.equals("1") && !FileAccessHelper.INSTANCE.canDownload(ConfigurationFragment.this, (String) o))
                Toaster.toast("Por favor selecciona la raiz de tu SD");
            return true;
        });
        if (PrefsUtil.INSTANCE.getDownloaderType() == 0) {
            getPreferenceScreen().findPreference("max_parallel_downloads").setEnabled(false);
            getPreferenceScreen().findPreference("buffer_size").setEnabled(true);
        } else {
            getPreferenceScreen().findPreference("max_parallel_downloads").setEnabled(true);
            getPreferenceScreen().findPreference("buffer_size").setEnabled(false);
        }
        getPreferenceScreen().findPreference("downloader_type").setOnPreferenceChangeListener((preference, o) -> {
            if (o.equals("0")) {
                getPreferenceScreen().findPreference("max_parallel_downloads").setEnabled(false);
                getPreferenceScreen().findPreference("buffer_size").setEnabled(true);
            } else {
                getPreferenceScreen().findPreference("max_parallel_downloads").setEnabled(true);
                getPreferenceScreen().findPreference("buffer_size").setEnabled(false);
            }
            return true;
        });
        getPreferenceScreen().findPreference("theme_option").setOnPreferenceChangeListener((preference, o) -> {
            AppCompatDelegate.setDefaultNightMode(Integer.parseInt((String) o));
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString("theme_value", (String) o).apply();
            WEmisionProvider.update(getActivity());
            getActivity().recreate();
            return true;
        });
        getPreferenceScreen().findPreference("recents_time").setOnPreferenceChangeListener((preference, o) -> {
            getPreferenceScreen().findPreference("notify_favs").setEnabled(!"0".equals(o));
            RecentsJob.reSchedule(Integer.valueOf((String) o) * 15);
            return true;
        });
        getPreferenceScreen().findPreference("dir_update_time").setOnPreferenceChangeListener((preference, o) -> {
            DirUpdateJob.reSchedule(Integer.valueOf((String) o) * 15);
            return true;
        });
        getPreferenceScreen().findPreference("dir_update").setOnPreferenceClickListener(preference -> {
            try {
                if (!DirectoryUpdateService.isRunning() && !DirectoryService.isRunning())
                    ContextCompat.startForegroundService(getActivity().getApplicationContext(), new Intent(getActivity().getApplicationContext(), DirectoryUpdateService.class));
                else if (DirectoryUpdateService.isRunning())
                    Toaster.toast("Ya se esta actualizando");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
        getPreferenceScreen().findPreference("dir_destroy").setOnPreferenceClickListener(preference -> {
            try {
                if (!DirectoryUpdateService.isRunning() && !DirectoryService.isRunning())
                    new MaterialDialog.Builder(getActivity())
                            .content("¿Desea recrear el directorio?")
                            .positiveText("continuar")
                            .negativeText("cancelar")
                            .onPositive((dialog, which) -> {
                                CacheDB.INSTANCE.animeDAO().nuke();
                                PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putBoolean("directory_finished", false).apply();
                                DirectoryService.run(getActivity().getApplicationContext());
                            }).build().show();
                else if (DirectoryService.isRunning())
                    Toaster.toast("Ya se esta creando");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
        if (EAHelper.getPhase() == 4)
            getPreferenceScreen().findPreference("theme_color").setOnPreferenceChangeListener((preference, newValue) -> {
                startActivity(new Intent(getActivity(), Main.class).putExtra("start_position", 3));
                getActivity().finish();
                return true;
            });
        else if (EAHelper.getPhase() == 0) {
            PreferenceCategory category = ((PreferenceCategory) getPreferenceScreen().findPreference("category_design"));
            category.removePreference(getPreferenceScreen().findPreference("theme_color"));
            Preference pref = new Preference(getActivity());
            pref.setTitle("Color de tema");
            pref.setSummary("Resuelve el secreto para desbloquear");
            pref.setIcon(R.drawable.ic_palette);
            pref.setOnPreferenceClickListener(preference -> {
                Toaster.toast(EAHelper.getEAMessage());
                return true;
            });
            category.addPreference(pref);
        } else {
            getPreferenceScreen().findPreference("theme_color").setSummary("Resuelve el secreto para desbloquear");
            getPreferenceScreen().findPreference("theme_color").setEnabled(false);
        }
        getPreferenceScreen().findPreference("hide_chaps").setOnPreferenceChangeListener((preference, o) -> {
            if (!FileAccessHelper.NOMEDIA_CREATING) {
                FileAccessHelper.INSTANCE.checkNoMedia((boolean) o);
                return true;
            } else {
                ((SwitchPreference) getPreferenceScreen().findPreference("hide_chaps")).setChecked(!((boolean) o));
                return false;
            }
        });
        getPreferenceScreen().findPreference("max_parallel_downloads").setOnPreferenceChangeListener((preference, o) -> {
            DownloadManager.setParallelDownloads((String) o);
            return true;
        });
        if (BuildConfig.DEBUG) {
            getPreferenceScreen().findPreference("reset_recents").setOnPreferenceClickListener(preference -> {
                AsyncTask.execute(() -> {
                    CacheDB.INSTANCE.recentsDAO().clear();
                    RecentsJob.run();
                });
                return true;
            });
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ListView lv = view.findViewById(android.R.id.list);
        if (lv != null)
            ViewCompat.setNestedScrollingEnabled(lv, true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getPreferenceScreen().findPreference("daynigth_permission").setEnabled(false);
        } else {
            getPreferenceManager().getSharedPreferences().edit().putBoolean("daynigth_permission", false).apply();
            ((SwitchPreference) getPreferenceScreen().findPreference("daynigth_permission")).setChecked(false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FileAccessHelper.SD_REQUEST && resultCode == Activity.RESULT_OK) {
            if (!FileAccessHelper.INSTANCE.isUriValid(data.getData())) {
                Toaster.toast("Directorio invalido");
                FileAccessHelper.openTreeChooser(this);
            }
        }
    }
}
