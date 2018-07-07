package knf.kuma.videoservers;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jsoup.Jsoup;

import knf.kuma.commons.PatternUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static knf.kuma.videoservers.VideoServer.Names.YOURUPLOAD;

public class YUServer extends Server {
    public YUServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return baseLink.contains("server=yu");
    }

    @Override
    public String getName() {
        return YOURUPLOAD;
    }

    @Nullable
    @Override
    public VideoServer getVideoServer() {
        String frame = baseLink.substring(baseLink.indexOf("'") + 1, baseLink.lastIndexOf("'"));
        String redir_link = Jsoup.parse(frame).select("iframe").first().attr("src");
        Log.e("Redir", redir_link);
        try {
            String yu_link = PatternUtil.getYULink(Jsoup.connect(redir_link).get().html());
            String video_link = PatternUtil.getYUvideoLink(Jsoup.connect(yu_link).get().html());
            OkHttpClient client = new OkHttpClient().newBuilder().followRedirects(false).build();
            Request request = new Request.Builder()
                    .url(video_link)
                    .addHeader("Referer", yu_link)
                    .build();
            Response response = client.newCall(request).execute();
            String ref_video_link = response.header("Location");
            Log.e("YU", ref_video_link);
            response.close();
            return new VideoServer(YOURUPLOAD, new Option(getName(), null, ref_video_link));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}