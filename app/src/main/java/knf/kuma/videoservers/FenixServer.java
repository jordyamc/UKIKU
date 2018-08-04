package knf.kuma.videoservers;

import android.content.Context;
import android.support.annotation.Nullable;

import org.json.JSONObject;
import org.jsoup.Jsoup;

import knf.kuma.commons.BypassUtil;
import knf.kuma.commons.PatternUtil;

import static knf.kuma.videoservers.VideoServer.Names.FENIX;

public class FenixServer extends Server {
    FenixServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return baseLink.contains("s=fenix");
    }

    @Override
    public String getName() {
        return FENIX;
    }

    @Nullable
    @Override
    public VideoServer getVideoServer() {
        try {
            String down_link = PatternUtil.extractLink(baseLink);
            String link = new JSONObject(Jsoup.connect(down_link.replace("embed", "check")).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get().body().text()).getString("file");
            return new VideoServer(FENIX, new Option(getName(), null, link));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
