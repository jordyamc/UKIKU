package knf.kuma.commons

import android.annotation.SuppressLint

import com.squareup.picasso.Picasso
import knf.kuma.App

object PicassoSingle {
    @SuppressLint("StaticFieldLeak")
    private lateinit var picasso: Picasso

    fun get(): Picasso {
        if (!::picasso.isInitialized)
            picasso = create()
        return picasso
    }

    private fun create(): Picasso = Picasso.Builder(App.context)
            .downloader(CookieImageDownloader(App.context)).build()

    fun clear() {
        picasso = create()
    }
}
