package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.jsoupCookies
import knf.kuma.videoservers.VideoServer.Names.OKRU
import java.net.URLEncoder

class OkruServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("ok.ru")

    override val name: String
        get() = OKRU

    override val videoServer: VideoServer?
        get() {
            try {
                val downLink = PatternUtil.extractLink(baseLink)
                val page = jsoupCookies("https://okvid.download/?url=${URLEncoder.encode(downLink, "UTF-8")}").get()
                val vs = VideoServer(OKRU, true)
                page.select("table.table tr:has(td:contains(0))").forEach { item ->
                    vs.addOption(Option(OKRU, item.select("td")[0].text().let { it.substring(it.lastIndexOf("x") + 1) }, item.select("a").attr("href")))
                }
                return vs
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

        }
}
