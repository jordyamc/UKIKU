package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.videoservers.VideoServer.Names.STREAMWISH

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
                val option = "file: ?\"(http[^\"]+)".toRegex().findAll(unpack).first()
                val (link) = option.destructured
                VideoServer(name, Option(name, "HLS", link))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}
