package knf.kuma.videoservers

import android.content.Context
import android.webkit.WebView
import com.venom.greendark.decoder.WebJS
import de.prosiebensat1digital.oasisjsbridge.JsBridge
import de.prosiebensat1digital.oasisjsbridge.JsBridgeConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Unpacker {
    private val packedRegex1 =
        "(function.*\\}\\s*\\('.*',\\s*.*?,\\s*\\d+,\\s*'.*?'\\.split\\('\\|'\\),\\d+,\\{.*\\}\\))".toRegex()
    private val packedRegex2 =
        "eval\\((function\\(p,a,c,k,e,?[dr]?\\).*.split\\('\\|'\\).*)\\)".toRegex()

    fun unpack(link: String): String {
        val html = Jsoup.connect(link).ignoreContentType(true).execute().body()
        val packedCode = packedRegex2.find(html)?.destructured?.component1()
        val jsBridge = JsBridge(JsBridgeConfig.bareConfig())
        return jsBridge.evaluateBlocking("function prnt() {var txt = $packedCode; return txt;}prnt();")
    }

    fun evalJs(context: Context, code: String): String {
        val evaluator = WebView(context)
        return runBlocking(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                evaluator.evaluateJavascript(code) {
                    continuation.resume(it)
                }
            }
        }
    }

    suspend fun getHtml(context: Context, link: String, delay: Long = 5000): String? {
        val evaluator = WebJS(context)
        return withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                evaluator.evalOnFinish(
                    link,
                    "(\"<html>\"+document.getElementsByTagName(\"html\")[0].innerHTML+\"<\\/html>\")",
                    delay
                ) {
                    continuation.resume(it)
                }
            }
        }
    }

}