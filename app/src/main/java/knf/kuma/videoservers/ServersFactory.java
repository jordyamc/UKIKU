package knf.kuma.videoservers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import knf.kuma.BuildConfig;
import knf.kuma.commons.BypassUtil;
import knf.kuma.commons.CastUtil;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.download.DownloadManager;
import knf.kuma.download.DownloadService;
import knf.kuma.download.FileAccessHelper;
import knf.kuma.player.ExoPlayer;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.DownloadObject;
import knf.kuma.pojos.QueueObject;
import knf.kuma.queue.QueueManager;
import xdroid.toaster.Toaster;

public class ServersFactory {
    private Context context;
    private String url;
    private AnimeObject.WebInfo.AnimeChapter chapter;
    private DownloadObject downloadObject;
    private boolean isStream;
    private ServersInterface serversInterface;

    private List<Server> servers = new ArrayList<>();
    private int selected = 0;

    private ServersFactory(Context context, String url, AnimeObject.WebInfo.AnimeChapter chapter, boolean isStream, boolean addQueue, ServersInterface serversInterface) {
        this.context = context;
        this.url = url;
        this.chapter = chapter;
        this.downloadObject = DownloadObject.fromChapter(chapter, addQueue);
        this.isStream = isStream;
        this.serversInterface = serversInterface;
    }

    private ServersFactory(Context context, String url, DownloadObject downloadObject, boolean isStream, ServersInterface serversInterface) {
        this.context = context;
        this.url = url;
        this.downloadObject = downloadObject;
        this.isStream = isStream;
        this.serversInterface = serversInterface;
    }

    public static void start(final Context context, final String url, final AnimeObject.WebInfo.AnimeChapter chapter, final boolean isStream, final boolean addQueue, final ServersInterface serversInterface) {
        final MaterialDialog dialog = new MaterialDialog.Builder(context)
                .content("Obteniendo servidores")
                .progress(true, 0)
                .build();
        dialog.show();
        AsyncTask.execute(() -> {
            ServersFactory factory = new ServersFactory(context, url, chapter, isStream, addQueue, serversInterface);
            factory.get(dialog);
        });
    }

    public static void start(final Context context, final String url, final AnimeObject.WebInfo.AnimeChapter chapter, final boolean isStream, final ServersInterface serversInterface) {
        start(context, url, chapter, isStream, false, serversInterface);
    }

    public static void start(final Context context, final String url, final DownloadObject downloadObject, final boolean isStream, final ServersInterface serversInterface) {
        final MaterialDialog dialog = new MaterialDialog.Builder(context)
                .content("Obteniendo servidores")
                .progress(true, 0)
                .build();
        dialog.show();
        AsyncTask.execute(() -> {
            ServersFactory factory = new ServersFactory(context, url, downloadObject, isStream, serversInterface);
            factory.get(dialog);
        });
    }

