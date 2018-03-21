package knf.kuma.commons;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import xdroid.toaster.Toaster;

/**
 * Created by jordy on 17/03/2018.
 */

public class BypassUtil {
    public static final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0";
    private static boolean isLoading = false;

    public static void check(final Activity activity) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (isNeeded(activity) && !isLoading) {
                    isLoading = true;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("CloudflareBypass", "is needed");
                            clearCookies();
                            final WebView webView = new WebView(activity);
                            webView.getSettings().setJavaScriptEnabled(true);
                            webView.setWebViewClient(new WebViewClient() {
                                @Override
                                public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                                    Log.e("CloudflareBypass", "Override " + url);
                                    if (url.equals("https://animeflv.net/")) {
                                        Log.e("CloudflareBypass", "Cookies: " + CookieManager.getInstance().getCookie("https://animeflv.net/"));
                                        saveCookies(activity);
                                        Toaster.toast("Bypass actualizado");
                                        PicassoSingle.clear();
                                        ((BypassListener) activity).onNeedRecreate();
                                    }
                                    isLoading = false;
                                    return false;
                                }
                            });
                            webView.getSettings().setUserAgentString(userAgent);
                            webView.loadUrl("https://animeflv.net/");
                        }
                    });
                } else Log.e("CloudflareBypass", "Not needed");
            }
        });
    }

    private static void saveCookies(Context context) {
        String cookies = CookieManager.getInstance().getCookie("https://animeflv.net/").trim();
        if (cookies.contains("cf_clearance")) {
            String[] parts = cookies.split(";");
            SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(context).edit();
            for (String cookie : parts) {
                if (cookie.contains("__cfduid"))
                    preferences.putString("__cfduid", cookie.trim().substring(cookie.trim().indexOf("=") + 1));
                if (cookie.contains("cf_clearance"))
                    preferences.putString("cf_clearance", cookie.trim().substring(cookie.trim().indexOf("=") + 1));
            }
            preferences.apply();
        }
    }

    private static void clearCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookiestring = cookieManager.getCookie(".animeflv.net");
        if (cookiestring != null) {
            String[] cookies = cookiestring.split(";");
            for (String cookie : cookies) {
                String[] cookieparts = cookie.split("=");
                cookieManager.setCookie(".animeflv.net", cookieparts[0].trim() + "=; Expires=Wed, 31 Dec 2025 23:59:59 GMT");
            }
        }
    }

    private static boolean isNeeded(Context context) {
        try {
            Connection.Response response = Jsoup.connect("https://animeflv.net/").cookies(getMapCookie(context)).userAgent(BypassUtil.userAgent).execute();
            return response.statusCode() == 503;
        } catch (HttpStatusException e) {
            return e.getStatusCode() == 503;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Map<String, String> getMapCookie(Context context) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("device", "computer");
        map.put("cf_clearance", getClearance(context));
        map.put("__cfduid", getCFDuid(context));
        return map;
    }

    public static String getStringCookie(Context context) {
        return "device=computer; " +
                "cf_clearance=" + getClearance(context) + "; " +
                "__cfduid=" + getCFDuid(context);
    }

    private static String getClearance(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("cf_clearance", "00000000");
    }

    private static String getCFDuid(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("__cfduid", "00000000");
    }

    public interface BypassListener {
        void onNeedRecreate();
    }
}
