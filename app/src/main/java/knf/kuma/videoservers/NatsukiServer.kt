package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.PatternUtil
import knf.kuma.videoservers.VideoServer.Names.NATSUKI
import org.json.JSONObject
import org.jsoup.Jsoup

class NatsukiServer internal constructor(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("s=natsuki")

    override val name: String
        get() = NATSUKI

    override val videoServer: VideoServer?
        get() {
            return try {
                val downLink = PatternUtil.extractLink(baseLink)
                val link = JSONObject(Jsoup.connect(downLink.replace("embed", "check")).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get().body().text()).getString("file")
                VideoServer(NATSUKI, Option(name, null, link))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
}
