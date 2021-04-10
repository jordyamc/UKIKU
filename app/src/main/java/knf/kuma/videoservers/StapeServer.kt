package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.videoservers.VideoServer.Names.STAPE
import org.jsoup.Jsoup

class StapeServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("streamtape.com")

    override val name: String
        get() = STAPE

    override val videoServer: VideoServer?
        get() {
            val downLink = PatternUtil.extractLink(baseLink)
            return try {
                val video =
                    Jsoup.connect(downLink).get().body().select("script:not([type],[src]) ").let {
                        it.forEach {
                            for (node in it.dataNodes()) {
                                val data = node.wholeData
                                if (data.contains("innerHTML") && data.contains("streamtape.com"))
                                    return@let data.replace(" ", "")
                                        .substringAfterLast("innerHTML=")
                                        .replace("[+\"';]".toRegex(), "")
                            }
                        }
                        return@let ""
                    }
                val link = "https:$video"
                val videoLink =
                    Jsoup.connect(link).ignoreContentType(true).followRedirects(true).execute()
                        .url().toString()
                check(!videoLink.contains("streamtape_do_not_delete.mp4"))
                VideoServer(STAPE, Option(name, null, videoLink))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
}
