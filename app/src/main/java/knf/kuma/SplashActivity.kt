package knf.kuma

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.crashlytics.FirebaseCrashlytics
import knf.kuma.achievements.AchievementManager
import knf.kuma.ads.AdsUtils
import knf.kuma.ads.SubscriptionReceiver
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PrefsUtil
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.explorer.ExplorerCreator
import knf.kuma.migration.MigrationActivity
import knf.kuma.tv.ui.TVMain
import knf.tools.signatures.getSignatures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xdroid.toaster.Toaster
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.seconds


class SplashActivity : GenericActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blank)
        AchievementManager.onAppStart()
        SubscriptionReceiver.check(intent)
        printSignatures()
        route()
    }

    private fun route() {
        when {
            !PrefsUtil.isAV1DataMigrated -> {
                checkNeedMigration()
            }
            resources.getBoolean(R.bool.isTv) -> {
                startActivity(Intent(this, TVMain::class.java))
                finish()
            }
            else -> {
                lifecycleScope.launch {
                    showGDPR { startApp() }
                }
            }
        }
    }

    private fun checkNeedMigration() {
        lifecycleScope.launch(Dispatchers.IO) {
            val favs = CacheDB.INSTANCE.favsDAO().count
            val history = CacheDB.INSTANCE.recordsDAO().count
            val seen = CacheDB.INSTANCE.seenDAO().count
            val seeing = CacheDB.INSTANCE.seeingDAO().countAll
            if (favs > 0 || history > 0 || seen > 0 || seeing > 0) {
                startActivity(Intent(this@SplashActivity, MigrationActivity::class.java))
                finish()
            } else {
                PrefsUtil.isAV1DataMigrated = true
                delay(1.seconds)
                withContext(Dispatchers.Main) {
                    route()
                }
            }
        }
    }

    private suspend fun showGDPR(onFinish: () -> Unit) {
        val consentInfo = UserMessagingPlatform.getConsentInformation(this)
        val params = ConsentRequestParameters.Builder().apply {
            setTagForUnderAgeOfConsent(false)
        }.build()
        suspendCoroutine {
            val ok = { it.resume(true) }
            consentInfo.requestConsentInfoUpdate(this, params, { ok() }, { ok() })
        }
        Log.e("GDPR", "On consent, status: ${consentInfo.consentStatus}, available: ${consentInfo.isConsentFormAvailable}")
        if (consentInfo.consentStatus == ConsentInformation.ConsentStatus.REQUIRED && consentInfo.isConsentFormAvailable) {
            val form = suspendCoroutine { continuation ->
                UserMessagingPlatform.loadConsentForm(this,
                    {
                        continuation.resume(it)
                    },
                    {
                        continuation.resume(null)
                    }
                )
            }
            form?.show(this) {
                Log.e("GDPR", "On form dismiss, obtained: ${consentInfo.consentStatus == ConsentInformation.ConsentStatus.OBTAINED}")
                onFinish()
            }
        } else {
            onFinish()
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

    private fun printSignatures() {
        if (BuildConfig.DEBUG) {
            getSignatures().signatures.forEach {
                Log.e("Signature", it.encoded)
            }
        }
    }

    private suspend fun installSecurityProvider() {
        withContext(Dispatchers.IO) {
            try {
                ProviderInstaller.installIfNeeded(this@SplashActivity)
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
                Toaster.toastLong("SProvider: Unknown error, ${e.message}")
                PrefsUtil.spErrorType = "Error desconocido: ${e.message}"
                e.printStackTrace()
            }
            if (!PrefsUtil.isSecurityUpdated && FirebaseCrashlytics.getInstance().didCrashOnPreviousExecution()) {
                PrefsUtil.spProtectionEnabled = true
                Toaster.toastLong("Proteccion de SP reactivada")

            }
        }
    }

    private fun startApp() {
        ExplorerCreator.migrateDownloads()
        lifecycleScope.launch(Dispatchers.Main) {
            if (PrefsUtil.mayUseRandomUA)
                PrefsUtil.alwaysGenerateUA = !withContext(Dispatchers.IO) { doBlockTests() }
            else
                PrefsUtil.alwaysGenerateUA = false
            installSecurityProvider()
            DesignUtils.change(this@SplashActivity, start = false)
            AdsUtils.remoteConfigs.ensureInitialized().addOnCompleteListener {
                var initializated = false
                AdsUtils.setUp(this@SplashActivity) {
                    if (!initializated) {
                        initializated = true
                        startActivity(Intent(this@SplashActivity, DesignUtils.mainClass))
                        finish()
                    }
                }
            }
        }
    }
}