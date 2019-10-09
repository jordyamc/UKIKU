package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.jsoupCookies
import knf.kuma.videoservers.VideoServer.Names.VERYSTREAM

class VeryStreamServer internal constructor(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("verystream.com")

    override val name: String
        get() = VERYSTREAM

    override val videoServer: VideoServer?
        get() {
            return try {
                val downLink = PatternUtil.extractLink(baseLink)
                val link = "https://verystream.com/gettoken/${jsoupCookies(downLink).get().select("videolink").text()}?mime=true"
                VideoServer(name, Option(name, null, link))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
}
