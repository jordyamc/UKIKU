package knf.kuma.widgets.emision;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

import knf.kuma.R;
import knf.kuma.animeinfo.ActivityAnime;
import knf.kuma.database.CacheDB;
import knf.kuma.emision.EmisionActivity;

public class WEmisionProvider extends AppWidgetProvider {

    public static void update(Context context) {
        Intent intent = new Intent(context, WEmisionProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, WEmisionProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int i : appWidgetIds) {
            RemoteViews remoteViews = updateWidgetListView(context,
                    i);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.words);
            appWidgetManager.updateAppWidget(i, remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private RemoteViews updateWidgetListView(Context context, int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget_emision);
        Intent svcIntent = new Intent(context, WEmisionService.class);
        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setRemoteAdapter(R.id.words,
                svcIntent);
        Intent clickIntent = new Intent(context, EmisionActivity.class);
        remoteViews.setTextViewText(R.id.title_day, getActualDay());
        remoteViews.setTextViewText(R.id.title_count, String.valueOf(CacheDB.INSTANCE.animeDAO().getByDayDirect(getActualDayCode(), getBlacklist(context)).size()));
        remoteViews.setOnClickPendingIntent(R.id.back_layout, PendingIntent.getActivity(context, 555, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        remoteViews.setPendingIntentTemplate(R.id.words, PendingIntent.getActivity(context, appWidgetId, new Intent(context, ActivityAnime.class), PendingIntent.FLAG_UPDATE_CURRENT));
        remoteViews.setEmptyView(R.id.words, R.id.empty);
        return remoteViews;
    }

    private String getActualDay() {
        switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "LUNES";
            case Calendar.TUESDAY:
                return "MARTES";
            case Calendar.WEDNESDAY:
                return "MIERCOLES";
            case Calendar.THURSDAY:
                return "JUEVES";
            case Calendar.FRIDAY:
                return "VIERNES";
            case Calendar.SATURDAY:
                return "SABADO";
            case Calendar.SUNDAY:
                return "DOMINGO";
            default:
                return "DESCONOCIDO(LUNES POR DEFECTO)";
        }
    }

    private int getActualDayCode() {
        switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            default:
            case Calendar.MONDAY:
                return 2;
            case Calendar.TUESDAY:
                return 3;
            case Calendar.WEDNESDAY:
                return 4;
            case Calendar.THURSDAY:
                return 5;
            case Calendar.FRIDAY:
                return 6;
            case Calendar.SATURDAY:
                return 7;
            case Calendar.SUNDAY:
                return 1;
        }
    }

    private Set<String> getBlacklist(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getStringSet("emision_blacklist", new LinkedHashSet<String>());
    }

}
