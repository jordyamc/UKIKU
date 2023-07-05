package knf.kuma.commons

import android.os.Build
import android.text.Html
import android.util.Log
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import knf.kuma.App
import knf.kuma.pojos.AnimeObject
import java.util.*
import java.util.regex.Pattern


object PatternUtil {
    @Suppress("DEPRECATION")
    fun fromHtml(html: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(html.r("\\\\u", "\\u").r("\\/", "/"), Html.FROM_HTML_MODE_LEGACY).toString()
        else
            Html.fromHtml(html.r("\\\\u", "\\u").r("\\/", "/")).toString()
    }

    fun getLinkNumber(link: String): String {
        val pattern = Pattern.compile("/(\\d+)[/.]")
        val matcher = pattern.matcher(link)
        matcher.find()
        return matcher.group(1)
    }

    fun getRapidLink(link: String): String {
        val pattern = Pattern.compile("value=([\\w#.]+)")
        val matcher = pattern.matcher(link)
        matcher.find()
        return "https://www.rapidvideo.com/e/" + matcher.group(1)
    }

    fun getRapidVideoLink(link: String): String {
        val pattern = Pattern.compile("\"(http.*\\.mp4)\"")
        val matcher = pattern.matcher(link)
        matcher.find()
        return matcher.group(1)
    }

    fun getYULink(link: String): String {
        val pattern = Pattern.compile("\"(.*yourupload.*)\"")
        val matcher = pattern.matcher(link)
        matcher.find()
        return matcher.group(1)
    }

    fun getYUvideoLink(link: String): String {
        val pattern = Pattern.compile("file: ?'(.*vidcache.*mp4)'")
        val matcher = pattern.matcher(link)
        matcher.find()
        return matcher.group(1)
    }

    fun getLinkId(link: String): String {
        val matcher = Pattern.compile("^.*/(.*)-\\d+$").matcher(link)
        matcher.find()
        return matcher.group(1)
    }

    fun getLinkNum(link: String): String {
        val matcher = Pattern.compile("^.*-(\\d+)$").matcher(link)
        matcher.find()
        return matcher.group(1)
    }

    fun getFileName(link: String): String {
        return try {
            val matcher = Pattern.compile("^.*/(.*-\\d+\\.?\\d*)$").matcher(link)
            matcher.find()
            matcher.group(1) + ".mp4"
        } catch (e: Exception) {
            Log.e("Pattern", "No name found in: $link", e)
            "N-F.mp4"
        }

    }

    fun getRootFileName(link: String): String {
        return try {
            val matcher = Pattern.compile("^.*/([a-z\\-\\d]+).*$").matcher(link)
            matcher.find()
            matcher.group(1)
        } catch (e: Exception) {
            Log.e("Pattern", "No name found in: $link", e)
            "N-F"
        }

    }

    fun getNameFromFile(file: String?): String {
        if (file.isNull()) return ""
        val matcher = Pattern.compile("^.*\\$(.*)-\\d+\\.?\\d*\\.mp4$").matcher(file)
        matcher.find()
        return noCrashLet("null/") { matcher.group(1) + "/" }
    }

    fun getNumFromFile(file: String): String {
        val matcher = Pattern.compile("^.*\\$[\\w-]+-(\\d+\\.?\\d*)\\.mp4$").matcher(file)
        matcher.find()
        return matcher.group(1)
    }

    fun getEidFromFile(file: String): String {
        val matcher = Pattern.compile("^(-?\\d+)\\$.*$").matcher(file)
        matcher.find()
        return matcher.group(1)
    }

    fun extractLink(html: String): String {
        val matcher = Pattern.compile("https?://[a-zA-Z0-9.=?/!&#_\\-]+|/[a-zA-Z0-9.=?/!&#_\\-]+").matcher(html)
        matcher.find()
        return matcher.group(0)
    }

    fun extractMangoLink(html: String): String {
        val matcher = Pattern.compile("\"(https.*streamango\\.com[/a-z]+)\"").matcher(html)
        matcher.find()
        return matcher.group(1)
    }

    fun extractMediaLink(html: String): String {
        val matcher = Pattern.compile("www\\.mediafire[a-zA-Z0-a.=?/&%]+").matcher(html)
        matcher.find()
        return "https://" + matcher.group().replace("%2F", "/")
    }

    fun extractOkruLink(html: String): String {
        val matcher = Pattern.compile("\"(https://ok\\.ru.*)\"").matcher(html)
        matcher.find()
        return matcher.group(1)
    }

    fun getAnimeUrl(chapter: String, aid: String): String {
        return "https://www3.animeflv.net/anime/" + aid + chapter.substring(
            chapter.lastIndexOf("/"),
            chapter.lastIndexOf("-")
        )
    }

    fun getCover(aid: String?): String {
        return "https://m.animeflv.net/uploads/animes/covers/$aid.jpg"
    }

    fun getThumb(aid: String?): String {
        return "https://ukiku.app/thumbs/$aid.jpg"
    }

    fun getCoverGlide(aid: String?): GlideUrl {
        return GlideUrl(
            "https://m.animeflv.net/uploads/animes/covers/$aid.jpg",
            LazyHeaders.Builder().apply {
                addHeader("Cookie", BypassUtil.getStringCookie(App.context))
                addHeader("User-Agent", BypassUtil.userAgent)
            }.build()
        )
    }

    fun getBanner(aid: String): String {
        return "https://www3.animeflv.net/uploads/animes/banners/$aid.jpg"
    }

    fun getEpListMap(code: String): HashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        val matcher = Pattern.compile("\\[(\\d+\\.?\\d?),(\\d+)]").matcher(code)
        while (matcher.find()) {
            map[matcher.group(1)] = matcher.group(2)
        }
        return map
    }

    fun isCustomSearch(s: String): Boolean {
        return s.matches("^:[a-z]+:.*$".toRegex())
    }

    fun getCustomSearch(s: String): String {
        val matcher = Pattern.compile("^:[a-z]+:(.*$)").matcher(s)
        matcher.find()
        return matcher.group(1)
    }

    fun getCustomAttr(s: String): String {
        val matcher = Pattern.compile("^:([a-z]+):.*$").matcher(s)
        matcher.find()
        return matcher.group(1)
    }

    fun getEids(chapters: MutableList<AnimeObject.WebInfo.AnimeChapter>): MutableList<String> =
            chapters.map { it.eid }.toMutableList()
}
