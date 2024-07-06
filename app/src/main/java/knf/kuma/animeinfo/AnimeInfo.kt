package knf.kuma.animeinfo

import knf.kuma.commons.PatternUtil
import knf.kuma.pojos.AnimeObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class AnimeInfo(code: String) {
    var aid: String? = null
    var title: String? = null
    var sid: String? = null
    var day: AnimeObject.Day? = null
        get() {
            try {
                if (date == null)
                    return AnimeObject.Day.NONE
                val calendar = Calendar.getInstance()
                calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                return when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    2 -> AnimeObject.Day.MONDAY
                    3 -> AnimeObject.Day.TUESDAY
                    4 -> AnimeObject.Day.WEDNESDAY
                    5 -> AnimeObject.Day.THURSDAY
                    6 -> AnimeObject.Day.FRIDAY
                    7 -> AnimeObject.Day.SATURDAY
                    1 -> AnimeObject.Day.SUNDAY
                    else -> AnimeObject.Day.NONE
                }
            } catch (e: Exception) {
                //e.printStackTrace()
                return AnimeObject.Day.NONE
            }
        }
    var epMap: HashMap<String, String>
    private var date: String? = null

    init {
        val matcher = Pattern.compile("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\",?").matcher(code)
        var i = 0
        while (matcher.find()) {
            when (i) {
                0 -> this.aid = matcher.group(1)
                1 -> this.title = PatternUtil.fromHtml(matcher.group(1)).replace("\\", "")
                2 -> this.sid = matcher.group(1)
                3 -> this.date = matcher.group(1)
            }
            i++
        }
        this.day = day
        this.epMap = PatternUtil.getEpListMap(code)
    }
}
