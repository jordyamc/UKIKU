package knf.kuma.widgets.emision

import android.content.Context
import android.graphics.Color
import android.preference.PreferenceManager
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.database.CacheDB
import java.util.*

class WEListProvider internal constructor(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private val items = ArrayList<WEListItem>()

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

    init {
        populateListItem()
    }

    private fun populateListItem() {
        items.clear()
        val list = CacheDB.INSTANCE.animeDAO().getByDayDirect(actualDayCode, getBlacklist(context)!!)
        for (obj in list) {
            items.add(WEListItem(obj.key, obj.link!!, obj.name!!, obj.aid!!, obj.img!!))
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
            val clickIntent = ActivityAnime.getSimpleIntent(context, listItem)
            remoteView.setOnClickFillInIntent(R.id.linear, clickIntent)
            remoteView.setInt(R.id.linear, "setBackgroundColor", getColor(false))
        } catch (e: Exception) {
            //e
        }

        return remoteView
    }

    private fun getColor(isText: Boolean): Int {
        val type = PreferenceManager.getDefaultSharedPreferences(context).getString("theme_value", "0")
        return when (type) {
            "1" -> if (isText)
                Color.parseColor("#323232")
            else
                Color.parseColor("#FFFFFFFF")
            "2" -> if (isText)
                Color.parseColor("#bebebe")
            else
                Color.parseColor("#FF424242")
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

    private fun getBlacklist(context: Context): Set<String>? {
        return PreferenceManager.getDefaultSharedPreferences(context).getStringSet("emision_blacklist", LinkedHashSet())
    }

}
