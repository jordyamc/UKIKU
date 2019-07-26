package knf.kuma.commons

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.preference.PreferenceManager
import knf.kuma.App
import knf.kuma.uagen.UAGenerator
import org.jsoup.HttpStatusException
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
            get() = if (PrefsUtil.useDefaultUserAgent) noCrashLet { WebSettings.getDefaultUserAgent(App.context) }
                    ?: PrefsUtil.userAgent else PrefsUtil.userAgent
        var isLoading = false
        var isChecking = false

        private const val keyCfClearance = "cf_clearance"
        private const val keyCfDuid = "__cfduid"
        private const val defaultValue = ""

        fun saveCookies(context: Context): Boolean {
            val cookies = CookieManager.getInstance().getCookie("https://animeflv.net/").trim()
            if (cookies.contains(keyCfClearance)) {
                val parts = cookies.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (cookie in parts) {
                    if (cookie.contains(keyCfDuid))
                        setCFDuid(context, cookie.trim().substring(cookie.trim().indexOf("=") + 1))
                    if (cookie.contains(keyCfClearance))
                        setClearance(context, cookie.trim().substring(cookie.trim().indexOf("=") + 1))
                }
                return true
            }
            return false
        }

        fun clearCookies() {
            val cookieManager = CookieManager.getInstance()
            val cookiestring = cookieManager.getCookie(".animeflv.net")
            if (cookiestring != null) {
                val cookies = cookiestring.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (cookie in cookies) {
                    val cookieparts = cookie.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    cookieManager.setCookie(".animeflv.net", cookieparts[0].trim() + "=; Expires=Wed, 31 Dec 2025 23:59:59 GMT")
                }
            }
            setCFDuid(App.context)
            setClearance(App.context)
            PrefsUtil.userAgent = UAGenerator.getRandomUserAgent()
        }

        fun isNeeded(): Boolean {
            return try {
                val response = jsoupCookies("https://animeflv.net/").execute()
                response.statusCode().let { it == 503 || it == 403 }
            } catch (e: HttpStatusException) {
                e.statusCode.let { it == 503 || it == 403 }
            } catch (e: Exception) {
                e.printStackTrace()
                false
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
            getClearance(context).let { if (it != defaultValue) map[keyCfClearance] = it }
            getCFDuid(context).let { if (it != defaultValue) map[keyCfDuid] = it }
            return map
        }

        fun getStringCookie(context: Context): String {
            return "device=computer;${getClearanceString(context)}${getCFDuidString(context)}".dropLastWhile { it == ' ' || it == ';' }
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

        private fun getCFDuidString(context: Context): String {
            val value = PreferenceManager.getDefaultSharedPreferences(context).getString(keyCfDuid, defaultValue)
                    ?: defaultValue
            return if (value == defaultValue) "" else " __cfduid=$value;"
        }
    }
}
