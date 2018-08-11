package knf.kuma.videoservers;

import android.content.Context;

import org.jsoup.Jsoup;

import androidx.annotation.Nullable;
import knf.kuma.commons.BypassUtil;
import knf.kuma.commons.PatternUtil;

import static knf.kuma.videoservers.VideoServer.Names.RV;

public class RVServer extends Server {
    public RVServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return baseLink.contains("rapidvideo") || baseLink.contains("&server=rv");
    }

    @Override
    public String getName() {
        return RV;
    }

    @Nullable
    @Override
    public VideoServer getVideoServer() {
        try {
            String frame = baseLink.substring(baseLink.indexOf("'") + 1, baseLink.lastIndexOf("'"));
            String down_link = Jsoup.parse(frame).select("iframe").first().attr("src").replaceAll("&q=720p|&q=480p|&q=360p", "");
            if (down_link.contains("&server=rv"))
                down_link = PatternUtil.getRapidLink(Jsoup.connect(down_link).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get().outerHtml()).replaceAll("&q=720p|&q=480p|&q=360p", "");
            VideoServer videoServer = new VideoServer(RV);
            try {
                String jsoup720 = PatternUtil.getRapidVideoLink(Jsoup.connect(down_link + "&q=720p").get().html());
                videoServer.addOption(new Option(getName(), "720p", jsoup720));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                String jsoup480 = PatternUtil.getRapidVideoLink(Jsoup.connect(down_link + "&q=480p").get().html());
                videoServer.addOption(new Option(getName(), "480p", jsoup480));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                String jsoup360 = PatternUtil.getRapidVideoLink(Jsoup.connect(down_link + "&q=360p").get().html());
                videoServer.addOption(new Option(getName(), "360p", jsoup360));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (videoServer.options.size() > 0)
                return videoServer;
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
