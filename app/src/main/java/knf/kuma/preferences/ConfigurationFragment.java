package knf.kuma.preferences;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;
import android.widget.ListView;

import knf.kuma.R;
import knf.kuma.downloadservice.FileAccessHelper;
import knf.kuma.jobscheduler.DirUpdateJob;
import knf.kuma.jobscheduler.RecentsJob;
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
        getPreferenceScreen().findPreference("daynigth_permission").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean check = (Boolean) o;
                if (check && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 5587);
                    } else if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        getPreferenceManager().getSharedPreferences().edit().putBoolean("daynigth_permission", true).apply();
                        getPreferenceScreen().findPreference("daynigth_permission").setEnabled(false);
                    }
                return true;
            }
        });
        getPreferenceScreen().findPreference("download_type").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (o.equals("1") && !FileAccessHelper.INSTANCE.canDownload(ConfigurationFragment.this,(String)o))
                    Toaster.toast("Por favor selecciona la raiz de tu SD");
                return true;
            }
        });
        getPreferenceScreen().findPreference("theme_option").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                AppCompatDelegate.setDefaultNightMode(Integer.parseInt((String) o));
                getActivity().recreate();
                return true;
            }
        });
        getPreferenceScreen().findPreference("recents_time").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                getPreferenceScreen().findPreference("notify_favs").setEnabled(!"0".equals(o));
                RecentsJob.reSchedule(Integer.valueOf((String)o)*15);
                return true;
            }
        });
        getPreferenceScreen().findPreference("dir_update_time").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                DirUpdateJob.reSchedule(Integer.valueOf((String)o)*15);
                return true;
            }
        });
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
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FileAccessHelper.SD_REQUEST && resultCode == Activity.RESULT_OK) {
            if (!FileAccessHelper.INSTANCE.isUriValid(data.getData())) {
                Toaster.toast("Directorio invalido");
                FileAccessHelper.openTreeChooser(this);
            }
        }
    }
}
