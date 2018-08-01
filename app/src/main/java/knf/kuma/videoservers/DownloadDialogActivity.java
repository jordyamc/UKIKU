package knf.kuma.videoservers;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import knf.kuma.commons.EAHelper;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.DownloadObject;
import knf.kuma.pojos.NotificationObj;

public class DownloadDialogActivity extends AppCompatActivity {

    private DownloadObject object;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(EAHelper.getThemeDialog(this));
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(false);
        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("Obteniendo informacion...")
                .progress(true, 0)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .build();
        dialog.show();
        AsyncTask.execute(() -> {
            try {
                Document document = Jsoup.connect(getIntent().getDataString()).get();
                String name = document.select("nav.Brdcrmb.fa-home a[href^=/anime/]").first().text();
                String aid = null;
                String eid = extract(getIntent().getDataString(), "^.*/(\\d+)/.*$");
                String num = null;
                Matcher matcher = Pattern.compile("var (.*) = (\\d+);").matcher(document.html());
                while (matcher.find()) {
                    switch (matcher.group(1)) {
                        case "anime_id":
                            aid = matcher.group(2);
                            break;
                        case "episode_number":
                            num = matcher.group(2);
                            break;
                    }
                }
                Log.e("Download deep", "\nName: " + name + "\nAid: " + aid + "\nEid: " + eid + "\nNum: " + num);
                AnimeObject.WebInfo.AnimeChapter chapter = new AnimeObject.WebInfo.AnimeChapter(Integer.parseInt(aid), "Episodio " + num, eid, getIntent().getDataString(), name, aid);
                object = DownloadObject.fromChapter(chapter, false);
                runOnUiThread(() -> {
                    try {
                        dialog.dismiss();
                        showSelectDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                        finish();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                finish();
            }
        });

    }

    private void showSelectDialog() {
        new MaterialDialog.Builder(this)
                .items(new String[]{"Descarga", "Streaming"})
                .itemsCallbackSingleChoice(0, (dialog, itemView, which, text) -> {
                    runOnUiThread(() ->
                            ServersFactory.start(DownloadDialogActivity.this, getIntent().getDataString(), object, which == 1, new ServersFactory.ServersInterface() {
                                @Override
                                public void onFinish(boolean started, boolean success) {
                                    if (success)
                                        removeNotification();
                                    finish();
                                }

                                @Override
                                public void onCast(String url) {

                                }
                            }));
                    return true;
                })
                .positiveText("seleccionar")
                .negativeText("cancelar")
                .onNegative((dialog, which) -> finish())
                .build().show();
    }

    private String extract(String st, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(st);
        matcher.find();
        return matcher.group(1);
    }

    private void removeNotification() {
        if (getIntent().getBooleanExtra("notification", false)) {
            NotificationObj obj = NotificationObj.fromIntent(getIntent());
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(obj.key);
            sendBroadcast(obj.getBroadcast(this));
        }
    }
}
