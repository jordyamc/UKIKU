package knf.kuma.videoservers

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
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
                val linkData: String? = runBlocking(Dispatchers.Main) {
                    suspendCoroutine {
                        WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                            }
                            addJavascriptInterface(object : ZippyJSInterface() {
                                @JavascriptInterface
                                override fun printHtml(string: String) {
                                    val result = Jsoup.parse(string).select("a#dlbutton").attr("href")
                                    it.resume(if (result.isBlank()) null else decoded.substringBefore("/v/") + result)
                                }
                            }, "HtmlViewer")
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loadUrl("javascript:window.HtmlViewer.printHtml" +
                                            "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
                                }
                            }
                            loadUrl(decoded)
                        }
                    }
                }
                linkData ?: return null
                VideoServer(name, Option(name, null, linkData))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }

    abstract class ZippyJSInterface {
        @JavascriptInterface
        open fun printHtml(string: String) {
        }
    }
}
