package knf.kuma.commons;

import android.content.Context;
import android.net.Uri;

import com.squareup.picasso.UrlConnectionDownloader;

import java.io.IOException;
import java.net.HttpURLConnection;

public  class CookieImageDownloader extends UrlConnectionDownloader {

    private Context context;
    public CookieImageDownloader(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected HttpURLConnection openConnection(Uri path) throws IOException {
        HttpURLConnection conn = super.openConnection(path);
        conn.setRequestProperty("Cookie", BypassUtil.getStringCookie(context));
        conn.setRequestProperty("User-Agent", BypassUtil.userAgent);
        return conn;
    }
}
