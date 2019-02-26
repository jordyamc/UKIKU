package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.jsoupCookies
import knf.kuma.videoservers.VideoServer.Names.FIRE
import org.jsoup.Jsoup

class FireServer internal constructor(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("efire.php")

    override val name: String
        get() = FIRE

    override val videoServer: VideoServer?
        get() {
            return try {
                val frame = PatternUtil.extractLink(baseLink)
                val mediaFunc = jsoupCookies(frame).get().select("script").last().outerHtml()
                val download = Jsoup.connect(PatternUtil.extractMediaLink(mediaFunc)).get().select("a[href~=http://download.*]").first().attr("href")
                VideoServer(FIRE, Option(name, null, download))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
}