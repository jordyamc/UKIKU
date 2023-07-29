package knf.kuma.commons

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.preference.PreferenceManager
import knf.kuma.App
import knf.kuma.ads.AdsUtils
import knf.kuma.uagen.UAGenerator
import knf.kuma.uagen.randomUA
import knf.tools.bypass.DisplayType
import knf.tools.bypass.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup

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

        private const val keyCfClearance = "cf_clearance"
        private const val keyCfDuid = "__cfduid"
        private const val keyCookiesBypass = "bypass_cookies"
        private const val defaultValue = ""
        const val testLink = "https://www3.animeflv.net/"

        fun createRequest(): Request {
            return Request(
                testLink,
                lastUA = PrefsUtil.userAgent,
                showReload = AdsUtils.remoteConfigs.getBoolean("bypass_show_reload"),
                useFocus = isTV,
                maxTryCount = AdsUtils.remoteConfigs.getLong("bypass_max_tries").toInt(),
                useLatestUA = true,
                reloadOnCaptcha = false,
                waitCaptcha = true,
                clearCookiesAtStart = true,
                displayType = DisplayType.DIALOG,
                dialogStyle = 0
            )
        }

        suspend fun clearCookiesIfNeeded() {
            if (!withContext(Dispatchers.IO) { isCloudflareActive() })
                clearCookies(null)
        }

        fun saveCookies(context: Context, cookies: String): Boolean =
            noCrashLet(false) {
                bypassCookies = cookies
                if (cookies.contains(keyCfClearance)) {
                        val parts = cookies.split(";").dropLastWhile { it.isEmpty() }.toTypedArray()
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

        fun clearCookies(webView: WebView?) {
            noCrash {
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                webView?.clearCache(true)
                bypassCookies = null
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

        fun getMapCookie(context: Context): Map<String, String> {
            val map = LinkedHashMap<String, String>()
            map["device"] = "computer"
            map["InstiSession"] =
                "eyJpZCI6IjRlNGYwNWYxLTg4NDMtNGQwOS05ODlmLWM1OWQ5N2NmNjVlYyIsInJlZmVycmVyIjoiIiwiY2FtcGFpZ24iOnsic291cmNlIjpudWxsLCJtZWRpdW0iOm51bGwsImNhbXBhaWduIjpudWxsLCJ0ZXJtIjpudWxsLCJjb250ZW50IjpudWxsfX0="
            bypassCookies?.split(";")?.forEach {
                if (it.contains("=")) {
                    val split = it.split("=")
                    map[split[0]] = split[1]
                }
            }
            //getClearance(context).let { if (it != defaultValue) map[keyCfClearance] = it }
            //getCFDuid(context).let { if (it != defaultValue) map[keyCfDuid] = it }
            return map
        }

        fun getStringCookie(context: Context): String {
            val builder = StringBuilder()
            for ((key, value) in getMapCookie(context))
                builder.append("$key=$value;")
            return builder.toString().dropLastWhile { it == ' ' || it == ';' }
        }

        var bypassCookies: String?
            set(value) = PreferenceManager.getDefaultSharedPreferences(App.context).edit().putString(keyCookiesBypass, value).apply()
            get() = PreferenceManager.getDefaultSharedPreferences(App.context).getString(keyCookiesBypass, null)

        fun getClearance(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(keyCfClearance, defaultValue)
                ?: defaultValue
        }

        private fun setClearance(context: Context, value: String = defaultValue) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(keyCfClearance, value).apply()
        }

        fun getCFDuid(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(keyCfDuid, defaultValue)
                    ?: defaultValue
        }

        private fun setCFDuid(context: Context, value: String = defaultValue) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(keyCfDuid, value).apply()
        }
    }
}
