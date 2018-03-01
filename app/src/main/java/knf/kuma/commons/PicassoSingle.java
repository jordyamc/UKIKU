package knf.kuma.commons;

import android.annotation.SuppressLint;
import android.content.Context;

import com.squareup.picasso.Picasso;

public class PicassoSingle {
    @SuppressLint("StaticFieldLeak")
    private static Picasso picasso;

    public static Picasso get(Context context){
        if (picasso==null)
            PicassoSingle.picasso = new Picasso.Builder(context).downloader(new CookieImageDownloader(context)).build();
        return picasso;
    }
}
