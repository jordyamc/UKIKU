package knf.kuma.videoservers

import android.content.Context
import android.util.Log
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.urlFixed
import knf.kuma.videoservers.VideoServer.Names.STAPE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
                val link = runBlocking {
                    val html = withContext(Dispatchers.Main) { Unpacker.getHtml(context, downLink) }
                    val doc = Jsoup.parse(html, "https://streamtape.com")
                    doc.select("video[id]").attr("abs:src")
                }
                val videoLink =
                    Jsoup.connect(link).ignoreContentType(true).followRedirects(true).execute().url().toString()
                check(!videoLink.contains("streamtape_do_not_delete.mp4"))
                VideoServer(STAPE, Option(name, null, videoLink))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
}
