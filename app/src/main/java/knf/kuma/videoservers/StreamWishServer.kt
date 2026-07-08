package knf.kuma.videoservers

import android.content.Context
import androidx.core.net.toUri
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.jsoupCookies
import knf.kuma.videoservers.VideoServer.Names.STREAMWISH
import kotlinx.coroutines.runBlocking

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
                val downLink = "https://sfastwish.com/e/${PatternUtil.extractLink(baseLink).substringAfterLast("/")}"
                for (i in 0..3) {
                    val unpack = if (i >= 2) {
                        runBlocking { Unpacker.unpackWeb(context, downLink) }
                    } else {
                        runBlocking { Unpacker.unpack(downLink) }
                    }
                    val host = unpack.url!!.toUri().let { it.scheme + "://" + it.host }
                    val options = "hls\\d\": ?\"([^\"]*)".toRegex().findAll(unpack.unpacked).toList().reversed().mapIndexed { index, it ->
                        val (link) = it.destructured
                        Option(name, "HLS${index + 1}", if (link.startsWith("http")) link else host + link)
                    }.toMutableList()
                    val selected = options.firstOrNull { option ->
                        jsoupCookies(option.url)
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .execute().let { it.statusCode() in (200..299) && it.body().startsWith("#") }
                    }
                    if (selected == null) continue
                    return VideoServer(name, selected)
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}
