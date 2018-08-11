package knf.kuma.changelog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.fabric.sdk.android.services.concurrency.AsyncTask;
import knf.kuma.R;
import knf.kuma.changelog.objects.Changelog;
import knf.kuma.commons.EAHelper;
import knf.kuma.jobscheduler.DirUpdateJob;
import xdroid.toaster.Toaster;

public class ChangelogActivity extends AppCompatActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.progress)
    ProgressBar progres;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;

    public static void open(Context context) {
        context.startActivity(new Intent(context, ChangelogActivity.class));
    }

    public static void check(final Activity activity) {
        AsyncTask.execute(() -> {
            try {
                int c_code = PreferenceManager.getDefaultSharedPreferences(activity).getInt("version_code", 0);
                final int p_code = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
                if (p_code > c_code && c_code != 0) {
                    runWVersion(p_code);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            new MaterialDialog.Builder(activity)
                                    .content("Nueva versión, ¿Leer Changelog?")
                                    .positiveText("Leer")
                                    .negativeText("Omitir")
                                    .onPositive((dialog, which) -> ChangelogActivity.open(activity))
                                    .onAny((dialog, which) -> PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt("version_code", p_code).apply())
                                    .cancelListener(dialog -> PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt("version_code", p_code).apply()).build().show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else
                    PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt("version_code", p_code).apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void runWVersion(int code) {
        switch (code) {
            case 36:
                DirUpdateJob.runNow();
                break;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(EAHelper.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_changelog);
        ButterKnife.bind(this);
        toolbar.setTitle("Changelog");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
        AsyncTask.execute(() -> {
            try {
                final Changelog changelog = getChangelog();
                progres.post(() -> progres.setVisibility(View.GONE));
                recyclerView.post(() -> recyclerView.setAdapter(new ReleaseAdapter(changelog)));
            } catch (Exception e) {
                Crashlytics.logException(e);
                Toaster.toast("Error al cargar changelog");
                finish();
            }
        });
    }

    private Changelog getChangelog() throws Exception {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("changelog_load", true)) {
            return new Changelog(Jsoup.parse(getXml(), "", Parser.xmlParser()));
        } else {
            return new Changelog(Jsoup.connect("https://raw.githubusercontent.com/jordyamc/UKIKU/master/app/src/main/assets/changelog.xml").parser(Parser.xmlParser()).get());
        }
    }

    private String getXml() {
        String xmlString = null;
        AssetManager am = getAssets();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(am.open("changelog.xml")));
            StringBuilder sb = new StringBuilder();
            String mLine = reader.readLine();
            while (mLine != null) {
                sb.append(mLine);
                mLine = reader.readLine();
            }
            reader.close();
            xmlString = sb.toString();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return xmlString;
    }
}
