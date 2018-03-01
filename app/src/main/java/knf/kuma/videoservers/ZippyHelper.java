package knf.kuma.videoservers;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZippyHelper {
    public static void calculate(final Context context, final String u, final OnZippyResult callback) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                final MaterialDialog dialog=new MaterialDialog.Builder(context)
                        .content("Obteniendo link")
                        .progress(true,0)
                        .cancelable(false)
                        .build();
                dialog.show();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Connection.Response response = Jsoup.connect(u).execute();
                            Document document = response.parse();
                            String cookies = response.cookie("JSESSIONID");
                            if (cookies!=null){
                                String url = URLDecoder.decode(u, "utf-8");
                                Element center = document.select("div.center").first();
                                Element script = center.select("script").get(1);
                                String script_text = script.outerHtml().replace("<script type=\"text/javascript\">", "");

                                Matcher matcher = Pattern.compile("(\\d+).?%.?(\\d+).?\\+.?(\\d+).?%.?(\\d+)").matcher(script_text);
                                matcher.find();
                                String a = String.valueOf(generateNumber(matcher.group(1), matcher.group(2), matcher.group(4)));
                                Matcher name = Pattern.compile(".*\\/d\\/([a-zA-Z0-9]*)\\/.*\\/(\\d+_\\d+\\.mp4).*").matcher(script_text);
                                name.find();
                                String pre = name.group(1);
                                String d_url = url.substring(0, url.indexOf("/v/")) + "/d/" + pre + "/" + a + "/" + name.group(2);
                                Log.e("Zippy Download", d_url);
                                dialog.dismiss();
                                callback.onSuccess(new ZippyObject(d_url, new CookieConstructor("JSESSIONID="+cookies+";", System.getProperty("http.agent"), url)));
                            }else {
                                Log.e("Zippy", "No cookie");
                                dialog.dismiss();
                                callback.onError();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            dialog.dismiss();
                            callback.onError();
                        }
                    }
                });
            }
        });
    }

    private static int generateNumber(int a, int b, int c) {
        return ((int) ((a % b) + (a % c)));
    }

    private static int generateNumber(String a, String b, String c) {
        return (((Integer.parseInt(a) % Integer.parseInt(b)) + (Integer.parseInt(a) % Integer.parseInt(c))));
    }

    public interface OnZippyResult {
        void onSuccess(ZippyObject object);

        void onError();
    }

    public static class ZippyObject {
        public String download_url;
        public CookieConstructor cookieConstructor;

        public ZippyObject(String url, CookieConstructor cookieConstructor) {
            this.download_url = url;
            this.cookieConstructor = cookieConstructor;
        }
    }
}
