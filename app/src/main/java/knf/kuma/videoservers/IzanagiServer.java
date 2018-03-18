package knf.kuma.videoservers;

import android.content.Context;
import android.support.annotation.Nullable;

import org.json.JSONObject;
import org.jsoup.Jsoup;

import knf.kuma.commons.BypassUtil;

import static knf.kuma.videoservers.VideoServer.Names.IZANAGI;

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

    @Nullable
    @Override
    VideoServer getVideoServer() {
        String frame = baseLink.substring(baseLink.indexOf("'") + 1, baseLink.lastIndexOf("'"));
        String down_link = Jsoup.parse(frame).select("iframe").first().attr("src");
        try {
            return new VideoServer(IZANAGI, new Option(null, new JSONObject(Jsoup.connect(down_link.replace("embed", "check")).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get().body().text()).getString("file")));
        } catch (Exception e) {
            return null;
        }
    }
}
