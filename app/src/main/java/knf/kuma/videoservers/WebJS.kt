package com.venom.greendark.decoder

import android.content.Context
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import java.util.regex.Pattern

class WebJS(context: Context) {
    private val webView = WebView(context)
    private val cookieManager = CookieManager.getInstance()
    private var currentUrl: String? = null
    private var callback: ((String?, String) -> Unit)? = null

    init {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        webView.addJavascriptInterface(
            JSInterface { callback?.invoke(currentUrl, it) },
            "myInterface"
        )
    }

    fun evalOnFinish(
        link: String,
        js: String,
        delay: Long = 5000,
        callback: (String?, String) -> Unit
    ) {
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

    fun listenResources(
        link: String,
        pattern: Pattern,
        userAgent: String,
        timeout: Long,
        executeOnFinish: String? = null,
        callback: (String?, Map<String, String>?) -> Unit
    ) {
        var response = false
        val handler = Handler(Looper.getMainLooper())
        val regex = pattern.toRegex()
        val run = Runnable {
            webView.post {
                webView.loadUrl("about:blank")
            }
            if (!response) {
                response = true
                callback(null, null)
            }
        }
        WebView.setWebContentsDebuggingEnabled(true)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            userAgentString = userAgent
        }
        Log.e("WebJS", "listenResources: $link")
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
                if (!response && request?.url?.toString()?.matches(regex) == true) {
                    Log.e("WebJS", "Found: ${request?.url}")
                    handler.removeCallbacks(run)
                    response = true
                    callback(request.url.toString(), request.requestHeaders)
                    webView.post {
                        webView.loadUrl("about:blank")
                    }
                }
                return true
            }

            /*override fun shouldInterceptRequest(
                view: WebView?,
                url: String?
            ): WebResourceResponse? {
                if (!response && url?.matches(regex) == true) {
                    Log.e("WebJS", "Found: $url")
                    handler.removeCallbacks(run)
                    response = true
                    callback(url, cookieManager.getCookie(link).let {
                        if (it.isNullOrBlank() || !it.contains("=")) {
                            emptyMap()
                        } else {
                            it.split(";").associate {
                                it.trim().split("=").let {
                                    it[0] to it[1]
                                }
                            }
                        }
                    })
                    webView.post {
                        webView.loadUrl("about:blank")
                    }
                }
                return if (response) {
                    WebResourceResponse("text/plain", "UTF-8", null)
                } else {
                    Log.e("WebJS", "shouldInterceptRequest: $url")
                    super.shouldInterceptRequest(view, url)
                }
            }*/

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (!response && request?.url?.toString()?.matches(regex) == true) {
                    Log.e("WebJS", "Found: ${request?.url}")
                    handler.removeCallbacks(run)
                    response = true
                    callback(request.url.toString(), request.requestHeaders)
                    webView.post {
                        webView.loadUrl("about:blank")
                    }
                }
                return if (response) {
                    WebResourceResponse("text/plain", "UTF-8", null)
                } else {
                    Log.e("WebJS", "shouldInterceptRequest: ${request?.url}")
                    super.shouldInterceptRequest(view, request)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val javascript = """
            (function() {
                document.querySelectorAll('video, audio').forEach(function(media) {
                    media.muted = true;
                });
            })();
        """.trimIndent()
                view?.evaluateJavascript(javascript, null)
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