package knf.kuma.commons

import android.annotation.SuppressLint
import android.content.Context

import com.squareup.picasso.Picasso

object PicassoSingle {
    @SuppressLint("StaticFieldLeak")
    private var picasso: Picasso? = null

    operator fun get(context: Context): Picasso {
        if (picasso == null)
            PicassoSingle.picasso = Picasso.Builder(context).downloader(CookieImageDownloader(context)).build()
        return picasso!!
    }

    fun clear() {
        picasso = null
    }
}
