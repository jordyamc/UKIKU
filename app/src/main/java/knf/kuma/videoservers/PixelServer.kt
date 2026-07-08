package knf.kuma.videoservers

import android.content.Context
import knf.kuma.videoservers.VideoServer.Names.PDRAIN

class PixelServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("pixeldrain")

    override val name: String
        get() = PDRAIN

    override val videoServer: VideoServer?
        get() {
            return try {
                val url = baseLink.replace("/u/", "/api/file/").let {
                    if (it.endsWith("?embed")) {
                        it.replace("?embed", "?download=")
                    } else {
                        "$it?download="
                    }
                }
                VideoServer(PDRAIN,
                        mutableListOf(
                                Option(name, null, url),
                        ))
            } catch (e: Exception) {
                null
            }

        }
}
