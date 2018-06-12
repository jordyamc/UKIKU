package knf.kuma.videoservers;

import android.content.Context;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import knf.kuma.commons.BypassUtil;

import static knf.kuma.videoservers.VideoServer.Names.HYPERION;

public class HyperionServer extends Server {
    public HyperionServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return baseLink.contains("server=hyperion");
    }

    @Override
    public String getName() {
        return HYPERION;
    }

    @Nullable
    @Override
    public VideoServer getVideoServer() {
        String frame = baseLink.substring(baseLink.indexOf("'") + 1, baseLink.lastIndexOf("'"));
        String down_link = Jsoup.parse(frame).select("iframe").first().attr("src");
        try {
            JSONObject options = new JSONObject(Jsoup.connect(down_link.replace("embed_hyperion", "check")).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get().body().text());
            JSONArray array = options.getJSONArray("streams");
            VideoServer videoServer = new VideoServer(HYPERION);
            for (int i = 0; i < array.length(); i++) {
                try {
                    switch (array.getJSONObject(i).getInt("label")) {
                        case 360:
                            videoServer.addOption(new Option(getName(), "360p", array.getJSONObject(i).getString("file")));
                            break;
                        case 480:
                            videoServer.addOption(new Option(getName(), "480p", array.getJSONObject(i).getString("file")));
                            break;
                        case 720:
                            videoServer.addOption(new Option(getName(), "720p", array.getJSONObject(i).getString("file")));
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                videoServer.addOption(new Option(getName(), "Direct", options.getString("direct")));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return videoServer;
        } catch (Exception e) {
            return null;
        }
    }
}
