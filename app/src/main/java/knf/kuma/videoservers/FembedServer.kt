package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.iterator
import knf.kuma.videoservers.VideoServer.Names.FEMBED
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup

class FembedServer internal constructor(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("fembed") || baseLink.contains("embedsito.com")

    override val name: String
        get() = FEMBED

    override val videoServer: VideoServer?
        get() {
            return try {
                val downLink = PatternUtil.extractLink(baseLink)
                val fLink = if (downLink.contains("value="))
                    "https://embedsito.com/v/${downLink.substring(downLink.lastIndexOf("=") + 1)}"
                else
                    downLink
                val json = JSONObject(Jsoup.connect(fLink.replace("/v/", "/api/source/")).ignoreContentType(true).method(Connection.Method.POST).execute().body())
                check(json.getBoolean("success")) { "Request was not succeeded" }
                val array = json.getJSONArray("data")
                val options = mutableListOf<Option>()
                for (item in array) {
                    options.add(Option(name, item.getString("label"), item.getString("file")))
                }
                VideoServer(name, options, true)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
}
