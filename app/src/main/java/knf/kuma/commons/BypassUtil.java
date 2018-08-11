package knf.kuma.commons;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import xdroid.toaster.Toaster;

/**
 * Created by jordy on 17/03/2018.
 */

public class BypassUtil extends AppCompatActivity {
    public static final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0";
    public static boolean isLoading = false;

    @BindView(R.id.webview)
    WebView web;

    public static void check(final Activity activity) {
        start(activity);
        /*AsyncTask.execute(() -> {
            if (isNeeded(activity) && !isLoading) {
                isLoading = true;
                activity.runOnUiThread(() -> {
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

                        @Override
                        public void onPageStarted(WebView view, String url, Bitmap favicon) {
                            super.onPageStarted(view, url, favicon);
                            Log.e("CloudflareBypass", "Started " + url);
                        }
                    });
                    webView.getSettings().setUserAgentString(userAgent);
                    webView.loadUrl("https://animeflv.net/");
                });
            } else Log.e("CloudflareBypass", "Not needed");
        });*/
    }

    public static void saveCookies(Context context) {
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

    public static void clearCookies() {
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

    public static boolean isNeeded(Context context) {
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

    public static void start(Context context) {
        context.startActivity(new Intent(context, BypassUtil.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        ButterKnife.bind(this);
        AsyncTask.execute(() -> {
            if (isNeeded(this) && !isLoading) {
                Toaster.toast("Creando bypass");
                isLoading = true;
                Log.e("CloudflareBypass", "is needed");
                clearCookies();
                web.getSettings().setJavaScriptEnabled(true);
                web.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                        Log.e("CloudflareBypass", "Override " + url);
                        if (url.equals("https://animeflv.net/")) {
                            Log.e("CloudflareBypass", "Cookies: " + CookieManager.getInstance().getCookie("https://animeflv.net/"));
                            saveCookies(BypassUtil.this);
                            Toaster.toast("Bypass actualizado");
                            PicassoSingle.clear();
                        }
                        isLoading = false;
                        Toaster.toast("Bypass creado");
                        finish();
                        return false;
                    }
                });
                web.getSettings().setUserAgentString(userAgent);
                web.loadUrl("https://animeflv.net/");
            } else {
                Log.e("CloudflareBypass", "Not needed");
                Toaster.toast("Bypass no necesario");
                finish();
            }
        });
    }

    public interface BypassListener {
        void onNeedRecreate();
    }
}
