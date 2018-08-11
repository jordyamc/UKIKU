package knf.kuma.videoservers;

import android.content.Context;

import org.jsoup.Jsoup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;
import knf.kuma.commons.BypassUtil;
import knf.kuma.commons.PatternUtil;

import static knf.kuma.videoservers.VideoServer.Names.MANGO;

public class MangoServer extends Server {
    public MangoServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return baseLink.contains("server=streamango");
    }

    @Override
    public String getName() {
        return MANGO;
    }

    @Nullable
    @Override
    public VideoServer getVideoServer() {
        try {
            String frame = baseLink.substring(baseLink.indexOf("'") + 1, baseLink.lastIndexOf("'"));
            String down_link = Jsoup.parse(frame).select("iframe").first().attr("src");
            String mango_link = PatternUtil.extractMangoLink(Jsoup.connect(down_link).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get().select("script").last().html());
            String html = Jsoup.connect(mango_link).get().html();
            Matcher matcher = Pattern.compile("type:\"video/mp4\",src:d\\('([^']+)',(\\d+)\\)").matcher(html);
            matcher.find();
            String hash = matcher.group(1);
            int key = Integer.parseInt(matcher.group(2));
            String file = KDecoder.Companion.decodeMango(hash, key);
            if (file.isEmpty())
                return null;
            if (file.startsWith("//"))
                file = file.replaceFirst("//", "https://");
            return new VideoServer(MANGO, new Option(getName(), null, file));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String decode(String url, int mask) {
        String key = "=/+9876543210zyxwvutsrqponmlkjihgfedcbaZYXWVUTSRQPONMLKJIHGFEDCBA";
        StringBuffer result = new StringBuffer();
        String u = url.replaceAll("[^A-Za-z0-9\\+\\/\\=]", "");
        int idx = 0;
        while (idx < u.length()) {
            int a = key.indexOf(u.substring(idx, idx + 1));
            idx++;
            int b = key.indexOf(u.substring(idx, idx + 1));
            idx++;
            int c = key.indexOf(u.substring(idx, idx + 1));
            idx++;
            int d = key.indexOf(u.substring(idx, idx + 1));
            idx++;
            int s1 = a << 0x2 | (b >> 0x4) ^ mask;
            result.append(Character.valueOf((char) s1));
            int s2 = b & 0xf << 0x4 | (c >> 0x2);
            if (c != 0x40) {
                result.append(Character.valueOf((char) s2));
            }
            int s3 = c & 0x3 << 0x6 | d;
            if (d != 0x40) {
                result.append(Character.valueOf((char) s3));
            }
        }
        return result.toString();
    }
}