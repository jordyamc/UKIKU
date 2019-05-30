package knf.kuma.commons

import android.content.Context
import android.webkit.CookieManager
import androidx.preference.PreferenceManager
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
        val userAgent get() = PrefsUtil.userAgent
        var isLoading = false

        private const val keyCfClearance = "cf_clearance"
        private const val keyCfDuid = "__cfduid"
        private const val defaultValue = ""

        fun saveCookies(context: Context): Boolean {
            val cookies = CookieManager.getInstance().getCookie("https://animeflv.net/").trim { it <= ' ' }
            if (cookies.contains(keyCfClearance)) {
                val parts = cookies.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val preferences = PreferenceManager.getDefaultSharedPreferences(context).edit()
                for (cookie in parts) {
                    if (cookie.contains(keyCfDuid))
                        preferences.putString(keyCfDuid, cookie.trim().substring(cookie.trim().indexOf("=") + 1))
                    if (cookie.contains(keyCfClearance))
                        preferences.putString(keyCfClearance, cookie.trim().substring(cookie.trim().indexOf("=") + 1))
                }
                preferences.apply()
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
        }

        fun isNeeded(): Boolean {
            return try {
                val response = jsoupCookies("https://animeflv.net/").execute()
                response.statusCode() == 503
            } catch (e: HttpStatusException) {
                e.statusCode == 503
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
            map[keyCfClearance] = getClearance(context)
            map[keyCfDuid] = getCFDuid(context)
            return map
        }

        fun getStringCookie(context: Context): String {
            return "device=computer; cf_clearance=${getClearance(context)}; __cfduid=${getCFDuid(context)}"
        }

        fun getClearance(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(keyCfClearance, defaultValue)
                    ?: defaultValue
        }

        fun getCFDuid(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(keyCfDuid, defaultValue)
                    ?: defaultValue
        }
    }
}
