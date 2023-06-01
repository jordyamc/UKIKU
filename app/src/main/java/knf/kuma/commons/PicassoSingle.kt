package knf.kuma.commons

import android.annotation.SuppressLint
import com.squareup.picasso.OkHttp3Downloader

import com.squareup.picasso.Picasso
import knf.kuma.App
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

object PicassoSingle {
    @SuppressLint("StaticFieldLeak")
    private lateinit var picasso: Picasso

    fun get(): Picasso {
        if (!::picasso.isInitialized)
            picasso = create()
        return picasso
    }

    private fun create(): Picasso = Picasso.Builder(App.context)
            .downloader(OkHttp3Downloader(OkHttpClient().newBuilder().addInterceptor {
                val nRequest = it.request().newBuilder().apply {
                    addHeader("Cookie", BypassUtil.getStringCookie(App.context))
                    addHeader("User-Agent", BypassUtil.userAgent)
                }.build()
                it.proceed(nRequest)
            }.build())).build()

    fun clear() {
        picasso = create()
    }
}
