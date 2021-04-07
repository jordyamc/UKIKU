package knf.kuma.tv.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.tvprovider.media.tv.*
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.commons.*
import knf.kuma.custom.GenericActivity
import knf.kuma.directory.DirManager
import knf.kuma.directory.DirectoryService
import knf.kuma.jobscheduler.DirUpdateWork
import knf.kuma.jobscheduler.RecentsWork
import knf.kuma.recents.RecentsNotReceiver
import knf.kuma.retrofit.Repository
import knf.kuma.tv.ChannelUtils
import knf.kuma.tv.TVBaseActivity
import knf.kuma.tv.TVServersFactory
import knf.kuma.uagen.randomUA
import knf.kuma.updater.UpdateActivity
import knf.kuma.updater.UpdateChecker
import knf.tools.bypass.startBypass
import kotlinx.android.synthetic.main.tv_activity_main.*
import kotlinx.coroutines.*
import org.jetbrains.anko.doAsync
import kotlin.contracts.ExperimentalContracts


@ExperimentalCoroutinesApi
@ExperimentalContracts
class TVMain : TVBaseActivity(), TVServersFactory.ServersInterface, UpdateChecker.CheckListener {

    private var fragment: TVMainFragment? = null
    private var serversFactory: TVServersFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.BUILD_TYPE == "playstore") {
            finish()
            startActivity(Intent(this, DesignUtils.mainClass))
            return
        }
        if (savedInstanceState == null) {
            fragment = TVMainFragment.get().also {
                addFragment(it)
            }
            RecentsWork.schedule(this)
            DirUpdateWork.schedule(this)
            RecentsNotReceiver.removeAll(this)
            UpdateChecker.check(this, this)
            lifecycleScope.launch(Dispatchers.Main) {
                if (PrefsUtil.mayUseRandomUA)
                    PrefsUtil.alwaysGenerateUA = !withContext(Dispatchers.IO) { doBlockTests() }
                else
                    PrefsUtil.alwaysGenerateUA = false
                if (withContext(Dispatchers.IO) { BypassUtil.isNeeded() }) {
                    startBypass(this@TVMain, 7425, "https://animeflv.net", true)
                }
            }
            lifecycleScope.launch(Dispatchers.IO) {
                DirManager.checkPreDir()
                DirectoryService.run(this@TVMain)
            }
            ChannelUtils.createIfNeeded(this)
        }
    }

    private fun doBlockTests(): Boolean {
        var blockCount = 0
        repeat(3) {
            if (BypassUtil.isCloudflareActiveRandom())
                blockCount++
            if (blockCount >= 2)
                return true
        }
        return blockCount >= 2
    }

    override fun onNeedUpdate(o_code: String, n_code: String) {
        runOnUiThread {
            UpdateActivity.start(this@TVMain, true)
        }
    }

    override fun onReady(serversFactory: TVServersFactory) {
        this.serversFactory = serversFactory
    }

    override fun onFinish(started: Boolean, success: Boolean) {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 7425) {
            val cookiesUpdated = data?.let {
                PrefsUtil.useDefaultUserAgent = false
                PrefsUtil.userAgent = it.getStringExtra("user_agent") ?: randomUA()
                BypassUtil.saveCookies(this, it.getStringExtra("cookies") ?: "null")
            } ?: false
            GenericActivity.bypassLive.postValue(Pair(first = cookiesUpdated, second = false))
            Repository().reloadAllRecents()
            BypassUtil.isLoading = false
            PicassoSingle.clear()
            doOnUI {
                "Bypass actualizado".toast()
            }
            ChannelUtils.initChannelIfNeeded(this)
            if (!PrefsUtil.isDirectoryFinished) {
                lifecycleScope.launch(Dispatchers.IO) {
                    DirManager.checkPreDir()
                    DirectoryService.run(this@TVMain)
                }
            }
        } else
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
                    } else if (resultCode == Activity.RESULT_CANCELED && data.extras?.getBoolean(
                            "is_video_server",
                            false
                        ) == true
                    )
                        serversFactory?.showServerList()
            } catch (e: Exception) {
                e.printStackTrace()
            }

    }

    override fun onBackPressed() {
        if (webview.isVisible) {
            webview.isVisible = false
            BypassUtil.isLoading = false
        } else
            super.onBackPressed()
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
                        webView.settings.javaScriptEnabled = true
                        webView.settings.domStorageEnabled = true
                        webView.settings.loadWithOverviewMode = true
                        webView.settings.useWideViewPort = true
                        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        webView.settings.setAppCacheEnabled(false)
                        webView.webChromeClient = WebChromeClient()
                        webView.webViewClient = object : WebViewClient() {

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                shouldOverrideUrlLoading(view, url)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                shouldOverrideUrlLoading(view, url)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return shouldOverrideUrlLoading(view, request?.url?.toString())
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                Log.e("CloudflareBypass", "Override $url")
                                Log.e("CloudflareBypass", "Cookies: " + CookieManager.getInstance().getCookie("https://animeflv.net/"))
                                if (BypassUtil.isLoading && BypassUtil.saveCookies(App.context)) {
                                    doAsync {
                                        if (BypassUtil.isNeededFlag() == 0) {
                                            doOnUI {
                                                "Bypass actualizado".toast()
                                                PicassoSingle.clear()
                                                Repository().reloadRecents()
                                                BypassUtil.isLoading = false
                                                webView.visibility = View.GONE
                                            }
                                        }
                                    }
                                }
                                return false
                            }
                        }
                        //webView.settings.userAgentString = randomUA().also { PrefsUtil.userAgent = it }
                        webView.settings.userAgentString = PrefsUtil.userAgent
                        webView.loadUrl("https://animeflv.net/")
                        Log.e("CloudflareBypass", "UA: ${PrefsUtil.userAgent}")
                    }
                } else {
                    Log.e("CloudflareBypass", "Not needed")
                }
            }
        }
    }

}
