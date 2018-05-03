package knf.kuma.videoservers;

import android.content.Context;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URLDecoder;

public class ZippyServer extends Server {

    public ZippyServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return baseLink.contains("zippyshare");
    }

    @Override
    public String getName() {
        return VideoServer.Names.ZIPPYSHARE;
    }

    @Override
    VideoServer getVideoServer() {
        try {
            String decoded=URLDecoder.decode(baseLink, "utf-8");
            Document zi = Jsoup.connect(decoded).timeout(TIMEOUT).get();
            String t = zi.select("meta[property='og:title']").attr("content");
            if (!t.trim().equals(""))
                return new VideoServer(getName(), new Option(getName(), null, decoded));
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
