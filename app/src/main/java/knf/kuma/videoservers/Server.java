package knf.kuma.videoservers;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
                new NatsukiServer(context, base),
                new FenixServer(context, base),
                new HyperionServer(context, base),
                new IzanagiServer(context, base),
                new MangoServer(context, base),
                new MegaServer(context, base),
                new OkruServer(context, base),
                new RVServer(context, base),
                new YUServer(context, base),
                new MP4UploadServer(context, base)
        );
    }

    public static Server check(Context context, String base) {
        for (Server server: getServers(context, base)) {
            if (server.isValid())
                return server;
        }
        return null;
    }

    private static int findPosition(List<Server> servers, String name) {
        int i = 0;
        for (Server server: servers) {
            if (server.getName().equals(name))
                return i;
            i++;
        }
        return 0;
    }

    public static boolean existServer(List<Server> servers, int position) {
        String name = VideoServer.Names.getDownloadServers()[position - 1];
        for (Server server: servers) {
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
        for (Server server: servers) {
            names.add(server.getName());
        }
        return names;
    }

    public abstract boolean isValid();

    public abstract String getName();

    @Nullable
    public abstract VideoServer getVideoServer();

    @Nullable
    private VideoServer verify(@Nullable VideoServer videoServer) {
        if (videoServer == null)
            return null;
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true).build();
        for (Option option: new ArrayList<>(videoServer.options))
            try {
                Request.Builder request = new Request.Builder()
                        .url(option.url);
                if (option.headers != null)
                    for (Pair<String, String> pair: option.headers.getHeaders())
                        request.addHeader(pair.first, pair.second);
                Response response = client.newCall(request.build()).execute();
                if (!response.isSuccessful()) {
                    Log.e("Remove Option", "Server: " + option.server + "\nUrl: " + option.url + "\nCode: " + response.code());
                    videoServer.options.remove(option);
                }
                if (response.body() != null)
                    response.close();
            } catch (Exception e) {
                e.printStackTrace();
                videoServer.options.remove(option);
            }
        if (videoServer.options.size() == 0)
            return null;
        return videoServer;
    }

    @Nullable
    public VideoServer getVerified() {
        if (server == null)
            server = verify(getVideoServer());
        return server;
    }

    @Override
    public int compareTo(@NonNull Server server) {
        return getName().compareTo(server.getName());
    }
}
