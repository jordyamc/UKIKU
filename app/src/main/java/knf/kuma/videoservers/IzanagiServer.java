package knf.kuma.videoservers;

import android.content.Context;

import org.json.JSONObject;
import org.jsoup.Jsoup;

import androidx.annotation.Nullable;
import knf.kuma.commons.BypassUtil;

import static knf.kuma.videoservers.VideoServer.Names.IZANAGI;

public class IzanagiServer extends Server {
    public IzanagiServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return baseLink.contains("s=izanagi");
    }

    @Override
    public String getName() {
        return IZANAGI;
    }

    @Nullable
    @Override
    public VideoServer getVideoServer() {
        String frame = baseLink.substring(baseLink.indexOf("'") + 1, baseLink.lastIndexOf("'"));
        String down_link = Jsoup.parse(frame).select("iframe").first().attr("src");
        try {
            String link = new JSONObject(Jsoup.connect(down_link.replace("embed", "check")).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get().body().text()).getString("file").replace("\\", "");
            link = link.replaceAll("/", "//").replace(":////", "://");
            return new VideoServer(IZANAGI, new Option(getName(), null, link));
        } catch (Exception e) {
            return null;
        }
    }
}
