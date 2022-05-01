package knf.kuma

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.input.input
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.crashlytics.FirebaseCrashlytics
import knf.kuma.achievements.AchievementManager
import knf.kuma.ads.SubscriptionReceiver
import knf.kuma.commons.*
import knf.kuma.custom.GenericActivity
import knf.kuma.tv.ui.TVMain
import knf.tools.signatures.getSignatures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xdroid.toaster.Toaster
import java.util.*
import kotlin.contracts.ExperimentalContracts

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
            !isFullMode && BuildConfig.BUILD_TYPE != "amazon" && !PrefsUtil.isPSWarned -> MaterialDialog(
                this
            ).safeShow {
                title(text = "Aviso")
                message(text = "Usted esta usando la version de Google Play, esta version tiene las descargas y streaming deshabilitados debido al copyright, para una experiencia completa por favor use la version de la pagina oficial\nEscriba \"confirmar\" para continuar.")
                input(hint = "Respuesta...", waitForPositiveButton = false) { _, text ->
                    getActionButton(WhichButton.POSITIVE).isEnabled = text.toString()
                        .lowercase(Locale.getDefault()) == "confirmar"
                }
                negativeButton(text = "Descargar") {
                    noCrash {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ukiku.app")))
                    }
                }
                positiveButton(text = "Continuar") {
                    PrefsUtil.isPSWarned = true
                    startApp()
                }
                cancelOnTouchOutside(false)
            }
            else -> {
                startApp()
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
            startActivity(Intent(this@SplashActivity, DesignUtils.mainClass))
            finish()
        }
    }
}