package knf.kuma.videoservers;

import android.content.Context;

import org.jsoup.Jsoup;

import androidx.annotation.Nullable;
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
                down_link = PatternUtil.getRapidLink(down_link);
            boolean needPost = Jsoup.connect(down_link).get().html().contains("Please click on this button to open this video");
            VideoServer videoServer = new VideoServer(RV);
            try {
                String jsoup720 = PatternUtil.getRapidVideoLink(getHtml(down_link + "&q=720p", needPost));
                videoServer.addOption(new Option(getName(), "720p", jsoup720));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                String jsoup480 = PatternUtil.getRapidVideoLink(getHtml(down_link + "&q=480p", needPost));
                videoServer.addOption(new Option(getName(), "480p", jsoup480));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                String jsoup360 = PatternUtil.getRapidVideoLink(getHtml(down_link + "&q=360p", needPost));
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

    private String getHtml(String link, boolean needPost) {
        try {
            if (needPost)
                return Jsoup.connect(link + "#").data("block", "1").post().html();
            else
                return Jsoup.connect(link).get().html();
        } catch (Exception e) {
            return "";
        }
    }
}
