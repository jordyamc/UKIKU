package knf.kuma.videoservers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
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

import knf.kuma.commons.BypassUtil;
import knf.kuma.commons.CastUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.downloadservice.DownloadService;
import knf.kuma.downloadservice.FileAccessHelper;
import knf.kuma.player.ExoPlayer;
import knf.kuma.pojos.DownloadObject;
import xdroid.toaster.Toaster;

public class ServersFactory {
    private Context context;
    private String url;
    private DownloadObject downloadObject;
    private boolean isStream;
    private ServersInterface serversInterface;

    private List<Server> servers = new ArrayList<>();
    private int selected = 0;

    public ServersFactory(Context context, String url, DownloadObject downloadObject, boolean isStream, ServersInterface serversInterface) {
        this.context = context;
        this.url = url;
        this.downloadObject = downloadObject;
        this.isStream = isStream;
        this.serversInterface = serversInterface;
    }

    public static void start(final Context context, final String url, final DownloadObject downloadObject, final boolean isStream, final ServersInterface serversInterface) {
        final MaterialDialog dialog = new MaterialDialog.Builder(context)
                .content("Obteniendo servidores")
                .progress(true, 0)
                .build();
        dialog.show();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ServersFactory factory = new ServersFactory(context, url, downloadObject, isStream, serversInterface);
                factory.get(dialog);
            }
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
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (servers.size() == 0) {
                        Toaster.toast("Sin servidores disponibles");
                        serversInterface.onFinish(false, false);
                    } else {
                        MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                                .title("Selecciona servidor")
                                .items(Server.getNames(servers))
                                .autoDismiss(true)
                                .itemsCallbackSingleChoice(selected, new MaterialDialog.ListCallbackSingleChoice() {
                                    @Override
                                    public boolean onSelection(MaterialDialog d, View itemView, int which, final CharSequence text) {
                                        selected = which;
                                        d.dismiss();
                                        final MaterialDialog dialog = new MaterialDialog.Builder(context)
                                                .content("Obteniendo link")
                                                .progress(true, 0)
                                                .build();
                                        dialog.show();
                                        AsyncTask.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    final VideoServer server = servers.get(selected).getVideoServer();
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
                                                        showOptions(server, false);
                                                    } else {
                                                        switch (text.toString().toLowerCase()) {
                                                            case "zippyshare":
                                                                ZippyHelper.calculate(context, server.getOption().url, new ZippyHelper.OnZippyResult() {
                                                                    @Override
                                                                    public void onSuccess(ZippyHelper.ZippyObject object) {
                                                                        startDownload(server.getOption(), object);
                                                                    }

                                                                    @Override
                                                                    public void onError() {
                                                                        Toaster.toast("Error en servidor");
                                                                        showServerList();
                                                                    }
                                                                });
                                                                break;
                                                            case "mega":
                                                                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(server.getOption().url)));
                                                                break;
                                                            default:
                                                                if (isStream) {
                                                                    startStreaming(server.getOption());
                                                                } else {
                                                                    startDownload(server.getOption());
                                                                }
                                                                break;
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                        return true;
                                    }
                                }).positiveText("INICIAR")
                                .negativeText("CANCELAR")
                                .onNegative(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        serversInterface.onFinish(false, false);
                                    }
                                }).cancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        serversInterface.onFinish(false, false);
                                    }
                                });
                        if (isStream && CastUtil.get().connected())
                            builder.neutralText("CAST")
                                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull final MaterialDialog d, @NonNull DialogAction which) {
                                            selected = d.getSelectedIndex();
                                            d.dismiss();
                                            final MaterialDialog dialog = new MaterialDialog.Builder(context)
                                                    .content("Obteniendo link")
                                                    .progress(true, 0)
                                                    .build();
                                            dialog.show();
                                            AsyncTask.execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        final VideoServer server = servers.get(selected).getVideoServer();
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
                                                                case "zippyshare":
                                                                    Toaster.toast("No soportado en CAST");
                                                                    showServerList();
                                                                    break;
                                                                case "mega":
                                                                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(server.getOption().url)));
                                                                    break;
                                                                default:
                                                                    serversInterface.onCast(server.getOption().url);
                                                                    break;
                                                            }
                                                        }
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });
                                        }
                                    });
                        builder.build().show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void showOptions(final VideoServer server, final boolean isCast) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    new MaterialDialog.Builder(context)
                            .title(server.name)
                            .items(Option.getNames(server.options))
                            .autoDismiss(true)
                            .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                    dialog.dismiss();
                                    if (isCast) {
                                        serversInterface.onCast(server.options.get(which).url);
                                    } else if (isStream) {
                                        startStreaming(server.options.get(which));
                                    } else {
                                        startDownload(server.options.get(which));
                                    }
                                    return true;
                                }
                            })
                            .positiveText("INICIAR")
                            .negativeText("ATRAS")
                            .cancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    Log.e("Download", "ShowList from canceled");
                                    showServerList();
                                }
                            }).build().show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startStreaming(Option option) {
        Answers.getInstance().logCustom(new CustomEvent("Streaming").putCustomAttribute("Server", option.server));
        if (PreferenceManager.getDefaultSharedPreferences(context).getString("player_type","0").equals("0")){
            context.startActivity(new Intent(context, ExoPlayer.class).setData(Uri.parse(option.url)).putExtra("title", downloadObject.title));
        }else {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.parse(option.url), "video/mp4")
                    .putExtra("title", downloadObject.title);
            context.startActivity(intent);
        }
        serversInterface.onFinish(false, true);
    }

    private void startDownload(Option option) {
        Answers.getInstance().logCustom(new CustomEvent("Download").putCustomAttribute("Server", option.server));
        downloadObject.link = option.url;
        CacheDB.INSTANCE.downloadsDAO().insert(downloadObject);
        ContextCompat.startForegroundService(context, new Intent(context, DownloadService.class).putExtra("eid", downloadObject.eid).setData(Uri.parse(option.url)));
        serversInterface.onFinish(true, true);
    }

    private void startDownload(Option option,ZippyHelper.ZippyObject object) {
        downloadObject.link = object.download_url;
        CacheDB.INSTANCE.downloadsDAO().insert(downloadObject);
        CookieConstructor constructor=object.cookieConstructor;
        Intent intent=new Intent(context,DownloadService.class)
                .setData(Uri.parse(option.url))
                .putExtra("eid", downloadObject.eid)
                .putExtra("constructor",true)
                .putExtra("cookie",constructor.getCookie())
                .putExtra("referer",constructor.getReferer())
                .putExtra("ua",constructor.getUseAgent());
        ContextCompat.startForegroundService(context, intent);
        serversInterface.onFinish(true, true);
    }

    public void get(MaterialDialog dialog) {
        try {
            Log.e("Url", url);
            Document main = Jsoup.connect(url).timeout(5000).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get();
            Elements descargas = main.select("table.RTbl.Dwnl").first().select("a.Button.Sm.fa-download");
            List<Server> servers = new ArrayList<>();
            for (Element e : descargas) {
                String z = e.attr("href");
                z = z.substring(z.lastIndexOf("http"));
                Server server = Server.check(context, z);
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
