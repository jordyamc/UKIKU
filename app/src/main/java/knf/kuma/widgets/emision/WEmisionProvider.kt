package knf.kuma.widgets.emision

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import knf.kuma.R
import knf.kuma.commons.DesignUtils
import knf.kuma.emision.EmissionActivity
import knf.kuma.emision.EmissionActivityMaterial
import java.util.Calendar

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

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (i in appWidgetIds) {
            val remoteViews = updateWidgetListView(context, i)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.words)
            appWidgetManager.updateAppWidget(i, remoteViews)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun updateWidgetListView(context: Context, appWidgetId: Int): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_emision)
        val svcIntent = Intent(context, WEmissionService::class.java)
        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        svcIntent.data = Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME))
        remoteViews.setRemoteAdapter(R.id.words, svcIntent)
        val clickIntent = if (DesignUtils.isFlat) Intent(
            context,
            EmissionActivityMaterial::class.java
        ) else Intent(context, EmissionActivity::class.java)
        remoteViews.setTextViewText(R.id.title_day, actualDay)
        remoteViews.setTextColor(R.id.title_day, getColor(context, true))
        remoteViews.setOnClickPendingIntent(
            R.id.back_layout,
            PendingIntent.getActivity(
                context,
                555,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        remoteViews.setPendingIntentTemplate(
            R.id.words,
            PendingIntent.getActivity(
                context,
                appWidgetId,
                Intent(context, DesignUtils.infoClass),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        )
        remoteViews.setInt(R.id.back_layout, "setBackgroundColor", getColor(context, false))
        remoteViews.setEmptyView(R.id.words, R.id.empty)
        return remoteViews
    }

    private fun getColor(context: Context, isText: Boolean): Int {
        return when (PreferenceManager.getDefaultSharedPreferences(context).getString("theme_value", "0")) {
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

    companion object {
        fun update(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, WEListProvider::class.java))
                for (id in ids) {
                    manager.notifyAppWidgetViewDataChanged(id, R.id.words)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
