package knf.kuma.tv.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.afollestad.materialdialogs.MaterialDialog
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import knf.kuma.commons.*
import knf.kuma.directory.DirectoryService
import knf.kuma.jobscheduler.DirUpdateJob
import knf.kuma.jobscheduler.RecentsJob
import knf.kuma.recents.RecentsNotReceiver
import knf.kuma.retrofit.Repository
import knf.kuma.tv.TVBaseActivity
import knf.kuma.tv.TVServersFactory
import knf.kuma.uagen.randomUA
import knf.kuma.updater.UpdateActivity
import knf.kuma.updater.UpdateChecker
import kotlinx.android.synthetic.main.tv_activity_main.*
import org.jetbrains.anko.doAsync

class TVMain : TVBaseActivity(), TVServersFactory.ServersInterface, UpdateChecker.CheckListener {

    private var fragment: TVMainFragment? = null
    private var serversFactory: TVServersFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState==null){
            fragment = TVMainFragment.get().also {
                addFragment(it)
            }
            DirectoryService.run(this)
            RecentsJob.schedule(this)
            DirUpdateJob.schedule(this)
            RecentsNotReceiver.removeAll(this)
            UpdateChecker.check(this, this)
            checkBypass()
            Answers.getInstance().logCustom(CustomEvent("TV UI"))
        }
    }

    override fun onNeedUpdate(o_code: String, n_code: String) {
        runOnUiThread {
            MaterialDialog(this@TVMain).safeShow {
                title(text = "Actualización")
                message(text = "Parece que la versión $n_code está disponible, ¿Quieres actualizar?")
                positiveButton(text = "si") {
                    UpdateActivity.start(this@TVMain)
                }
                negativeButton(text = "despues")
            }
        }
    }

    override fun onReady(serversFactory: TVServersFactory) {
        this.serversFactory = serversFactory
    }

    override fun onFinish(started: Boolean, success: Boolean) {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (data != null)
                if (resultCode == Activity.RESULT_OK) {
                    val bundle = data.extras
                    if (requestCode == TVServersFactory.REQUEST_CODE_MULTI)
                        serversFactory?.analyzeMulti(bundle?.getInt("position", 0) ?: 0)
                    else {
                        if (bundle?.getBoolean("is_video_server", false) == true)
                            serversFactory?.analyzeOption(bundle.getInt("position", 0))
                        else
                            serversFactory?.analyzeServer(bundle?.getInt("position", 0) ?: 0)
                    }
                } else if (resultCode == Activity.RESULT_CANCELED && data.extras?.getBoolean("is_video_server", false) == true)
                    serversFactory?.showServerList()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun checkBypass() {
        noCrash {
            val webView = webview
            doAsync {
                if (BypassUtil.isNeeded() && !BypassUtil.isLoading) {
                    "Creando bypass...".toast()
                    BypassUtil.isLoading = true
                    Log.e("CloudflareBypass", "is needed")
                    BypassUtil.clearCookies()
                    doOnUI {
                        webView.visibility = View.VISIBLE
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
                                    if (BypassUtil.saveCookies(this@TVMain)) {
                                        "Bypass actualizado".toast()
                                        PicassoSingle.clear()
                                        webView.visibility = View.GONE
                                    }
                                    Repository().reloadRecents()
                                    BypassUtil.isLoading = false
                                }
                                return false
                            }
                        }
                        webView.settings?.userAgentString = randomUA().also { PrefsUtil.userAgent = it }
                        webView.loadUrl("https://animeflv.net/")
                    }
                } else {
                    Log.e("CloudflareBypass", "Not needed")
                }
            }
        }
    }

}
