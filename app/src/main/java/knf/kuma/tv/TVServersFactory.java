package knf.kuma.tv;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v17.leanback.widget.Presenter;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import knf.kuma.commons.BypassUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.DownloadObject;
import knf.kuma.pojos.RecordObject;
import knf.kuma.tv.exoplayer.TVPlayer;
import knf.kuma.tv.streaming.TVServerSelection;
import knf.kuma.tv.streaming.TVServerSelectionFragment;
import knf.kuma.videoservers.Option;
import knf.kuma.videoservers.Server;
import knf.kuma.videoservers.VideoServer;
import xdroid.toaster.Toaster;

public class TVServersFactory {
    public static int REQUEST_CODE_LIST = 4456;
    public static int REQUEST_CODE_OPTION = 6157;

    private Activity activity;
    private String url;
    private AnimeObject.WebInfo.AnimeChapter chapter;
    private DownloadObject downloadObject;
    private ServersInterface serversInterface;

    private List<Server> servers = new ArrayList<>();

    private VideoServer current;

    private Presenter.ViewHolder viewHolder;

    private TVServersFactory(Activity activity, String url, AnimeObject.WebInfo.AnimeChapter chapter, Presenter.ViewHolder viewHolder, ServersInterface serversInterface) {
        this.activity = activity;
        this.url = url;
        this.chapter = chapter;
        this.downloadObject = DownloadObject.fromChapter(chapter, false);
        this.viewHolder = viewHolder;
        this.serversInterface = serversInterface;
    }

    public static void start(final Activity activity, final String url, final AnimeObject.WebInfo.AnimeChapter chapter, final ServersInterface serversInterface) {
        start(activity, url, chapter, null, serversInterface);
    }

    public static void start(final Activity activity, final String url, final AnimeObject.WebInfo.AnimeChapter chapter, Presenter.ViewHolder viewHolder, final ServersInterface serversInterface) {
        final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .content("Obteniendo servidores")
                .progress(true, 0)
                .build();
        dialog.show();
        AsyncTask.execute(() -> {
            TVServersFactory factory = new TVServersFactory(activity, url, chapter, viewHolder, serversInterface);
            serversInterface.onReady(factory);
            factory.get(dialog);
        });
    }

    public void showServerList() {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (servers.size() == 0) {
                    Toaster.toast("Sin servidores disponibles");
                    serversInterface.onFinish(false, false);
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList(TVServerSelectionFragment.SERVERS_DATA, (ArrayList<String>) Server.getNames(servers));
                    activity.startActivityForResult(new Intent(activity, TVServerSelection.class)
                            .putExtras(bundle), REQUEST_CODE_LIST);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    public void analizeServer(int position) {
        final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .content("Obteniendo link")
                .progress(true, 0)
                .build();
        dialog.show();
        AsyncTask.execute(() -> {
            try {
                String text = servers.get(position).getName();
                final VideoServer server = servers.get(position).getVideoServer();
                dialog.dismiss();
                if (server == null && servers.size() == 1) {
                    Toaster.toast("Error en servidor, intente mas tarde");
                    serversInterface.onFinish(false, false);
                } else if (server == null) {
                    Toaster.toast("Error en servidor");
                    showServerList();
                } else if (server.options.size() == 0) {
                    Toaster.toast("Error en servidor");
                    showServerList();
                } else if (server.haveOptions()) {
                    showOptions(server);
                } else {
                    switch (text.toLowerCase()) {
                        case "mega":
                            Toaster.toast("No se puede usar Mega en TV");
                            showServerList();
                            break;
                        default:
                            startStreaming(server.getOption());
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void analizeOption(int position) {
        if (current != null)
            startStreaming(current.options.get(position));
    }

    private void showOptions(final VideoServer server) {
        this.current = server;
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(TVServerSelectionFragment.VIDEO_DATA, (ArrayList<String>) Option.getNames(server.options));
        bundle.putString("name", server.name);
        activity.startActivityForResult(new Intent(activity, TVServerSelection.class)
                .putExtras(bundle), REQUEST_CODE_OPTION);
    }

    private void startStreaming(Option option) {
        CacheDB.INSTANCE.chaptersDAO().addChapter(chapter);
        CacheDB.INSTANCE.recordsDAO().add(RecordObject.fromChapter(chapter));
        Answers.getInstance().logCustom(new CustomEvent("Streaming").putCustomAttribute("Server", option.server));
        activity.startActivity(new Intent(activity, TVPlayer.class)
                .putExtra("url", option.url)
                .putExtra("title", downloadObject.name)
                .putExtra("chapter", downloadObject.chapter));
        serversInterface.onFinish(false, true);
    }

    public Presenter.ViewHolder getViewHolder() {
        return viewHolder;
    }

    public void get(MaterialDialog dialog) {
        try {
            Log.e("Url", url);
            Document main = Jsoup.connect(url).timeout(5000).cookies(BypassUtil.getMapCookie(activity)).userAgent(BypassUtil.userAgent).get();
            Elements descargas = main.select("table.RTbl.Dwnl").first().select("a.Button.Sm.fa-download");
            List<Server> servers = new ArrayList<>();
            for (Element e : descargas) {
                String z = e.attr("href");
                z = z.substring(z.lastIndexOf("http"));
                Server server = Server.check(activity, z);
                if (server != null)
                    servers.add(server);
            }
            Elements s_script = main.select("script");
            String j = "";
            for (Element element : s_script) {
                String s_el = element.outerHtml();
                if (s_el.contains("var video = [];")) {
                    j = s_el;
                    break;
                }
            }
            String[] parts = j.substring(j.indexOf("var video = [];") + 14, j.indexOf("$(document).ready(function()")).split("video\\[[^a-z]*\\]");
            for (String baseLink : parts) {
                Server server = Server.check(activity, baseLink);
                if (server != null)
                    servers.add(server);
            }
            Collections.sort(servers);
            this.servers = servers;
            dialog.dismiss();
            showServerList();
        } catch (Exception e) {
            e.printStackTrace();
            this.servers = new ArrayList<>();
            dialog.dismiss();
            serversInterface.onFinish(false, false);
        }
    }

    public interface ServersInterface {
        void onReady(TVServersFactory serversFactory);

        void onFinish(boolean started, boolean success);
    }
}
