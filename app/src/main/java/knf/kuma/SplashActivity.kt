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
import io.netsocks.peer.NetsocksSdk
import knf.kuma.achievements.AchievementManager
import knf.kuma.ads.AdsUtils
import knf.kuma.ads.SubscriptionReceiver
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.isFullMode
import knf.kuma.custom.GenericActivity
import knf.kuma.tv.ui.TVMain
import knf.tools.signatures.getSignatures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xdroid.toaster.Toaster
import kotlin.contracts.ExperimentalContracts
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
@ExperimentalContracts
class SplashActivity : GenericActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AchievementManager.onAppStart()
        SubscriptionReceiver.check(intent)
        printSignatures()
        when {
            resources.getBoolean(R.bool.isTv) -> {
                startActivity(Intent(this, TVMain::class.java))
                finish()
            }
            /*!isFullMode && BuildConfig.BUILD_TYPE != "amazon" && !PrefsUtil.isPSWarned -> MaterialDialog(
                this
            ).safeShow {
                title(text = "Aviso")
                message(text = "Usted esta usando la version de Google Play, esta version tiene caracteristicas deshabilitadas, para una experiencia completa por favor use la version de la pagina oficial\nEscriba \"confirmar\" para continuar.")
                input(hint = "Respuesta...", waitForPositiveButton = false) { _, text ->
                    getActionButton(WhichButton.POSITIVE).isEnabled = text.toString()
                        .lowercase(Locale.getDefault()) == "confirmar"
                }
                negativeButton(text = "Web") {
                    noCrash {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ukiku.app")))
                    }
                }
                positiveButton(text = "Continuar") {
                    PrefsUtil.isPSWarned = true
                    startApp()
                }
                cancelOnTouchOutside(false)
            }*/
            else -> {
                lifecycleScope.launch {
                    showGDPR { startApp() }
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
        lifecycleScope.launch(Dispatchers.Main) {
            if (PrefsUtil.mayUseRandomUA)
                PrefsUtil.alwaysGenerateUA = !withContext(Dispatchers.IO) { doBlockTests() }
            else
                PrefsUtil.alwaysGenerateUA = false
            installSecurityProvider()
            DesignUtils.change(this@SplashActivity, start = false)
            AdsUtils.remoteConfigs.ensureInitialized().addOnCompleteListener {
                if (isFullMode && AdsUtils.isNetsocksEnabled) {
                    NetsocksSdk.enable(this@SplashActivity, "96F32F5B-DDC9-5A35-BBD0-6E222F420394")
                }
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