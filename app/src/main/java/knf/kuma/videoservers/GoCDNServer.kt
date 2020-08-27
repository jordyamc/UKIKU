package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.videoservers.VideoServer.Names.GOCDN
import org.json.JSONObject
import java.net.URL

class GoCDNServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("gocdn")

    override val name: String
        get() = GOCDN

    override val videoServer: VideoServer?
        get() {
            return try {
                val downLink = PatternUtil.extractLink(baseLink)
                val json = JSONObject(URL("https://streamium.xyz/gocdn.php?v=${downLink.substringAfterLast("#")}").readText())
                VideoServer(GOCDN, Option(GOCDN, null, json.getString("file")))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
}
