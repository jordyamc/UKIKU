package knf.kuma.videoservers

import android.content.Context
import com.evgenii.jsevaluator.JsEvaluator
import com.evgenii.jsevaluator.interfaces.JsCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import java.net.URLDecoder
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ZippyServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("zippyshare")

    override val name: String
        get() = VideoServer.Names.ZIPPYSHARE

    override val videoServer: VideoServer?
        get() {
            return try {
                val decoded = URLDecoder.decode(baseLink, "utf-8")
                val zi = Jsoup.connect(decoded).timeout(TIMEOUT).get()
                val file = zi.select("meta[property=og:title]").attr("content")
                val scripts = zi.select("script[type]:not([src],[data-cfasync])")
                var evalData: String? = null
                scripts.forEach { element ->
                    element.dataNodes().forEach {
                        if (it.wholeData.contains("document.getElementById('dlbutton')"))
                            evalData = "\\(([\\d %+/\\-*]+)\\)".toRegex().find(it.wholeData)?.destructured?.component1()
                    }
                }
                evalData ?: return null
                val linkData: String? = runBlocking(Dispatchers.Main) {
                    suspendCoroutine<String?> {
                        JsEvaluator(context).evaluate(evalData, object : JsCallback {
                            override fun onResult(p0: String?) {
                                it.resume(p0)
                            }

                            override fun onError(p0: String?) {
                                it.resume(null)
                            }
                        })
                    }
                }
                linkData ?: return null
                val link = decoded.replace("/v/", "/d/").replace("file.html", "$linkData/$file")
                VideoServer(name, Option(name, null, link))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }
}
