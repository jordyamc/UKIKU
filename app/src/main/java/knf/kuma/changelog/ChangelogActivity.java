package knf.kuma.changelog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.fabric.sdk.android.services.concurrency.AsyncTask;
import knf.kuma.R;
import knf.kuma.changelog.objects.Changelog;
import knf.kuma.commons.EAHelper;
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
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int c_code = PreferenceManager.getDefaultSharedPreferences(activity).getInt("version_code", 0);
                    final int p_code = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
                    if (p_code > c_code && c_code != 0)
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    new MaterialDialog.Builder(activity)
                                            .content("Nueva versión, ¿Leer Changelog?")
                                            .positiveText("Leer")
                                            .negativeText("Omitir")
                                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                @Override
                                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                    PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt("version_code", p_code).apply();
                                                    ChangelogActivity.open(activity);
                                                }
                                            })
                                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                                @Override
                                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                    PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt("version_code", p_code).apply();
                                                }
                                            })
                                            .cancelListener(new DialogInterface.OnCancelListener() {
                                                @Override
                                                public void onCancel(DialogInterface dialog) {
                                                    PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt("version_code", p_code).apply();
                                                }
                                            }).build().show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    else
                        PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt("version_code", p_code).apply();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
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
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Changelog changelog = getChangelog();
                    progres.post(new Runnable() {
                        @Override
                        public void run() {
                            progres.setVisibility(View.GONE);
                        }
                    });
                    recyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            recyclerView.setAdapter(new ReleaseAdapter(changelog));
                        }
                    });
                } catch (Exception e) {
                    Crashlytics.logException(e);
                    Toaster.toast("Error al cargar changelog");
                    finish();
                }
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
            InputStream is = am.open("changelog.xml");
            int length = is.available();
            byte[] data = new byte[length];
            is.read(data);
            is.close();
            xmlString = new String(data);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return xmlString;
    }
}
