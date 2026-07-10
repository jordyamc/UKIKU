package knf.kuma.commons

import android.os.Build
import android.text.Html
import android.util.Log
import java.util.regex.Pattern


object PatternUtil {
    @Suppress("DEPRECATION")
    fun fromHtml(html: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(html.r("\\\\u", "\\u").r("\\/", "/"), Html.FROM_HTML_MODE_LEGACY).toString()
        else
            Html.fromHtml(html.r("\\\\u", "\\u").r("\\/", "/")).toString()
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

    fun getYUvideoLink(link: String): String {
        val pattern = Pattern.compile("file: ?'(.*vidcache.*mp4)'")
        val matcher = pattern.matcher(link)
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
        file?: return ""
        return file.substringAfterLast("$").substringBeforeLast("-") + "/"
    }

    fun getNumFromFile(file: String): String {
        return file.substringAfterLast("-").replace(".mp4", "")
    }

    fun getEidFromFile(file: String): String {
        return file.substringBefore("$")
    }

    fun extractLink(html: String): String {
        val matcher = Pattern.compile("https?://[a-zA-Z0-9.=?/!&#_\\-]+|/[a-zA-Z0-9.=?/!&#_\\-]+").matcher(html)
        matcher.find()
        return matcher.group(0)
    }

    fun extractMediaLink(html: String): String {
        val matcher = Pattern.compile("www\\.mediafire[a-zA-Z0-a.=?/&%]+").matcher(html)
        matcher.find()
        return "https://" + matcher.group().replace("%2F", "/")
    }

    fun getAnimeUrl(chapter: String, aid: String): String {
        return "https://www3.animeflv.net/anime/" + aid + chapter.substring(
            chapter.lastIndexOf("/"),
            chapter.lastIndexOf("-")
        )
    }

    fun getCover(aid: String?): String {
        return "https://cdn.animeav1.com/covers/$aid.jpg"
    }

    fun getEpListMap(code: String): HashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        val matcher = Pattern.compile("\\[(\\d+\\.?\\d?),(\\d+)]").matcher(code)
        while (matcher.find()) {
            map[matcher.group(1)] = matcher.group(2)
        }
        return map
    }
}
