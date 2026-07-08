package knf.kuma.tv.ui

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.crashlytics.FirebaseCrashlytics
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PicassoSingle
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUI
import knf.kuma.commons.isTV
import knf.kuma.commons.toast
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.jobscheduler.RecentsWork
import knf.kuma.migration.MigrationActivity
import knf.kuma.recents.RecentsNotReceiver
import knf.kuma.retrofit.Repository
import knf.kuma.tv.TVBaseActivity
import knf.kuma.tv.TVServersFactory
import knf.kuma.uagen.randomUA
import knf.kuma.updater.UpdateActivity
import knf.kuma.updater.UpdateChecker
import knf.tools.bypass.startBypass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.jvm.java
import kotlin.time.Duration.Companion.seconds

class TVMain : TVBaseActivity(), TVServersFactory.ServersInterface, UpdateChecker.CheckListener {

    private var fragment: TVMainFragment? = null
    private var serversFactory: TVServersFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //SSLManager.disable()
        if (!isTV) {
            finish()
            startActivity(Intent(this, DesignUtils.mainClass))
            return
        }
        if (!PrefsUtil.isAV1DataMigrated) {
            lifecycleScope.launch(Dispatchers.IO) {
                val favs = CacheDB.INSTANCE.favsDAO().count
                val history = CacheDB.INSTANCE.recordsDAO().count
                val seen = CacheDB.INSTANCE.seenDAO().count
                val seeing = CacheDB.INSTANCE.seeingDAO().countAll
                if (favs > 0 || history > 0 || seen > 0 || seeing > 0) {
                    startActivity(Intent(this@TVMain, MigrationActivity::class.java).apply {
                        putExtra("is_tv", true)
                    })
                    finish()
                } else {
                    PrefsUtil.isAV1DataMigrated = true
                    withContext(Dispatchers.Main) {
                        start(savedInstanceState)
                    }
                }
            }
        } else {
            start(savedInstanceState)
        }
    }

    private fun start(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            fragment = TVMainFragment.get().also {
                addFragment(it)
            }
            RecentsNotReceiver.removeAll(this)
            UpdateChecker.check(this, this)
            RecentsWork.schedule(this@TVMain)
            lifecycleScope.launch(Dispatchers.IO) {
                installSecurityProvider()
            }
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
            } catch (e: Throwable) {
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
        return false
    }

    override fun onNeedUpdate(o_code: String, n_code: String) {
        runOnUiThread {
            UpdateActivity.start(this@TVMain, true, n_code)
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
                    BypassUtil.createRequest()
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
            val cookiesUpdated = data?.let {
                PrefsUtil.useDefaultUserAgent = false
                PrefsUtil.userAgent = it.getStringExtra("user_agent") ?: randomUA()
                BypassUtil.saveCookies(this, it.getStringExtra("cookies") ?: "null")
            } ?: false
            GenericActivity.bypassLive.postValue(Pair(first = cookiesUpdated, second = false))
            Repository().reloadRecents()
            BypassUtil.isLoading = false
            PicassoSingle.clear()
            RecentsWork.run()
            doOnUI {
                "Bypass actualizado".toast()
            }
        } else
            try {
                if (data != null)
                    if (resultCode == RESULT_OK) {
                        val bundle = data.extras
                        if (requestCode == TVServersFactory.REQUEST_CODE_MULTI)
                            serversFactory?.analyzeMulti(bundle?.getInt("position", 0) ?: 0)
                        else {
                            if (bundle?.getBoolean("is_video_server", false) == true)
                                serversFactory?.analyzeOption(bundle.getInt("position", 0))
                            else
                                serversFactory?.analyzeServer(bundle?.getInt("position", 0) ?: 0)
                        }
                    } else if (resultCode == RESULT_CANCELED && data.extras?.getBoolean(
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
