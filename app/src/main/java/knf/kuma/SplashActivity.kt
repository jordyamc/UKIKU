package knf.kuma

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import knf.kuma.achievements.AchievementManager
import knf.kuma.ads.SubscriptionReceiver
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.safeShow
import knf.kuma.custom.GenericActivity
import knf.kuma.tv.ui.TVMain
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
                message(text = "Usted esta usando la version de Google Play, esta version tiene las descargas y streaming deshabilitados debido al copyright, para una experiencia completa por favor use la version de la pagina oficial")
                positiveButton(text = "Descargar") {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ukiku.ga")))
                }
                negativeButton(text = "Continuar") {
                    PrefsUtil.isPSWarned = true
                    startActivity(Intent(this@SplashActivity, Main::class.java))
                    finish()
                }
            }
            else -> {
                startActivity(Intent(this, Main::class.java))
                finish()
            }
        }
    }
}