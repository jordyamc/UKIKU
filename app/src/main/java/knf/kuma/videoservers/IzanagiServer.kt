package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.jsoupCookies
import knf.kuma.videoservers.VideoServer.Names.IZANAGI
import org.json.JSONObject

class IzanagiServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("s=izanagi")

    override val name: String
        get() = IZANAGI

    override val videoServer: VideoServer?
        get() {
            val downLink = PatternUtil.extractLink(baseLink)
            return try {
                val link = JSONObject(jsoupCookies(downLink.replace("embed", "check")).get().body().text()).getString("file").replace("\\", "")
                VideoServer(IZANAGI,
                        mutableListOf(
                                Option(name, null, link),
                                Option(name, null, link.replace("/".toRegex(), "//").replace(":////", "://"))
                        ))
            } catch (e: Exception) {
                null
            }

        }
}
