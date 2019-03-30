package knf.kuma.custom

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import knf.kuma.App
import knf.kuma.R
import knf.kuma.commons.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.findOptional

open class GenericActivity : AppCompatActivity() {
    override fun onResume() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            val appIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            setTaskDescription(ActivityManager.TaskDescription(getString(R.string.app_name), appIcon, ContextCompat.getColor(this, R.color.colorPrimary)))
        }
        checkBypass()
        super.onResume()
    }

    open fun getSnackbarAnchor(): View? = null

    open fun onBypassUpdated() {

    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun checkBypass() {
        val webView = findOptional<WebView>(R.id.webview)
        if (webView != null)
            doAsync {
                if (BypassUtil.isNeeded() && !BypassUtil.isLoading) {
                    val snack = getSnackbarAnchor()?.showSnackbar("Creando bypass...", Snackbar.LENGTH_INDEFINITE)
                            ?: null.also { "Creando bypass...".toast() }
                    BypassUtil.isLoading = true
                    Log.e("CloudflareBypass", "is needed")
                    BypassUtil.clearCookies()
                    doOnUI {
                        webView.settings?.javaScriptEnabled = true
                        webView.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                shouldOverrideUrlLoading(view, request?.url?.toString())
                                return false
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                Log.e("CloudflareBypass", "Override $url")
                                snack?.safeDismiss()
                                if (url == "https://animeflv.net/") {
                                    Log.e("CloudflareBypass", "Cookies: " + CookieManager.getInstance().getCookie("https://animeflv.net/"))
                                    if (BypassUtil.saveCookies(App.context)) {
                                        getSnackbarAnchor()?.showSnackbar("Bypass actualizado")
                                                ?: "Bypass actualizado".toast()
                                        PicassoSingle.clear()
                                    }
                                    onBypassUpdated()
                                    BypassUtil.isLoading = false
                                }
                                return false
                            }
                        }
                        webView.settings?.userAgentString = BypassUtil.userAgent
                        webView.loadUrl("https://animeflv.net/")
                    }
                } else {
                    Log.e("CloudflareBypass", "Not needed")
                }
            }
    }
}