package knf.kuma.commons

import de.prosiebensat1digital.oasisjsbridge.JsBridge
import de.prosiebensat1digital.oasisjsbridge.JsBridgeConfig
import knf.kuma.App
import knf.kuma.uagen.UAGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException
import kotlin.time.Duration.Companion.milliseconds

object JsExtractor {
    private var client: OkHttpClient = createClient()
    private val jsBridge = JsBridge(JsBridgeConfig.bareConfig(), App.context)

    private fun createClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionSpecs(
                listOf(
                    ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
                        .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0)
                        .build(),
                    ConnectionSpec.MODERN_TLS,
                    ConnectionSpec.CLEARTEXT
                )
            )
            .addInterceptor {
                it.proceed(
                    it.request().newBuilder().headers(BypassUtil.getOKHttpHeaders()).build()
                )
            }.build()
    }

    suspend fun processLink(link: String, key: String? = null, tries: Int = 0): JSONArray? {
        try {
            val response = fetchUrl(link)
            val rawArrayJs = extractDataArray(response)
            val jsonString = executeJS(rawArrayJs, key) ?: return null
            val parsed = JSONTokener(jsonString).nextValue()
            return when (val fixed = fixMojibakeRecursive(parsed)) {
                is JSONArray -> fixed
                is JSONObject -> JSONArray().apply { put(fixed) }
                else -> null
            }
        }catch (e: SSLHandshakeException) {
            e.printStackTrace()
            client = createClient()
            if (tries < 3) {
                return processLink(link, key, tries + 1)
            }
            return null
        } catch (e : Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun processLinkMultiple(link: String, keys: List<String>, tries: Int = 0): Map<String, JSONArray?> {
        try {
            val response = fetchUrl(link)
            val rawArrayJs = extractDataArray(response)
            return keys.associateWith {
                try {
                    val jsonString = executeJS(rawArrayJs, it) ?: return@associateWith null
                    val parsed = JSONTokener(jsonString).nextValue()
                    when (val fixed = fixMojibakeRecursive(parsed)) {
                        is JSONArray -> fixed
                        is JSONObject -> JSONArray().apply { put(fixed) }
                        else -> null
                    }
                } catch (e : Exception) {
                    e.printStackTrace()
                    null
                }
            }
        } catch (e: SSLHandshakeException) {
            client = createClient()
            if (tries < 3) {
                return processLinkMultiple(link, keys, tries + 1)
            }
           throw e
        }
    }

    private suspend fun fetchUrl(url: String, tries: Int = 0): String = withContext(Dispatchers.IO) {
        try {
            if (tries > 0) {
                Jsoup.connect(url).userAgent(UAGenerator.getLatestUserAgent())
            } else {
                jsoupCookies(url)
            }.ignoreContentType(true).execute().body()
        } catch (e: Exception) {
            if (e is HttpStatusException && e.statusCode == 404) throw e
            if (tries < 3) {
                delay(500.milliseconds * (tries + 1))
                fetchUrl(url, tries + 1)
            } else {
                throw e
            }
        }
    }

    private fun executeJS(rawArrayJs: String, key: String?): String? {
        try {
            val script = """
                function findKey(obj, keys) {
                    if (obj === null || typeof obj !== 'object') return undefined;
                    if (!Array.isArray(obj)) {
                        for (var i = 0; i < keys.length; i++) {
                            var k = keys[i];
                            if (obj.hasOwnProperty(k)) return obj[k];
                        }
                    }
                    for (var key in obj) {
                        var found = findKey(obj[key], keys);
                        if (found !== undefined) return found;
                    }
                    return undefined;
                }
                (function() {
                    var arr = $rawArrayJs;
                    var result;
                    for (var idx = 0; idx < arr.length; idx++) {
                        var found = findKey(arr[idx], [${if (key == null) "'media', 'result', 'results', 'latestEpisodes'" else "'$key'"}]);
                        if (found !== undefined) { result = found; break; }
                    }
                    return JSON.stringify(result);
                })();
            """.trimIndent()
            val result = jsBridge.evaluateBlocking<String?>(script)
            return if (result == null || result == "undefined") {
                null
            } else {
                result
            }
        } catch (e: Exception){
            e.printStackTrace()
            return null
        }
    }

    private fun extractBalanced(text: String, startIdx: Int, openChar: Char = '[', closeChar: Char = ']'): Int {
        var depth = 0
        var inString = false
        var stringChar = ' '
        var i = startIdx
        while (i < text.length) {
            val c = text[i]
            if (inString) {
                if (c == '\\') {
                    i += 2
                    continue
                }
                if (c == stringChar) {
                    inString = false
                }
            } else {
                if (c == '"' || c == '\'' || c == '`') {
                    inString = true
                    stringChar = c
                } else if (c == openChar) {
                    depth++
                } else if (c == closeChar) {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return -1
    }

    private fun extractDataArray(content: String): String {
        val regex = Regex("""data:\s*\[""")
        val match = regex.find(content) ?: throw IllegalArgumentException("data: [ not found")
        val startIdx = match.range.last
        val endIdx = extractBalanced(content, startIdx, '[', ']')
        return content.substring(startIdx, endIdx + 1)
    }

    private fun fixMojibake(input: String): String {
        if (input.none { it.code > 127 }) return input
        return try {
            val iso8859_1 = Charset.forName("ISO-8859-1")
            val encoder = iso8859_1.newEncoder()
            if (!encoder.canEncode(input)) return input

            val bytes = input.toByteArray(iso8859_1)
            val decoded = String(bytes, Charsets.UTF_8)
            if (decoded.contains('\uFFFD') && !input.contains('\uFFFD')) {
                input
            } else {
                decoded
            }
        } catch (e: Exception) {
            input
        }
    }

    private fun fixMojibakeRecursive(value: Any?): Any? = when (value) {
        is String -> fixMojibake(value)
        is JSONArray -> JSONArray().apply {
            for (i in 0 until value.length()) put(fixMojibakeRecursive(value.get(i)))
        }
        is JSONObject -> JSONObject().apply {
            val keys = value.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                put(k, fixMojibakeRecursive(value.get(k)))
            }
        }
        else -> value
    }
}