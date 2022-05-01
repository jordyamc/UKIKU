package knf.kuma.widgets.emision

import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.preference.PreferenceManager
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.animeinfo.ActivityAnimeMaterial
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PatternUtil
import knf.kuma.database.CacheDB
import xdroid.toaster.Toaster
import java.util.*

class WEListProvider internal constructor(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private val items = mutableListOf<WEListItem>()

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

    private fun populateListItem() {
        items.clear()
        val list = CacheDB.INSTANCE.animeDAO().getByDayDirect(actualDayCode, getBlacklist(context))
        for (obj in list) {
            items.add(WEListItem(obj.key, obj.link, obj.name, obj.aid, PatternUtil.getCover(obj.aid)))
        }
    }

    override fun onCreate() {

    }

    override fun onDataSetChanged() {
        populateListItem()
    }

    override fun onDestroy() {

    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val remoteView = RemoteViews(
                context.packageName, R.layout.item_widget_list)
        try {
            val listItem = items[position]
            remoteView.setTextViewText(R.id.heading, listItem.title)
            remoteView.setTextColor(R.id.heading, getColor(true))
            val clickIntent = if (DesignUtils.isFlat)
                ActivityAnimeMaterial.getSimpleIntent(context, listItem)
            else
                ActivityAnime.getSimpleIntent(context, listItem)
            remoteView.setOnClickPendingIntent(
                R.id.linear,
                PendingIntent.getActivity(
                    context,
                    324 + position,
                    clickIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            remoteView.setInt(R.id.linear, "setBackgroundColor", getColor(false))
        } catch (e: Exception) {
            e.printStackTrace()
            Toaster.toastLong("Error: ${e.message}")
        }

        return remoteView
    }

    private fun getColor(isText: Boolean): Int {
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

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return try {
            items[position].key.toLong()
        } catch (e: Exception) {
            0
        }

    }

    override fun hasStableIds(): Boolean {
        return true
    }

    private fun getBlacklist(context: Context): Set<String> {
        return PreferenceManager.getDefaultSharedPreferences(context).getStringSet("emision_blacklist", LinkedHashSet())
                ?: setOf()
    }

}
