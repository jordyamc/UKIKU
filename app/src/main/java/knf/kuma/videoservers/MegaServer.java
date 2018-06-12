package knf.kuma.videoservers;

import android.content.Context;
import android.support.annotation.Nullable;

import java.net.URLDecoder;

import static knf.kuma.videoservers.VideoServer.Names.MEGA;

public class MegaServer extends Server {
    public MegaServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return baseLink.contains("mega.nz") && !baseLink.contains("embed");
    }

    @Override
    public String getName() {
        return MEGA;
    }

    @Nullable
    @Override
    public VideoServer getVideoServer() {
        try {
            return new VideoServer(MEGA, new Option(getName(), null, URLDecoder.decode(baseLink, "utf-8")));
        } catch (Exception e) {
            return null;
        }
    }
}
