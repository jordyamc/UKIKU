package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.PatternUtil
import knf.kuma.videoservers.VideoServer.Names.MANGO
import org.jsoup.Jsoup
import java.util.regex.Pattern

class MangoServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("server=streamango")

    override val name: String
        get() = MANGO

    override val videoServer: VideoServer?
        get() {
            try {
                val downLink = PatternUtil.extractLink(baseLink)
                val mangoLink = PatternUtil.extractMangoLink(Jsoup.connect(downLink).cookies(BypassUtil.getMapCookie(context)).userAgent(BypassUtil.userAgent).get().select("script").last().html())
                val html = Jsoup.connect(mangoLink).get().html()
                val matcher = Pattern.compile("type:\"video/mp4\",src:d\\('([^']+)',(\\d+)\\)").matcher(html)
                matcher.find()
                val hash = matcher.group(1)
                val key = Integer.parseInt(matcher.group(2))
                var file = KDecoder.decodeMango(hash, key)
                if (file!!.isEmpty())
                    return null
                if (file.startsWith("//"))
                    file = file.replaceFirst("//".toRegex(), "https://")
                return VideoServer(MANGO, Option(name, null, file))
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

        }
}