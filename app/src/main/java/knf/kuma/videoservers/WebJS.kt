package com.venom.greendark.decoder

import android.content.Context
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class WebJS(context: Context) {
    private val webView = WebView(context)
    private var currentUrl: String? = null
    private var callback: ((String?, String) -> Unit)? = null

    init {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        webView.addJavascriptInterface(JSInterface { callback?.invoke(currentUrl, it) }, "myInterface")
    }

    fun evalOnFinish(link: String, js: String, delay: Long = 5000, callback: (String?, String) -> Unit) {
        this.callback = callback
        var response = false
        val handler = Handler(Looper.getMainLooper())
        val run = Runnable {
            if (!response) {
                response = true
                currentUrl = webView.url
                webView.loadUrl("javascript:myInterface.returnResult(eval('try{$js}catch(e){e}'));")
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                handler.removeCallbacks(run)
                run.run()
            }
        }
        handler.postDelayed(run, delay)
        webView.loadUrl(link)
    }

    fun listenResources(link: String, pattern: Pattern, timeout: Long, executeOnFinish: String? = null, callback: (String?, Map<String, String>?) -> Unit) {
        var response = false
        val handler = Handler(Looper.getMainLooper())
        val regex = pattern.toRegex()
        val run = Runnable {
            if (!response) {
                response = true
                callback(null, null)
            }
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.proceed()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return true
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (!response && request?.url?.toString()?.matches(regex) == true) {
                    handler.removeCallbacks(run)
                    response = true
                    callback(request.url.toString(), request.requestHeaders)
                    webView.post {
                        webView.loadUrl("about:blank")
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                if (executeOnFinish != null) {
                    webView.loadUrl(executeOnFinish)
                }
            }
        }
        handler.postDelayed(run, timeout)
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