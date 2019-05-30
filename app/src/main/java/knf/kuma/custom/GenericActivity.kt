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
import androidx.lifecycle.MutableLiveData
import com.crashlytics.android.Crashlytics
import com.google.android.material.snackbar.Snackbar
import knf.kuma.App
import knf.kuma.R
import knf.kuma.commons.*
import knf.kuma.directory.DirectoryService
import knf.kuma.retrofit.Repository
import knf.kuma.uagen.randomUA
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.findOptional

open class GenericActivity : AppCompatActivity() {

    override fun onResume() {
        noCrash {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                val appIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                setTaskDescription(ActivityManager.TaskDescription(getString(R.string.app_name), appIcon, ContextCompat.getColor(this, R.color.colorPrimary)))
            }
            checkBypass()
            super.onResume()
        }
    }

    open fun getSnackbarAnchor(): View? = null

    open fun onBypassUpdated() {

    }

    open fun forceCreation(): Boolean = false

    private fun checkBypass() {
        doAsync(exceptionHandler = { it.also { Crashlytics.logException(it) }.message?.toastLong() }) {
            if ((BypassUtil.isNeeded() || forceCreation()) && !BypassUtil.isLoading) {
                val snack = getSnackbarAnchor()?.showSnackbar("Creando bypass...", Snackbar.LENGTH_INDEFINITE)
                bypassLive.postValue(Pair(true, true))
                BypassUtil.isLoading = true
                Log.e("CloudflareBypass", "is needed")
                runWebView(snack)
            } else {
                bypassLive.postValue(Pair(false, false))
                Log.e("CloudflareBypass", "Not needed")
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun runWebView(snack: Snackbar?) {
        BypassUtil.clearCookies()
        doOnUI {
            val webView = noCrashLet {
                findOptional<WrapWebView>(R.id.webview) ?: AppWebView(App.context)
            }
            if (webView != null) {
                webView.settings?.javaScriptEnabled = true
                webView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        shouldOverrideUrlLoading(view, request?.url?.toString())
                        return false
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        Log.e("CloudflareBypass", "Override $url")
                        if (url == "https://animeflv.net/") {
                            Log.e("CloudflareBypass", "Cookies: " + CookieManager.getInstance().getCookie("https://animeflv.net/"))
                            if (BypassUtil.saveCookies(App.context)) {
                                webView.loadUrl("about:blank")
                                doAsync {
                                    if (BypassUtil.isConnectionBlocked())
                                        runWebView(snack)
                                    else {
                                        doOnUI {
                                            snack?.safeDismiss()
                                            getSnackbarAnchor()?.showSnackbar("Bypass actualizado")
                                            bypassLive.postValue(Pair(true, false))
                                            Repository().reloadRecents()
                                            onBypassUpdated()
                                            BypassUtil.isLoading = false
                                            PicassoSingle.clear()
                                            if (!PrefsUtil.isDirectoryFinished)
                                                DirectoryService.run(this@GenericActivity)
                                        }
                                    }
                                }
                            }
                        }
                        return false
                    }
                }
                webView.settings?.userAgentString = randomUA().also { PrefsUtil.userAgent = it }
                webView.loadUrl("https://animeflv.net/")
            } else {
                snack?.safeDismiss()
                getSnackbarAnchor()?.showSnackbar("Error al iniciar WebView")
            }
        }
    }

    companion object {
        val bypassLive: MutableLiveData<Pair<Boolean, Boolean>> = MutableLiveData()
    }
}