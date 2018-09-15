package knf.kuma.commons

import android.content.Context
import android.net.Uri

import com.squareup.picasso.UrlConnectionDownloader

import java.io.IOException
import java.net.HttpURLConnection

class CookieImageDownloader(private val context: Context) : UrlConnectionDownloader(context) {

    @Throws(IOException::class)
    override fun openConnection(path: Uri): HttpURLConnection {
        val conn = super.openConnection(path)
        conn.setRequestProperty("Cookie", BypassUtil.getStringCookie(context))
        conn.setRequestProperty("User-Agent", BypassUtil.userAgent)
        return conn
    }
}
