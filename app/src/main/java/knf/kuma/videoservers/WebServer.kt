package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.jsoupCookies
import knf.kuma.videoservers.VideoServer.Names.FENIX
import org.json.JSONObject

class WebServer (context: Context, baseLink: String, val serverName: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = true

    override val name: String
        get() = "$serverName (WEB)"

    override val canStream: Boolean
        get() = false

    override val videoServer: VideoServer?
        get() {
            return try {
                VideoServer(serverName, Option(name, null, PatternUtil.extractLink(baseLink)))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}
