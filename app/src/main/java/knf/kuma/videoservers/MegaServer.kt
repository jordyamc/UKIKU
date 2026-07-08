package knf.kuma.videoservers

import android.content.Context
import knf.kuma.videoservers.VideoServer.Names.MEGA

class MegaServer(context: Context, baseLink: String) : Server(context, baseLink) {
    private val DOWNLOAD = "D"
    private val STREAM = "S"

    override val isValid: Boolean
        get() = baseLink.contains("mega.nz")

    override val name: String
        get() = "$MEGA $type (WEB)"

    private val type: String
        get() = if (baseLink.contains("mega.nz") && !baseLink.contains("embed"))
            DOWNLOAD
        else
            STREAM

    override val canStream: Boolean
        get() = type == STREAM

    override val canDownload: Boolean
        get() = type == DOWNLOAD

    override val videoServer: VideoServer
        get() {
            return VideoServer(name, Option(name, null, baseLink, needTabs = true), skipVerification = true)
        }
}
