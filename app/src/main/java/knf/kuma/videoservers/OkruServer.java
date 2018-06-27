package knf.kuma.videoservers;

import android.content.Context;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import knf.kuma.commons.BypassUtil;

import static knf.kuma.videoservers.VideoServer.Names.OKRU;

public class OkruServer extends Server {
    public OkruServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return baseLink.contains("ok.ru");
    }

    @Override
    public String getName() {
        return OKRU;
    }

    @Nullable
    @Override
    public VideoServer getVideoServer() {
        try {
            String frame = baseLink.substring(baseLink.indexOf("'") + 1, baseLink.lastIndexOf("'"));
            String down_link = "http:" + Jsoup.parse(frame).select("iframe").first().attr("src");
            String e_json = Jsoup.connect(down_link).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get().select("div[data-module='OKVideo']").first().attr("data-options");
            String cut_json = "{" + e_json.substring(e_json.lastIndexOf("\\\"videos"), e_json.indexOf(",\\\"metadataEmbedded")).replace("\\&quot;", "\"").replace("\\u0026", "&").replace("\\", "") + "}";
            JSONArray array = new JSONObject(cut_json).getJSONArray("videos");
            VideoServer videoServer = new VideoServer(OKRU);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                switch (object.getString("name")) {
                    case "hd":
                        videoServer.addOption(new Option(getName(), "HD", object.getString("url")));
                        break;
                    case "sd":
                        videoServer.addOption(new Option(getName(), "SD", object.getString("url")));
                        break;
                    case "low":
                        videoServer.addOption(new Option(getName(), "LOW", object.getString("url")));
                        break;
                    case "lowest":
                        videoServer.addOption(new Option(getName(), "LOWEST", object.getString("url")));
                        break;
                    case "mobile":
                        videoServer.addOption(new Option(getName(), "MOBILE", object.getString("url")));
                        break;
                }
            }
            return videoServer;
        } catch (Exception e) {
            return null;
        }
    }
}
