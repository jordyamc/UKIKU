package knf.kuma.videoservers;

import android.content.Context;

import org.json.JSONObject;
import org.jsoup.Jsoup;

import androidx.annotation.Nullable;
import knf.kuma.commons.BypassUtil;
import knf.kuma.commons.PatternUtil;

import static knf.kuma.videoservers.VideoServer.Names.NATSUKI;

public class NatsukiServer extends Server {
    NatsukiServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return baseLink.contains("s=natsuki");
    }

    @Override
    public String getName() {
        return NATSUKI;
    }

    @Nullable
    @Override
    public VideoServer getVideoServer() {
        try {
            String down_link = PatternUtil.extractLink(baseLink);
            String link = new JSONObject(Jsoup.connect(down_link.replace("embed", "check")).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get().body().text()).getString("file");
            return new VideoServer(NATSUKI, new Option(getName(), null, link));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
