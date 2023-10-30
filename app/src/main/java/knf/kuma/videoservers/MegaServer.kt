package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.urlDecode
import knf.kuma.videoservers.VideoServer.Names.MEGA

class MegaServer(context: Context, baseLink: String) : Server(context, baseLink) {
    private val DOWNLOAD = "D"
    private val STREAM = "S"

    override val isValid: Boolean
        get() = baseLink.contains("mega.nz")

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
                    VideoServer(name, Option(name, null, PatternUtil.extractLink(baseLink)))
                } else
                    VideoServer(name, Option(name, null, urlDecode(baseLink)))
            } catch (e: Exception) {
                null
            }

        }
}
