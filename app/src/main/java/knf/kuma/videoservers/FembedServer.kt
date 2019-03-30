package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.execute
import knf.kuma.commons.iterator
import knf.kuma.commons.okHttpCookies
import knf.kuma.videoservers.VideoServer.Names.FEMBED
import org.json.JSONObject

class FembedServer internal constructor(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("fembed.com")

    override val name: String
        get() = FEMBED

    override val videoServer: VideoServer?
        get() {
            return try {
                val downLink = PatternUtil.extractLink(baseLink)
                val json = JSONObject(okHttpCookies(downLink.replace("/v/", "/api/source/"), "POST").execute().body()?.string())
                if (!json.getBoolean("success"))
                    throw IllegalStateException("Request was not succeeded")
                val array = json.getJSONArray("data")
                val options = mutableListOf<Option>()
                for (item in array) {
                    options.add(Option(name, item.getString("label"), item.getString("file")))
                }
                VideoServer(name, options)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
}
