package knf.kuma.updater;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import knf.kuma.commons.Network;

public class Updatechecker {
    public static void check(final Context context, final CheckListener listener) {
        if (Network.isConnected())
            AsyncTask.execute(() -> {
                try {
                    Document document = Jsoup.connect("https://raw.githubusercontent.com/jordyamc/UKIKU/master/version.num").get();
                    int n_code = Integer.parseInt(document.select("body").first().ownText().trim());
                    int o_code = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
                    if (n_code > o_code) {
                        listener.onNeedUpdate(String.valueOf(o_code), String.valueOf(n_code));
                    } else {
                        Log.e("Version", "Up to date: " + o_code);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
    }

    public interface CheckListener {
        void onNeedUpdate(String o_code, String n_code);
    }
}
