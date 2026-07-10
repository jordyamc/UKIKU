package knf.kuma.widgets.emision

import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.JsExtractor
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.DirectoryAV1Calendar
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class WEListProvider internal constructor(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var items = listOf<DirectoryAV1Calendar>()

    private val actualDayCode: Int
        get() {
            return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
        }

    override fun onCreate() {

    }

    override fun onDataSetChanged() {
        runBlocking {
            val data = JsExtractor.processLink("https://animeav1.com/horario")
            if (data != null) {
                val blacklist = CacheDB.INSTANCE.calendarBlacklistDAO().allAids
                val allList = mutableListOf<DirectoryAV1Calendar>()
                for (i in 0 until data.length()) {
                    allList.add(DirectoryAV1Calendar.fromJson(data.getJSONObject(i)))
                }
                if (allList.isNotEmpty()) {
                    items = allList.filter { it.day == actualDayCode && it.aid !in blacklist }.sortedBy { it.name }
                }
            }
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val remoteView = RemoteViews(context.packageName, R.layout.item_widget_list)
        try {
            val listItem = items[position]
            remoteView.setTextViewText(R.id.item_heading, listItem.name)
            try {
                val bitmap = Glide.with(context).asBitmap().load(listItem.imageUrl)
                    .apply(
                        RequestOptions()
                            .override(120, 120)
                            .centerCrop()
                            .error(R.drawable.ic_faq)
                    )
                    .submit()
                    .get()
                remoteView.setImageViewBitmap(R.id.item_image, bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                remoteView.setImageViewResource(R.id.item_image, R.drawable.ic_faq)
            }
            remoteView.setTextColor(R.id.item_heading, getColor(true))
            val clickIntent = ActivityAnime.getSimpleIntent(listItem)
            remoteView.setOnClickFillInIntent(R.id.linear, clickIntent)
            remoteView.setInt(R.id.linear, "setBackgroundColor", getColor(false))
        } catch (e: Exception) {
            e.printStackTrace()
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
            items[position].aid.toLong()
        } catch (e: Exception) {
            position.toLong()
        }

    }

    override fun hasStableIds(): Boolean {
        return true
    }

}
