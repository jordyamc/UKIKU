package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.jsoupCookies
import knf.kuma.videoservers.VideoServer.Names.FENIX
import org.json.JSONObject

class FenixServer internal constructor(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("s=fenix")

    override val name: String
        get() = FENIX

    override val videoServer: VideoServer?
        get() {
            return try {
                val downLink = PatternUtil.extractLink(baseLink)
                val link = JSONObject(jsoupCookies(downLink.replace("embed", "check")).get().body().text()).getString("file")
                VideoServer(FENIX, Option(name, null, link))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}
