package knf.kuma

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.input.input
import knf.kuma.achievements.AchievementManager
import knf.kuma.ads.SubscriptionReceiver
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.safeShow
import knf.kuma.custom.GenericActivity
import knf.kuma.tv.ui.TVMain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*
import kotlin.contracts.ExperimentalContracts

@ExperimentalCoroutinesApi
@ExperimentalContracts
class SplashActivity : GenericActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AchievementManager.onAppStart()
        SubscriptionReceiver.check(intent)
        when {
            resources.getBoolean(R.bool.isTv) -> {
                startActivity(Intent(this, TVMain::class.java))
                finish()
            }
            BuildConfig.BUILD_TYPE == "playstore" && !PrefsUtil.isPSWarned -> MaterialDialog(this).safeShow {
                title(text = "Aviso")
                message(text = "Usted esta usando la version de Google Play, esta version tiene las descargas y streaming deshabilitados debido al copyright, para una experiencia completa por favor use la version de la pagina oficial\nEscriba \"confirmar\" para continuar.")
                input(hint = "Respuesta...", waitForPositiveButton = false) { _, text ->
                    getActionButton(WhichButton.POSITIVE).isEnabled = text.toString().toLowerCase(Locale.getDefault()) == "confirmar"
                }
                negativeButton(text = "Descargar") {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ukiku.ga")))
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

    private fun startApp(){
        DesignUtils.change(this, start = false)
        startActivity(Intent(this@SplashActivity, DesignUtils.mainClass))
        finish()
    }
}