package knf.kuma.videoservers;

import android.content.Context;
import android.support.annotation.Nullable;

import org.json.JSONObject;
import org.jsoup.Jsoup;

import static knf.kuma.videoservers.VideoServer.Names.IZANAGI;

/**
 * Created by Jordy on 11/01/2018.
 */

public class IzanagiServer extends Server {
    public IzanagiServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return baseLink.contains("server=izanagi");
    }

    @Override
    public String getName() {
        return IZANAGI;
    }

    //TODO: Bypass

    @Nullable
    @Override
    VideoServer getVideoServer() {
        String frame = baseLink.substring(baseLink.indexOf("'") + 1, baseLink.lastIndexOf("'"));
        String down_link = Jsoup.parse(frame).select("iframe").first().attr("src");
        try {
            return new VideoServer(IZANAGI, new Option(null, new JSONObject(Jsoup.connect(down_link.replace("embed", "check")).get().body().text()).getString("file")));
        } catch (Exception e) {
            return null;
        }
    }
}
