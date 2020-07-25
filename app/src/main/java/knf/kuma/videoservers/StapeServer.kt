package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.videoservers.VideoServer.Names.STAPE
import org.jsoup.Jsoup

class StapeServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("streamtape.com")

    override val name: String
        get() = STAPE

    override val videoServer: VideoServer?
        get() {
            val downLink = PatternUtil.extractLink(baseLink)
            return try {
                val video = Jsoup.connect(downLink).get().select("#videolink").first()
                val link = "https:${video.text()}&stream=1"
                VideoServer(STAPE, Option(name, null, Jsoup.connect(link).ignoreContentType(true).followRedirects(true).execute().url().toString()))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
}
