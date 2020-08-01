package knf.kuma.preferences

import android.os.Bundle
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.custom.GenericActivity
import kotlinx.android.synthetic.main.activity_ads_settings.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class AdsPreferenceActivity : GenericActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ads_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Configuracion de anuncios"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        switchNative.isChecked = PrefsUtil.isNativeAdsEnabled
        preferenceNative.onClick {
            switchNative.toggle()
            PrefsUtil.isNativeAdsEnabled = switchNative.isChecked
        }
        switchFull.isChecked = PrefsUtil.isFullAdsEnabled
        preferenceFullText.isEnabled = switchFull.isChecked
        sliderFull.isEnabled = switchFull.isChecked
        probabilityFull.isEnabled = switchFull.isChecked
        preferenceFull.onClick {
            switchFull.toggle()
            preferenceFullText.isEnabled = switchFull.isChecked
            sliderFull.isEnabled = switchFull.isChecked
            probabilityFull.isEnabled = switchFull.isChecked
        }
        sliderFull.value = PrefsUtil.fullAdsProbability
        probabilityFull.text = "${PrefsUtil.fullAdsProbability.toInt()}%"
        sliderFull.addOnChangeListener { _, value, _ ->
            probabilityFull.text = "${value.toInt()}%"
            PrefsUtil.fullAdsProbability = value
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}