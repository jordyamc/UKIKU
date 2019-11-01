package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.jsoupCookies
import knf.kuma.videoservers.VideoServer.Names.OKRU
import org.json.JSONObject
import org.jsoup.Jsoup

class OkruServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("ok.ru")

    override val name: String
        get() = OKRU

    override val videoServer: VideoServer?
        get() {
            try {
                val downLink = PatternUtil.extractLink(baseLink)
                val response = JSONObject(jsoupCookies("https://worldvideodownload.com/tr/index/getir").data(mutableMapOf<String, String>().apply {
                    put("lang", "en")
                    put("url", downLink)
                }).ignoreContentType(true).post().body().html()).getString("html")
                val document = Jsoup.parse(response)
                val videoServer = VideoServer(OKRU)
                document.select(".row").forEach {
                    videoServer.addOption(Option(OKRU, it.select("#quality").text(), it.select("a").attr("href").replace("\\u003d", "=").replace("\\u0026", "&")))
                }
                return videoServer
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

        }
}
