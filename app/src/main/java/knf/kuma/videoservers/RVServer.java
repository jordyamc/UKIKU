package knf.kuma.videoservers;

import android.content.Context;
import android.support.annotation.Nullable;

import org.jsoup.Jsoup;

import static knf.kuma.videoservers.VideoServer.Names.RV;

/**
 * Created by Jordy on 11/01/2018.
 */

public class RVServer extends Server {
    public RVServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return baseLink.contains("rapidvideo");
    }

    @Override
    public String getName() {
        return RV;
    }

    @Nullable
    @Override
    VideoServer getVideoServer() {
        try {
            String frame = baseLink.substring(baseLink.indexOf("'") + 1, baseLink.lastIndexOf("'"));
            String down_link = Jsoup.parse(frame).select("iframe").first().attr("src").replace("&q=720p", "");
            VideoServer videoServer = new VideoServer(RV);

            try {
                String jsoup720 = Jsoup.connect(down_link + "&q=720p").get().select("video source").first().attr("src");
                videoServer.addOption(new Option("720p", jsoup720));
            } catch (Exception e) {
                //
            }

            try {
                String jsoup480 = Jsoup.connect(down_link + "&q=480p").get().select("video source").first().attr("src");
                videoServer.addOption(new Option("480p", jsoup480));
            } catch (Exception e) {
                //
            }

            try {
                String jsoup360 = Jsoup.connect(down_link + "&q=360p").get().select("video source").first().attr("src");
                videoServer.addOption(new Option("360p", jsoup360));
            } catch (Exception e) {
                //
            }
            if (videoServer.options.size() > 0)
                return videoServer;
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
