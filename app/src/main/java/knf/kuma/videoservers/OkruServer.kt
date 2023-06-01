package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.iterator
import knf.kuma.uagen.randomLatestUA
import knf.kuma.uagen.randomWindowsUA
import knf.kuma.videoservers.VideoServer.Names.OKRU
import kotlinx.coroutines.runBlocking
import org.apache.commons.text.StringEscapeUtils
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.helper.HttpConnection

class OkruServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("ok.ru")

    override val name: String
        get() = OKRU

    override val videoServer: VideoServer?
        get() {
            try {
                val downLink = PatternUtil.extractLink(baseLink)
                val (source) = Regex("data-options=\"(.*?)\"").find(Jsoup.connect(downLink).get().html())?.destructured!!
                val jsonText = StringEscapeUtils.unescapeHtml4(source)
                val videos = JSONObject(JSONObject(jsonText).getJSONObject("flashvars").getString("metadata")).getJSONArray("videos")
                val options = mutableListOf<Option>()
                for(video in videos){
                    val url = video.getString("url")
                    val name = when(video.getString("name")){
                        "mobile" -> "144p"
                        "lowest" -> "240p"
                        "low" -> "360p"
                        "sd" -> "480p"
                        "hd" -> "720p"
                        "full" -> "1080p"
                        "quad" -> "2000p"
                        "ultra" -> "4000p"
                        else -> "Default"
                    }
                    options.add(Option(this.name, name, url, Headers().apply { addHeader("User-Agent", HttpConnection.DEFAULT_UA) }))//mapOf("User-agent" to userAgent)
                }
                check(options.isNotEmpty())
                return VideoServer(OKRU, options, true)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
}
