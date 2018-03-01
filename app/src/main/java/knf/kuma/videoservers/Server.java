package knf.kuma.videoservers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Server implements Comparable<Server> {
    int TIMEOUT = 10000;
    Context context;
    String baseLink;
    private VideoServer server;

    public Server(Context context, String baseLink) {
        this.context = context;
        this.baseLink = baseLink;
    }

    private static List<Server> getServers(Context context, String base) {
        return Arrays.asList(
                new FireServer(context, base),
                new HyperionServer(context, base),
                new IzanagiServer(context, base),
                new MegaServer(context, base),
                new OkruServer(context, base),
                new RVServer(context, base),
                new YUServer(context, base)
                //new ZippyServer(context, base)
        );
    }

    public static Server check(Context context, String base) {
        for (Server server : getServers(context, base)) {
            if (server.isValid())
                return server;
        }
        return null;
    }

    public static int findPosition(List<Server> servers, String name) {
        int i = 0;
        for (Server server : servers) {
            if (server.getName().equals(name))
                return i;
            i++;
        }
        return 0;
    }

    public static boolean existServer(List<Server> servers, int position) {
        String name = VideoServer.Names.getDownloadServers()[position - 1];
        for (Server server : servers) {
            if (server.getName().equals(name))
                return true;
        }
        return false;
    }

    public static Server findServer(List<Server> servers, int position) {
        String name = VideoServer.Names.getDownloadServers()[position - 1];
        return servers.get(findPosition(servers, name));
    }

    public static List<String> getNames(List<Server> servers) {
        List<String> names = new ArrayList<>();
        for (Server server : servers) {
            names.add(server.getName());
        }
        return names;
    }

    public abstract boolean isValid();

    public abstract String getName();

    @Nullable
    abstract VideoServer getVideoServer();

    @Nullable
    public VideoServer getLink() {
        if (server == null)
            server = getVideoServer();
        return server;
    }

    @Override
    public int compareTo(@NonNull Server server) {
        return getName().compareTo(server.getName());
    }
}
