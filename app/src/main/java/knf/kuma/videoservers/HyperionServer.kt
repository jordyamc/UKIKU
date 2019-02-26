package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.jsoupCookies
import knf.kuma.videoservers.VideoServer.Names.HYPERION
import org.json.JSONObject

class HyperionServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("server=hyperion")

    override val name: String
        get() = HYPERION

    override val videoServer: VideoServer?
        get() {
            val downLink = PatternUtil.extractLink(baseLink)
            try {
                val options = JSONObject(jsoupCookies(downLink.replace("embed_hyperion", "check")).get().body().text())
                val array = options.getJSONArray("streams")
                val videoServer = VideoServer(HYPERION)
                for (i in 0 until array.length()) {
                    try {
                        when (array.getJSONObject(i).getInt("label")) {
                            360 -> videoServer.addOption(Option(name, "360p", array.getJSONObject(i).getString("file")))
                            480 -> videoServer.addOption(Option(name, "480p", array.getJSONObject(i).getString("file")))
                            720 -> videoServer.addOption(Option(name, "720p", array.getJSONObject(i).getString("file")))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
                try {
                    videoServer.addOption(Option(name, "Direct", options.getString("direct")))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return videoServer
            } catch (e: Exception) {
                return null
            }

        }
}
