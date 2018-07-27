package knf.kuma.videoservers;

import android.content.Context;
import android.support.annotation.Nullable;

import org.jsoup.Jsoup;

import java.net.URLDecoder;

import static knf.kuma.videoservers.VideoServer.Names.MEGA;

public class MegaServer extends Server {
    private final String DOWNLOAD = "1";
    private final String STREAM = "2";

    public MegaServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return (baseLink.contains("mega.nz") && !baseLink.contains("embed")) || baseLink.contains("server=mega");
    }

    @Override
    public String getName() {
        return MEGA + " " + getType();
    }

    private String getType() {
        if (baseLink.contains("mega.nz") && !baseLink.contains("embed"))
            return DOWNLOAD;
        else
            return STREAM;
    }

    @Nullable
    @Override
    public VideoServer getVideoServer() {
        try {
            if (getType().equals(STREAM)) {
                String frame = baseLink.substring(baseLink.indexOf("'") + 1, baseLink.lastIndexOf("'"));
                String down_link = Jsoup.parse(frame).select("iframe").first().attr("src");
                String link = "https://mega.nz/#" + down_link.substring(down_link.lastIndexOf("!"));
                return new VideoServer(getName(), new Option(getName(), null, link));
            } else
                return new VideoServer(getName(), new Option(getName(), null, URLDecoder.decode(baseLink, "utf-8")));
        } catch (Exception e) {
            return null;
        }
    }
}
