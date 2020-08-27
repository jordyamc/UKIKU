package knf.kuma.commons

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.preference.PreferenceManager
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.uagen.UAGenerator
import knf.kuma.uagen.randomUA
import kotlinx.coroutines.*
import okhttp3.Request
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.util.*

/**
 * Created by jordy on 17/03/2018.
 */

class BypassUtil {

    interface BypassListener {
        fun onNeedRecreate()
    }

    companion object {
        val userAgent: String
            get() = if (PrefsUtil.useDefaultUserAgent && !PrefsUtil.alwaysGenerateUA) noCrashLet { WebSettings.getDefaultUserAgent(App.context) }
                    ?: PrefsUtil.userAgent else PrefsUtil.userAgent
        var isLoading = false
        var isChecking = false
        var isVerifing = false

        private const val keyCfClearance = "cf_clearance"
        private const val keyCfDuid = "__cfduid"
        private const val defaultValue = ""
        const val testLink = "https://animeflv.net/"

        suspend fun clearCookiesIfNeeded() {
            if (!withContext(Dispatchers.IO){ isCloudflareActive() })
                clearCookies(null)
        }

        fun saveCookies(context: Context): Boolean =
                noCrashLet(false) {
                    val cookies = CookieManager.getInstance().getCookie(".animeflv.net")?.trim()
                            ?: ""
                    if (cookies.contains(keyCfClearance)) {
                        val parts = cookies.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        for (cookie in parts) {
                            if (cookie.contains(keyCfDuid))
                                setCFDuid(context, cookie.trim().substring(cookie.trim().indexOf("=") + 1))
                            if (cookie.contains(keyCfClearance)) {
                                val clearance = cookie.trim().substring(cookie.trim().indexOf("=") + 1)
                                if (clearance.isBlank())
                                    return@noCrashLet false
                                setClearance(context, clearance)
                            }
                        }
                        return@noCrashLet true
                    }
                    false
                }

        fun clearCookies() {
            noCrash {
                val cookieManager = CookieManager.getInstance()
                val cookiestring = cookieManager.getCookie(".animeflv.net")
                if (cookiestring != null) {
                    noCrash {
                        val cookies = cookiestring.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        for (cookie in cookies) {
                            val cookieparts = cookie.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            cookieManager.setCookie(".animeflv.net", cookieparts[0].trim() + "=; Expires=Wed, 31 Dec 2025 23:59:59 GMT")
                        }
                    }
                }
                setCFDuid(App.context)
                setClearance(App.context)
                PrefsUtil.userAgent = UAGenerator.getRandomUserAgent()
            }
        }

        fun clearCookies(webView: WebView?) {
            noCrash {
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                webView?.clearCache(true)
                setCFDuid(App.context)
                setClearance(App.context)
                PrefsUtil.userAgent = UAGenerator.getRandomUserAgent()
            }
        }

        fun isNeeded(url: String = testLink): Boolean {
            return try {
                val response = jsoupCookies(url).execute()
                response.statusCode().let { it == 503 || it == 403 }
            } catch (e: HttpStatusException) {
                e.statusCode.let { it == 503 || it == 403 }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun isCloudflareActive(url: String = testLink): Boolean {
            return try {
                val response = Jsoup.connect(url).followRedirects(true).execute()
                response.statusCode().let { it == 503 || it == 403 }
            } catch (e: HttpStatusException) {
                e.statusCode.let { it == 503 || it == 403 }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun isCloudflareActiveRandom(url: String = testLink): Boolean {
            return try {
                val response = Jsoup.connect(url).followRedirects(true).userAgent(randomUA()).execute()
                response.statusCode().let { it == 503 || it == 403 }
            } catch (e: HttpStatusException) {
                e.statusCode.let { it == 503 || it == 403 }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun isNeededFlag(): Int {
            return try {
                val response = jsoupCookies(testLink).execute()
                when (response.statusCode()) {
                    503 -> 1
                    403 -> 2
                    else -> 0
                }
            } catch (e: HttpStatusException) {
                when (e.statusCode) {
                    503 -> 1
                    403 -> 2
                    else -> 0
                }
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }

        fun isConnectionBlocked(): Boolean {
            return try {
                val response = jsoupCookies("https://animeflv.net/").execute()
                response.statusCode() == 403
            } catch (e: HttpStatusException) {
                e.statusCode == 403
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun getMapCookie(context: Context): Map<String, String> {
            val map = LinkedHashMap<String, String>()
            map["device"] = "computer"
            map["InstiSession"] = "eyJpZCI6IjRlNGYwNWYxLTg4NDMtNGQwOS05ODlmLWM1OWQ5N2NmNjVlYyIsInJlZmVycmVyIjoiIiwiY2FtcGFpZ24iOnsic291cmNlIjpudWxsLCJtZWRpdW0iOm51bGwsImNhbXBhaWduIjpudWxsLCJ0ZXJtIjpudWxsLCJjb250ZW50IjpudWxsfX0="
            getClearance(context).let { if (it != defaultValue) map[keyCfClearance] = it }
            getCFDuid(context).let { if (it != defaultValue) map[keyCfDuid] = it }
            return map
        }

        fun getStringCookie(context: Context): String {
            val builder = StringBuilder()
            for ((key, value) in getMapCookie(context))
                builder.append("$key=$value;")
            return builder.toString().dropLastWhile { it == ' ' || it == ';' }
        }

        fun getClearance(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(keyCfClearance, defaultValue)
                    ?: defaultValue
        }

        fun setClearance(context: Context, value: String = defaultValue) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(keyCfClearance, value).apply()
        }

        private fun getClearanceString(context: Context): String {
            val value = PreferenceManager.getDefaultSharedPreferences(context).getString(keyCfClearance, defaultValue)
                    ?: defaultValue
            return if (value == defaultValue) "" else " cf_clearance=$value;"
        }

        fun getCFDuid(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(keyCfDuid, defaultValue)
                    ?: defaultValue
        }

        fun setCFDuid(context: Context, value: String = defaultValue) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(keyCfDuid, value).apply()
        }

        fun doConnectionTests() {
            if (BuildConfig.DEBUG)
                GlobalScope.launch(Dispatchers.IO) {
                    delay(3000)
                    Log.e("Test", "Using cookie: ${getStringCookie(App.context)}")
                    noCrash {
                        Log.e("Test", "Jsoup normal")
                        Jsoup.connect("https://www3.animeflv.net/ver/kanojo-okarishimasu-6").execute().also {
                            Log.e("Test Result ${it.statusCode()}", it.body())
                        }
                    }
                    noCrash {
                        Log.e("Test", "Jsoup cookies")
                        jsoupCookies("https://www3.animeflv.net/ver/kanojo-okarishimasu-6").execute().also {
                            Log.e("Result ${it.statusCode()}", it.body())
                        }
                    }
                    noCrash {
                        Log.e("Test", "okttp normal")
                        Request.Builder().apply {
                            url("https://www3.animeflv.net/ver/kanojo-okarishimasu-6")
                            method("GET", null)
                            header("User-Agent", userAgent)
                        }.build().execute().also {
                            it.use {
                                Log.e("Test Result", "${it.body?.string()}")
                            }
                        }
                    }
                    noCrash {
                        Log.e("Test Test", "okhttp cookies")
                        okHttpCookies("https://www3.animeflv.net/ver/kanojo-okarishimasu-6").execute().also {
                            it.use {
                                Log.e("Test Result", "${it.body?.string()}")
                            }
                        }
                    }
                }
        }

        private fun getCFDuidString(context: Context): String {
            val value = PreferenceManager.getDefaultSharedPreferences(context).getString(keyCfDuid, defaultValue)
                    ?: defaultValue
            return if (value == defaultValue) "" else " __cfduid=$value;"
        }
    }
}
