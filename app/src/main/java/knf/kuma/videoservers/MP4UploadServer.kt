package knf.kuma.videoservers

import android.content.Context
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.jsoupCookies
import knf.kuma.videoservers.VideoServer.Names.MP4UPLOAD
import org.json.JSONObject
import java.util.regex.Pattern

class MP4UploadServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("s=mp4upload")

    override val name: String
        get() = MP4UPLOAD

    override val videoServer: VideoServer?
        get() {
            return try {
                val downLink = PatternUtil.extractLink(baseLink)
                val link = JSONObject(jsoupCookies(downLink.replace("embed", "check")).get().body().text()).getString("file")
                VideoServer(MP4UPLOAD, Option(name, null, link))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }


    private fun unpack(source: String): String? {
        var decoded: String? = null
        val pat = Pattern.compile("eval(.+),(\\d+),(\\d+),'(.+?)'")
        val m = pat.matcher(source)
        try {
            m.find()
            var p = m.group(1).replace("\\\\".toRegex(), "")
            val a = Integer.parseInt(m.group(2))
            var c = Integer.parseInt(m.group(3))
            val k = m.group(4).split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            while (c != 0) {
                c--
                if (k[c].isNotEmpty())
                    p = p.replace(("\\b" + baseT(c, a) + "\\b").toRegex(), k[c])
            }

            decoded = p
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return decoded
    }

    private fun baseT(num: Int, radix: Int): String {
        var edRadix = radix
        var mNum = num
        val digits = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()

        if (edRadix < 2 || edRadix > 62) {
            edRadix = 10
        }
        if (mNum < edRadix) {
            return "" + digits[mNum]
        }
        var hayMas = true
        var cadena = ""
        while (hayMas) {
            cadena = digits[mNum % edRadix] + cadena
            mNum /= edRadix
            if (mNum <= edRadix) {
                hayMas = false
                cadena = digits[mNum] + cadena
            }
        }
        return cadena
    }
}
