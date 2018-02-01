package knf.kuma.videoservers;

import android.content.Context;
import android.support.annotation.Nullable;

import java.net.URLDecoder;

import static knf.kuma.videoservers.VideoServer.Names.MEGA;

/**
 * Created by Jordy on 11/01/2018.
 */

public class MegaServer extends Server {
    public MegaServer(Context context, String baseLink) {
        super(context, baseLink);
    }

    @Override
    public boolean isValid() {
        return baseLink.contains("mega.nz");
    }

    @Override
    public String getName() {
        return MEGA;
    }

    @Nullable
    @Override
    VideoServer getVideoServer() {
        try {
            return new VideoServer(MEGA, new Option(null, URLDecoder.decode(baseLink, "utf-8")));
        } catch (Exception e) {
            return null;
        }
    }
}
