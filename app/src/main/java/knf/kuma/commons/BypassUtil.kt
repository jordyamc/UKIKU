package knf.kuma.commons

import android.content.Context
import android.preference.PreferenceManager
import android.webkit.CookieManager
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.io.IOException
import java.util.*

/**
 * Created by jordy on 17/03/2018.
 */

class BypassUtil {

    interface BypassListener {
        fun onNeedRecreate()
    }

    companion object {
        const val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0"
        var isLoading = false

        fun saveCookies(context: Context): Boolean {
            val cookies = CookieManager.getInstance().getCookie("https://animeflv.net/").trim { it <= ' ' }
            if (cookies.contains("cf_clearance")) {
                val parts = cookies.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val preferences = PreferenceManager.getDefaultSharedPreferences(context).edit()
                for (cookie in parts) {
                    if (cookie.contains("__cfduid"))
                        preferences.putString("__cfduid", cookie.trim { it <= ' ' }.substring(cookie.trim { it <= ' ' }.indexOf("=") + 1))
                    if (cookie.contains("cf_clearance"))
                        preferences.putString("cf_clearance", cookie.trim { it <= ' ' }.substring(cookie.trim { it <= ' ' }.indexOf("=") + 1))
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
                    cookieManager.setCookie(".animeflv.net", cookieparts[0].trim { it <= ' ' } + "=; Expires=Wed, 31 Dec 2025 23:59:59 GMT")
                }
            }
        }

        fun isNeeded(context: Context): Boolean {
            return try {
                val response = Jsoup.connect("https://animeflv.net/").cookies(getMapCookie(context)).userAgent(BypassUtil.userAgent).execute()
                response.statusCode() == 503
            } catch (e: HttpStatusException) {
                e.statusCode == 503
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }

        }

        fun getMapCookie(context: Context): Map<String, String> {
            val map = LinkedHashMap<String, String>()
            map["device"] = "computer"
            map["cf_clearance"] = getClearance(context)
            map["__cfduid"] = getCFDuid(context)
            return map
        }

        fun getStringCookie(context: Context): String {
            return "device=computer; " +
                    "cf_clearance=" + getClearance(context) + "; " +
                    "__cfduid=" + getCFDuid(context)
        }

        private fun getClearance(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context).getString("cf_clearance", "00000000")
                    ?: "00000000"
        }

        private fun getCFDuid(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context).getString("__cfduid", "00000000")
                    ?: "00000000"
        }
    }
}
