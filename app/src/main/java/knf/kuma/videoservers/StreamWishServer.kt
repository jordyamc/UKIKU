package knf.kuma.videoservers

import android.content.Context
import android.util.Log
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.jsoupCookies
import knf.kuma.videoservers.VideoServer.Names.FENIX
import knf.kuma.videoservers.VideoServer.Names.STREAMWISH
import org.json.JSONObject

class StreamWishServer internal constructor(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("streamwish.to")

    override val name: String
        get() = STREAMWISH

    override val canStream: Boolean
        get() = true

    override val canDownload: Boolean
        get() = false

    override val videoServer: VideoServer?
        get() {
            return try {
                val downLink = PatternUtil.extractLink(baseLink)
                val unpack = Unpacker.unpack(downLink)
                val link = "file:\"(.*)\"\\}\\],image".toRegex().find(unpack)?.destructured?.component1()
                VideoServer(name, Option(name, null, link))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}
