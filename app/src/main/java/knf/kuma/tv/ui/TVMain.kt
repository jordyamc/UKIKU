package knf.kuma.tv.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import knf.kuma.ads.AdsUtils
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PicassoSingle
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUI
import knf.kuma.commons.isTV
import knf.kuma.commons.toast
import knf.kuma.custom.GenericActivity
import knf.kuma.custom.SSLManager
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
import knf.tools.bypass.DisplayType
import knf.tools.bypass.Request
import knf.tools.bypass.startBypass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts


@ExperimentalCoroutinesApi
@ExperimentalContracts
class TVMain : TVBaseActivity(), TVServersFactory.ServersInterface, UpdateChecker.CheckListener {

    private var fragment: TVMainFragment? = null
    private var serversFactory: TVServersFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SSLManager.disable()
        if (!isTV) {
            finish()
            startActivity(Intent(this, DesignUtils.mainClass))
            return
        }
        if (savedInstanceState == null) {
            fragment = TVMainFragment.get().also {
                addFragment(it)
            }
            DirUpdateWork.schedule(this)
            RecentsNotReceiver.removeAll(this)
            UpdateChecker.check(this, this)
            RecentsWork.schedule(this@TVMain)
            lifecycleScope.launch(Dispatchers.IO) {
                DirManager.checkPreDir()
                DirectoryService.run(this@TVMain)
                installSecurityProvider()
            }
            ChannelUtils.createIfNeeded(this)
        }
    }

    private suspend fun installSecurityProvider() {
        withContext(Dispatchers.IO) {
            try {
                ProviderInstaller.installIfNeeded(this@TVMain)
                PrefsUtil.isSecurityUpdated = true
                PrefsUtil.spErrorType = null
            } catch (e: GooglePlayServicesRepairableException) {
                PrefsUtil.isSecurityUpdated = false
                PrefsUtil.spErrorType = "Gplay services deshabilitado o desactualizado"
                e.printStackTrace()
            } catch (e: GooglePlayServicesNotAvailableException) {
                PrefsUtil.isSecurityUpdated = false
                PrefsUtil.spErrorType = "GPlay services no esta disponible"
                e.printStackTrace()
            } catch (e: Exception) {
                PrefsUtil.isSecurityUpdated = false
                //Toaster.toastLong("SProvider: Unknown error, ${e.message}")
                PrefsUtil.spErrorType = "Error desconocido: ${e.message}"
                e.printStackTrace()
            }
            if (!PrefsUtil.isSecurityUpdated && FirebaseCrashlytics.getInstance().didCrashOnPreviousExecution()) {
                PrefsUtil.spProtectionEnabled = true
                //Toaster.toastLong("Proteccion de SP reactivada")

            }
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

    override fun onUpdateNotRequired() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (PrefsUtil.mayUseRandomUA)
                PrefsUtil.alwaysGenerateUA = !withContext(Dispatchers.IO) { doBlockTests() }
            else
                PrefsUtil.alwaysGenerateUA = false
            if (withContext(Dispatchers.IO) { BypassUtil.isNeeded() }) {
                startBypass(
                    7425,
                    Request(
                        BypassUtil.testLink,
                        lastUA = PrefsUtil.userAgent,
                        showReload = AdsUtils.remoteConfigs.getBoolean("bypass_show_reload"),
                        useFocus = isTV,
                        maxTryCount = AdsUtils.remoteConfigs.getLong("bypass_max_tries").toInt(),
                        useLatestUA = true,
                        reloadOnCaptcha = AdsUtils.remoteConfigs.getBoolean("bypass_skip_captcha"),
                        clearCookiesAtStart = true,
                        displayType = DisplayType.DIALOG,
                        dialogStyle = AdsUtils.remoteConfigs.getLong("bypass_dialog_style").toInt()
                    )
                )
                //startBypass(this@TVMain, 7425, "https://www3.animeflv.net", true)
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
        if (requestCode == 7425) {
            if (resultCode == Activity.RESULT_OK) {
                Firebase.analytics.logEvent("bypass_success") {
                    param("user_agent", data?.getStringExtra("user_agent") ?: "empty")
                    param("bypass_time", data?.getLongExtra("finishTime", 0L) ?: 0L)
                }
            }
            val cookiesUpdated = data?.let {
                PrefsUtil.useDefaultUserAgent = false
                PrefsUtil.userAgent = it.getStringExtra("user_agent") ?: randomUA()
                BypassUtil.saveCookies(this, it.getStringExtra("cookies") ?: "null")
            } ?: false
            GenericActivity.bypassLive.postValue(Pair(first = cookiesUpdated, second = false))
            Repository().reloadAllRecents()
            BypassUtil.isLoading = false
            PicassoSingle.clear()
            RecentsWork.run()
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

}
