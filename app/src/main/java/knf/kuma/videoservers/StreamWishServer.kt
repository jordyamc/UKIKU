package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.videoservers.VideoServer.Names.STREAMWISH
import java.net.URL

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
                val unpack = URL(downLink).readText() //Unpacker.unpack(downLink)
                val options = "file:\"([^\"]+sw-cdnstream[^\"]+)\"(?:,label:\"(\\d+p))?".toRegex().findAll(unpack).map {
                    val (link, label: String?) = it.destructured
                    Option(name, label, link)
                }.toMutableList()
                VideoServer(name, options)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}
