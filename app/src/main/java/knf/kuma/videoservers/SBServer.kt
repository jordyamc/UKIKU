package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.retry
import knf.kuma.uagen.randomUA
import knf.kuma.videoservers.VideoServer.Names.SBVIDEO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup

class SBServer internal constructor(context: Context, baseLink: String) :
    Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("sbvideo.net") || baseLink.contains("playersb.com") || baseLink.contains(
            "embedsb.com"
        )

    override val name: String
        get() = SBVIDEO

    override val videoServer: VideoServer?
        get() {
            return try {
                val downLink = PatternUtil.extractLink(baseLink).replace("/e/", "/")
                val userAgent = randomUA()
                val html = Jsoup.connect(downLink).userAgent(userAgent).get().html()
                val (videoId, type, hash) = Regex("download_video\\('(\\w+)','([nhl])','([\\w-]+)'\\)").find(
                    html
                )?.destructured ?: throw IllegalStateException()
                val downloadLink =
                    "${downLink.substringBeforeLast("/")}/dl?op=download_orig&id=$videoId&mode=$type&hash=$hash"
                val tryCode = {
                    val downCode =
                        runBlocking(Dispatchers.Main) { Unpacker.getHtml(context, downloadLink) }
                    val downDoc = Jsoup.parse(downCode)
                    val fLink = downDoc.select("a:contains(Download Link)").attr("href")
                    check(fLink.isNotBlank())
                    fLink
                }
                val directLink = retry(3, tryCode)
                VideoServer(SBVIDEO, Option(name, null, directLink))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
}
