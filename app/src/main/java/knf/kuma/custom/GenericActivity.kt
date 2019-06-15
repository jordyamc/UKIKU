package knf.kuma.custom

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.View
import android.webkit.*
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
import knf.kuma.uagen.UAGenerator
import knf.kuma.videoservers.ServersFactory
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.findOptional

open class GenericActivity : AppCompatActivity() {

    private var tryCount = 0

    override fun onResume() {
        noCrash {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                val appIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                setTaskDescription(ActivityManager.TaskDescription(getString(R.string.app_name), appIcon, ContextCompat.getColor(this, R.color.colorPrimary)))
            }
        }
        logText("On Resume check")
        checkBypass()
        super.onResume()
    }

    open fun getSnackbarAnchor(): View? = null

    open fun onBypassUpdated() {

    }

    open fun forceCreation(): Boolean = false

    open fun logText(text: String) {
        Log.e("Bypass", text)
    }

    fun checkBypass() {
        if (BypassUtil.isChecking) {
            logText("Already checking")
            return
        }
        BypassUtil.isChecking = true
        doAsync(exceptionHandler = {
            it.also {
                Crashlytics.logException(it)
                logText("Error: ${it.message}")
            }.message?.toastLong()
        }) {
            if ((BypassUtil.isNeeded() || forceCreation()).also { logText("Is needed or forced: $it") }
                    && !BypassUtil.isLoading.also { logText("Is already loading: $it") }) {
                BypassUtil.isChecking = false
                logText("Starting creation")
                BypassUtil.isLoading = true
                val snack = getSnackbarAnchor()?.showSnackbar("Creando bypass...", Snackbar.LENGTH_INDEFINITE)
                bypassLive.postValue(Pair(true, true))
                Log.e("CloudflareBypass", "is needed")
                runWebView(snack)
            } else {
                BypassUtil.isChecking = false
                logText("Creation not needed, aborting")
                bypassLive.postValue(Pair(false, false))
                Log.e("CloudflareBypass", "Not needed")
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun runWebView(snack: Snackbar?) {
        logText("Clearing cookies")
        BypassUtil.clearCookies()
        doOnUI(onLog = { logText("Error: $it") }) {
            logText("Searching webview")
            val webView = noCrashLet {
                findOptional<WrapWebView>(R.id.webview).also { if (it != null) logText("Use layout webview") }
                //?: AppWebView(App.context).also { logText("Use Application webview, not visible mode") }
            }
            if (webView != null) {
                logText("Applying settings for webview")
                webView.settings?.javaScriptEnabled = true
                webView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        shouldOverrideUrlLoading(view, request?.url?.toString())
                        return false
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        Log.e("CloudflareBypass", "Override $url")
                        logText("Override: $url")
                        logText("Waiting for resolve...")
                        if (url == "https://animeflv.net/") {
                            logText("Cookies for animeflv:")
                            logText(CookieManager.getInstance().getCookie(".animeflv.net"))
                            Log.e("CloudflareBypass", "Cookies: " + CookieManager.getInstance().getCookie("https://animeflv.net/"))
                            if (BypassUtil.saveCookies(App.context)) {
                                logText("Cookies saved")
                                webView.loadUrl("about:blank")
                                doAsync(exceptionHandler = { logText("Error: ${it.message}") }) {
                                    logText("Checking connection state")
                                    if (BypassUtil.isConnectionBlocked()) {
                                        if (tryCount < 4) {
                                            tryCount++
                                            logText("Connection blocked, retry connection... tries left: ${3 - tryCount}")
                                            runWebView(snack)
                                        } else {
                                            tryCount = 0
                                            logText("Connection was blocked, no tries left")
                                            BypassUtil.isLoading = false
                                            onBypassUpdated()
                                        }
                                    } else {
                                        doOnUI(onLog = { logText("Error: $it") }) {
                                            tryCount = 0
                                            logText("Connection was successful")
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
                            } else logText("cf_clearance not found")
                        }
                        return false
                    }
                }
                webView.settings?.userAgentString = (if (PrefsUtil.useDefaultUserAgent) {
                    noCrashLet { WebSettings.getDefaultUserAgent(App.context) }
                            ?: UAGenerator.getRandomUserAgent()
                } else UAGenerator.getRandomUserAgent()).also { PrefsUtil.userAgent = it }
                webView.loadUrl("https://animeflv.net/")
            } else {
                logText("Error finding suitable webview")
                snack?.safeDismiss()
                getSnackbarAnchor()?.showSnackbar("Error al iniciar WebView")
                onBypassUpdated()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        BypassUtil.isLoading = false
    }

    override fun onDestroy() {
        super.onDestroy()
        ServersFactory.clear()
    }

    companion object {
        val bypassLive: MutableLiveData<Pair<Boolean, Boolean>> = MutableLiveData()
    }
}