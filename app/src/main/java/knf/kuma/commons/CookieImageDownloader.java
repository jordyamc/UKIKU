package knf.kuma.commons;

import android.content.Context;
import android.net.Uri;

import com.squareup.picasso.UrlConnectionDownloader;

import java.io.IOException;
import java.net.HttpURLConnection;

public  class CookieImageDownloader extends UrlConnectionDownloader {

    public CookieImageDownloader(Context context) {
        super(context);
    }

    @Override
    protected HttpURLConnection openConnection(Uri path) throws IOException {
        HttpURLConnection conn = super.openConnection(path);

        String cookieName = "";
        String cookieValue = "";
        conn.setRequestProperty("Cookie",cookieName + "=" + cookieValue );

        return conn;
    }
}
