package knf.kuma.slices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;
import knf.kuma.R;
import knf.kuma.commons.PatternUtil;
import knf.kuma.commons.PicassoSingle;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;

public class AnimeLoad extends BroadcastReceiver {
    public static String QUERY = "/anime/";
    public static List<AnimeObject> LIST = new ArrayList<>();
    public static IconCompat openIcon;
    public static IconCompat searchIcon;

    private static Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        AsyncTask.execute(() -> {
            try {
                if (searchIcon == null)
                    searchIcon = IconCompat.createWithBitmap(getBitmap((VectorDrawable) Objects.requireNonNull(ContextCompat.getDrawable(context, R.drawable.ic_search_black_24dp))));
                if (openIcon == null)
                    openIcon = IconCompat.createWithBitmap(getBitmap((VectorDrawable) Objects.requireNonNull(ContextCompat.getDrawable(context, R.drawable.ic_open))));
                if (!QUERY.equals("/anime/")) {
                    LIST = CacheDB.createAndGet(context).animeDAO().getByName("%" + QUERY.replace("/anime/", "").trim() + "%");
                    for (AnimeObject object : LIST) {
                        try {
                            object.icon = IconCompat.createWithBitmap(PicassoSingle.get(context).load(PatternUtil.getCover(object.aid)).get());
                        } catch (IOException e) {
                            object.icon = IconCompat.createWithResource(context, R.mipmap.ic_launcher);
                        }
                    }
                    context.getContentResolver().notifyChange(Uri.parse("content://knf.kuma.slices" + QUERY), null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
