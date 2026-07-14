package knf.kuma.videoservers

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import com.venom.greendark.decoder.WebJS
import de.prosiebensat1digital.oasisjsbridge.JsBridge
import de.prosiebensat1digital.oasisjsbridge.JsBridgeConfig
import knf.kuma.commons.safeContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Unpacker {
    private val packedRegex1 =
        "(function.*\\}\\s*\\('.*',\\s*.*?,\\s*\\d+,\\s*'.*?'\\.split\\('\\|'\\),\\d+,\\{.*\\}\\))".toRegex()
    private val packedRegex2 =
        "eval\\((function\\(p,a,c,k,e,?[dr]?\\).*.split\\('\\|'\\).*)\\)".toRegex()

    fun unpack(link: String): UnpackResult {
        val result = Jsoup.connect(link).ignoreContentType(true).execute()
        val html = result.body()
        val packedCode = packedRegex2.find(html)?.destructured?.component1() ?: return UnpackResult(result.url().toString(), html)
        val jsBridge = JsBridge(JsBridgeConfig.bareConfig(), safeContext)
        return UnpackResult(result.url().toString(), jsBridge.evaluateBlocking("function prnt() {var txt = $packedCode; return txt;}prnt();"))
    }

    suspend fun unpackWeb(context: Context, link: String): UnpackResult {
        val html = getHtml(context, link)
        val packedCode = packedRegex2.find(html.html)?.destructured?.component1() ?: return UnpackResult(html.url, html.html)
        val jsBridge = JsBridge(JsBridgeConfig.bareConfig(), safeContext)
        return UnpackResult(html.url, jsBridge.evaluateBlocking("function prnt() {var txt = $packedCode; return txt;}prnt();"))
    }

    fun unpackHtml(html: String): String {
        val packedCode = packedRegex2.find(html)?.destructured?.component1() ?: return html
        val jsBridge = JsBridge(JsBridgeConfig.bareConfig(), safeContext)
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

    suspend fun getHtml(context: Context, link: String, delay: Long = 5000): HTMLResult {
        return withContext(Dispatchers.Main) {
            val evaluator = WebJS(context)
            suspendCoroutine { continuation ->
                evaluator.evalOnFinish(
                    link,
                    "(\"<html>\"+document.getElementsByTagName(\"html\")[0].innerHTML+\"<\\/html>\")",
                    delay
                ) { url, html ->
                    continuation.resume(HTMLResult(url, html))
                }
            }
        }
    }

    suspend fun listenResources(context: Context, link: String, pattern: Pattern, userAgent: String = WebSettings.getDefaultUserAgent(context), timeout: Long = 10000, executeOnFinish: String? = null): String? {
        return withContext(Dispatchers.Main) {
            val evaluator = WebJS(context)
            suspendCoroutine { continuation ->
                evaluator.listenResources(
                    link,
                    pattern,
                    userAgent,
                    timeout,
                    executeOnFinish
                ) { url, headers ->
                    continuation.resume(url)
                }
            }
        }
    }

    data class HTMLResult(
        val url: String?,
        val html: String
    )

    data class UnpackResult(
        val url: String?,
        val unpacked: String
    )

}