package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.videoservers.VideoServer.Names.RV
import org.jsoup.Jsoup

class RVServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("rapidvideo") || baseLink.contains("&server=rv")

    override val name: String
        get() = RV

    override val videoServer: VideoServer?
        get() {
            try {
                val frame = baseLink.substring(baseLink.indexOf("'") + 1, baseLink.lastIndexOf("'"))
                var downLink = Jsoup.parse(frame).select("iframe").first().attr("src").replace("&q=720p|&q=480p|&q=360p".toRegex(), "")
                if (downLink.contains("&server=rv"))
                    downLink = PatternUtil.getRapidLink(downLink)
                val needPost = Jsoup.connect(downLink).get().html().contains("Please click on this button to open this video")
                val videoServer = VideoServer(RV)
                try {
                    val jsoup720 = PatternUtil.getRapidVideoLink(getHtml("$downLink&q=720p", needPost))
                    videoServer.addOption(Option(name, "720p", jsoup720))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    val jsoup480 = PatternUtil.getRapidVideoLink(getHtml("$downLink&q=480p", needPost))
                    videoServer.addOption(Option(name, "480p", jsoup480))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    val jsoup360 = PatternUtil.getRapidVideoLink(getHtml("$downLink&q=360p", needPost))
                    videoServer.addOption(Option(name, "360p", jsoup360))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return if (videoServer.options!!.size > 0) videoServer else null
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

        }

    private fun getHtml(link: String, needPost: Boolean): String {
        return try {
            if (needPost)
                Jsoup.connect("$link#").data("block", "1").post().html()
            else
                Jsoup.connect(link).get().html()
        } catch (e: Exception) {
            ""
        }

    }
}
