package knf.kuma.videoservers

import android.content.Context
import org.jsoup.Jsoup
import java.net.URLDecoder

class ZippyServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("zippyshare")

    override val name: String
        get() = VideoServer.Names.ZIPPYSHARE

    override val videoServer: VideoServer?
        get() {
            return try {
                val decoded = URLDecoder.decode(baseLink, "utf-8")
                val zi = Jsoup.connect(decoded).timeout(TIMEOUT).get()
                val t = zi.select("meta[property='og:title']").attr("content")
                if (t.trim { it <= ' ' } != "") VideoServer(name, Option(name, null, decoded)) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
}
