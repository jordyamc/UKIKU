package knf.kuma.videoservers

import android.content.Context
import knf.kuma.videoservers.VideoServer.Names.ZILLA
import org.jsoup.Jsoup

class ZillaServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("zilla-networks")

    override val name: String
        get() = ZILLA

    override val canDownload: Boolean
        get() = false

    override val videoServer: VideoServer?
        get() {
            return try {
                val url = baseLink.replace("/play/", "/m3u8/")
                val response = Jsoup.connect(url).ignoreContentType(true).ignoreHttpErrors(true).execute()
                if (response.statusCode() != 200 || !response.body().startsWith("#EXTM3U")) {
                    throw IllegalStateException()
                }
                VideoServer(ZILLA,
                        mutableListOf(
                                Option(name, null, url),
                        ))
            } catch (e: Exception) {
                null
            }

        }
}
