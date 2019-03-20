package knf.kuma.widgets.emision

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.preference.PreferenceManager
import android.widget.RemoteViews
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.database.CacheDB
import knf.kuma.emision.EmisionActivity
import java.util.*

class WEmisionProvider : AppWidgetProvider() {

    private val actualDay: String
        get() {
            return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "LUNES"
                Calendar.TUESDAY -> "MARTES"
                Calendar.WEDNESDAY -> "MIERCOLES"
                Calendar.THURSDAY -> "JUEVES"
                Calendar.FRIDAY -> "VIERNES"
                Calendar.SATURDAY -> "SABADO"
                Calendar.SUNDAY -> "DOMINGO"
                else -> "DESCONOCIDO(LUNES POR DEFECTO)"
            }
        }

    private val actualDayCode: Int
        get() {
            return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 2
                Calendar.TUESDAY -> 3
                Calendar.WEDNESDAY -> 4
                Calendar.THURSDAY -> 5
                Calendar.FRIDAY -> 6
                Calendar.SATURDAY -> 7
                Calendar.SUNDAY -> 1
                else -> 2
            }
        }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (i in appWidgetIds) {
            val remoteViews = updateWidgetListView(context,
                    i)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.words)
            appWidgetManager.updateAppWidget(i, remoteViews)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun updateWidgetListView(context: Context, appWidgetId: Int): RemoteViews {
        val remoteViews = RemoteViews(context.packageName,
                R.layout.widget_emision)
        val svcIntent = Intent(context, WEmissionService::class.java)
        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        svcIntent.data = Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME))
        remoteViews.setRemoteAdapter(R.id.words, svcIntent)
        val clickIntent = Intent(context, EmisionActivity::class.java)
        remoteViews.setTextViewText(R.id.title_day, actualDay)
        remoteViews.setTextColor(R.id.title_day, getColor(context, true))
        remoteViews.setTextViewText(R.id.title_count, CacheDB.INSTANCE.animeDAO().getByDayDirect(actualDayCode, getBlacklist(context)).size.toString())
        remoteViews.setTextColor(R.id.title_count, getColor(context, true))
        remoteViews.setOnClickPendingIntent(R.id.back_layout, PendingIntent.getActivity(context, 555, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT))
        remoteViews.setInt(R.id.back_layout, "setBackgroundColor", getColor(context, false))
        remoteViews.setPendingIntentTemplate(R.id.words, PendingIntent.getActivity(context, appWidgetId, Intent(context, ActivityAnime::class.java), PendingIntent.FLAG_UPDATE_CURRENT))
        remoteViews.setEmptyView(R.id.words, R.id.empty)
        return remoteViews
    }

    private fun getColor(context: Context, isText: Boolean): Int {
        val type = PreferenceManager.getDefaultSharedPreferences(context).getString("theme_value", "0")
        return when (type) {
            "1" -> if (isText)
                Color.parseColor("#323232")
            else
                Color.parseColor("#FFFFFF")
            "2" -> if (isText)
                Color.parseColor("#bebebe")
            else
                Color.parseColor("#282828")
            else -> if (isText)
                Color.parseColor("#323232")
            else
                Color.parseColor("#FFFFFFFF")
        }
    }

    private fun getBlacklist(context: Context): Set<String> {
        return PreferenceManager.getDefaultSharedPreferences(context).getStringSet("emision_blacklist", LinkedHashSet())
                ?: setOf()
    }

    companion object {

        fun update(context: Context?) {
            if (context == null) return
            val intent = Intent(context, WEmisionProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, WEmisionProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

}