    public static void startPlay(Context context, String title, String file_name) {
        File file = FileAccessHelper.INSTANCE.getFile(file_name);
        if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type", "0").equals("0")) {
            context.startActivity(new Intent(context, ExoPlayer.class).setData(Uri.fromFile(file)).putExtra("isFile", true).putExtra("title", title));
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, FileAccessHelper.INSTANCE.getDataUri(file_name))
                    .setDataAndType(FileAccessHelper.INSTANCE.getDataUri(file_name), "video/mp4")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    .putExtra("title", title);
            context.startActivity(intent);
        }
    }

    private static String getEpTitle(String title, String file) {
        return title + " " + file.substring(file.lastIndexOf("-") + 1, file.lastIndexOf("."));
    }

    public static PendingIntent getPlayIntent(Context context, String title, String file_name) {
        File file = FileAccessHelper.INSTANCE.getFile(file_name);
        if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type", "0").equals("0")) {
            return PendingIntent.getActivity(context, Math.abs(file_name.hashCode()), new Intent(context, ExoPlayer.class).setData(Uri.fromFile(file)).putExtra("isFile", true).putExtra("title", getEpTitle(title, file_name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, FileAccessHelper.INSTANCE.getDataUri(file_name))
                    .setDataAndType(FileAccessHelper.INSTANCE.getDataUri(file_name), "video/mp4")
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("title", getEpTitle(title, file_name));
            return PendingIntent.getActivity(context, Math.abs(file_name.hashCode()), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    private void showServerList() {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (servers.size() == 0) {
                    Toaster.toast("Sin servidores disponibles");
                    serversInterface.onFinish(false, false);
                } else {
                    MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                            .title("Selecciona servidor")
                            .items(Server.getNames(servers))
                            .autoDismiss(true)
                            .itemsCallbackSingleChoice(selected, (d, itemView, which, text) -> {
                                selected = which;
                                d.dismiss();
                                final MaterialDialog dialog = new MaterialDialog.Builder(context)
                                        .content("Obteniendo link")
                                        .progress(true, 0)
                                        .build();
                                dialog.show();
                                AsyncTask.execute(() -> {
                                    try {
                                        final VideoServer server = servers.get(selected).getVerified();
                                        dialog.dismiss();
                                        if (server == null && servers.size() == 1) {
                                            Toaster.toast("Error en servidor, intente mas tarde");
                                            serversInterface.onFinish(false, false);
                                        } else if (server == null) {
                                            servers.remove(selected);
                                            selected = 0;
                                            Toaster.toast("Error en servidor");
                                            showServerList();
                                        } else if (server.options.size() == 0) {
                                            servers.remove(selected);
                                            selected = 0;
                                            Toaster.toast("Error en servidor");
                                            showServerList();
                                        } else if (server.haveOptions()) {
                                            showOptions(server, false);
                                        } else {
                                            switch (text.toString().toLowerCase()) {
                                                case "mega 1":
                                                case "mega 2":
                                                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(server.getOption().url)));
                                                    serversInterface.onFinish(false, false);
                                                    break;
                                                default:
                                                    if (isStream)
                                                        startStreaming(server.getOption());
                                                    else
                                                        startDownload(server.getOption());
                                                    break;
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                                return true;
                            }).positiveText(downloadObject.addQueue ? "AÑADIR" : "INICIAR")
                            .negativeText("CANCELAR")
                            .onNegative((dialog, which) -> serversInterface.onFinish(false, false)).cancelListener(dialog -> serversInterface.onFinish(false, false));
                    if (isStream && CastUtil.get().connected())
                        builder.neutralText("CAST")
                                .onNeutral((d, which) -> {
                                    selected = d.getSelectedIndex();
                                    d.dismiss();
                                    final MaterialDialog dialog = new MaterialDialog.Builder(context)
                                            .content("Obteniendo link")
                                            .progress(true, 0)
                                            .build();
                                    dialog.show();
                                    AsyncTask.execute(() -> {
                                        try {
                                            final VideoServer server = servers.get(selected).getVerified();
                                            dialog.dismiss();
                                            if (server == null && servers.size() == 1) {
                                                Toaster.toast("Error en servidor, intente mas tarde");
                                                serversInterface.onFinish(false, false);
                                            } else if (server == null) {
                                                Toaster.toast("Error en servidor");
                                                showServerList();
                                            } else if (server.haveOptions()) {
                                                showOptions(server, true);
                                            } else {
                                                switch (Server.getNames(servers).get(d.getSelectedIndex()).toLowerCase()) {
                                                    case "mega 1":
                                                    case "mega 2":
                                                    case "zippyshare":
                                                        Toaster.toast("No soportado en CAST");
                                                        showServerList();
                                                        break;
                                                    default:
                                                        serversInterface.onCast(server.getOption().url);
                                                        break;
                                                }
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                                });
                    builder.build().show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void showOptions(final VideoServer server, final boolean isCast) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                new MaterialDialog.Builder(context)
                        .title(server.name)
                        .items(Option.getNames(server.options))
                        .autoDismiss(true)
                        .itemsCallbackSingleChoice(0, (dialog, itemView, which, text) -> {
                            dialog.dismiss();
                            if (isCast) {
                                serversInterface.onCast(server.options.get(which).url);
                            } else if (isStream) {
                                startStreaming(server.options.get(which));
                            } else {
                                startDownload(server.options.get(which));
                            }
                            return true;
                        })
                        .positiveText(downloadObject.addQueue ? "AÑADIR" : "INICIAR")
                        .negativeText("ATRAS")
                        .cancelListener(dialog -> {
                            Log.e("Download", "ShowList from canceled");
                            showServerList();
                        }).build().show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void startStreaming(Option option) {
        if (chapter != null && downloadObject.addQueue) {
            QueueManager.add(Uri.parse(option.url), false, chapter);
        } else {
            Answers.getInstance().logCustom(new CustomEvent("Streaming").putCustomAttribute("Server", option.server));
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type", "0").equals("0")) {
                context.startActivity(new Intent(context, ExoPlayer.class).setData(Uri.parse(option.url)).putExtra("title", downloadObject.title));
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(Uri.parse(option.url), "video/mp4")
                        .putExtra("title", downloadObject.title);
                context.startActivity(intent);
            }
        }
        serversInterface.onFinish(false, true);
    }

    private void startDownload(Option option) {
        if (BuildConfig.DEBUG) Log.e("Download " + option.server, option.url);
        if (chapter != null && CacheDB.INSTANCE.queueDAO().isInQueue(chapter.eid))
            CacheDB.INSTANCE.queueDAO().add(new QueueObject(Uri.fromFile(FileAccessHelper.INSTANCE.getFile(chapter.getFileName())), true, chapter));
        Answers.getInstance().logCustom(new CustomEvent("Download").putCustomAttribute("Server", option.server));
        downloadObject.link = option.url;
        downloadObject.headers = option.headers;
        if (PrefsUtil.getDownloaderType() == 0) {
            CacheDB.INSTANCE.downloadsDAO().insert(downloadObject);
            ContextCompat.startForegroundService(context, new Intent(context, DownloadService.class).putExtra("eid", downloadObject.eid).setData(Uri.parse(option.url)));
            serversInterface.onFinish(true, true);
        } else
            serversInterface.onFinish(true, DownloadManager.start(downloadObject));
    }

    public void get(MaterialDialog dialog) {
        try {
            Document main = Jsoup.connect(url).timeout(5000).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get();
            Elements descargas = main.select("table.RTbl.Dwnl").first().select("a.Button.Sm.fa-download");
            List<Server> servers = new ArrayList<>();
            for (Element e: descargas) {
                String z = e.attr("href");
                z = z.substring(z.lastIndexOf("http"));
                Server server = Server.check(context, z);
                if (server != null)
                    servers.add(server);
            }
            Elements s_script = main.select("script");
            String j = "";
            for (Element element: s_script) {
                String s_el = element.outerHtml();
                if (s_el.contains("var video = [];")) {
                    j = s_el;
                    break;
                }
            }
            String[] parts = j.substring(j.indexOf("var video = [];") + 14, j.indexOf("$(document).ready(function()")).split("video\\[[^a-z]*\\]");
            for (String baseLink: parts) {
                Server server = Server.check(context, baseLink);
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
        void onFinish(boolean started, boolean success);

        void onCast(String url);
    }
}
