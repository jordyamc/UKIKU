package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.videoservers.VideoServer.Names.MEGA
import java.net.URLDecoder

class MegaServer(context: Context, baseLink: String) : Server(context, baseLink) {
    private val DOWNLOAD = "1"
    private val STREAM = "2"

    override val isValid: Boolean
        get() = baseLink.contains("mega.nz") && !baseLink.contains("embed") || baseLink.contains("server=mega")

    override val name: String
        get() = "$MEGA $type"

    private val type: String
        get() = if (baseLink.contains("mega.nz") && !baseLink.contains("embed"))
            DOWNLOAD
        else
            STREAM

    override val videoServer: VideoServer?
        get() {
            return try {
                if (type == STREAM) {
                    val downLink = PatternUtil.extractLink(baseLink)
                    val link = "https://mega.nz/#" + downLink.substring(downLink.lastIndexOf("!"))
                    VideoServer(name, Option(name, null, link))
                } else
                    VideoServer(name, Option(name, null, URLDecoder.decode(baseLink, "utf-8")))
            } catch (e: Exception) {
                null
            }

        }
}
