package knf.kuma.videoservers

import android.content.Context
import android.util.Log
import knf.kuma.commons.PatternUtil
import knf.kuma.videoservers.VideoServer.Names.GOCDN

class GoCDNServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("gocdn")

    override val name: String
        get() = GOCDN

    override val videoServer: VideoServer?
        get() {
            val downLink = PatternUtil.extractLink(baseLink)
            return try {
                val link = "https://s1.streamium.xyz/gocdn.php?v=${downLink.substringAfterLast("#")}"
                Log.e("GoCDN", link)
                VideoServer(GOCDN, Option(GOCDN, null, link))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
}
