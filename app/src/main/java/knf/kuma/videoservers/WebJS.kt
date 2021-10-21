package com.venom.greendark.decoder

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep

class WebJS(private val context: Context) {
    private val webView = WebView(context)
    private var callback: ((String) -> Unit)? = null

    init {
        webView.settings.apply {
            javaScriptEnabled = true
        }
        webView.addJavascriptInterface(JSInterface { callback?.invoke(it) }, "myInterface")
    }

    fun evalOnFinish(link: String, js: String, callback: (String) -> Unit) {
        this.callback = callback
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Handler(Looper.getMainLooper()).postDelayed({
                    webView.loadUrl("javascript:myInterface.returnResult(eval('try{$js}catch(e){e}'));")
                }, 1000)
            }
        }
        webView.loadUrl(link)
    }

    @Keep
    class JSInterface(private val callback: (String) -> Unit) {
        @JavascriptInterface
        fun returnResult(result: String) {
            callback(result)
        }
    }
}