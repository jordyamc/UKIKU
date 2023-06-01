package knf.kuma.preferences

import android.os.Bundle
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.custom.GenericActivity
import knf.kuma.databinding.ActivityAdsSettingsBinding
import org.jetbrains.anko.sdk27.coroutines.onClick

class AdsPreferenceActivity : GenericActivity() {

    private val binding by lazy { ActivityAdsSettingsBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Configuracion de anuncios"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.switchNative.isChecked = PrefsUtil.isNativeAdsEnabled
        binding.preferenceNative.onClick {
            binding.switchNative.toggle()
            PrefsUtil.isNativeAdsEnabled = binding.switchNative.isChecked
        }
        binding.switchFull.isChecked = PrefsUtil.isFullAdsEnabled
        binding.preferenceFullText.isEnabled = binding.switchFull.isChecked
        binding.preferenceFullTextExtra.isEnabled = binding.switchFull.isChecked
        binding.sliderFull.isEnabled = binding.switchFull.isChecked
        binding.sliderFullExtra.isEnabled = binding.switchFull.isChecked
        binding.probabilityFull.isEnabled = binding.switchFull.isChecked
        binding.probabilityFullExtra.isEnabled = binding.switchFull.isChecked
        binding.preferenceFull.onClick {
            binding.switchFull.toggle()
            PrefsUtil.isFullAdsEnabled = binding.switchFull.isChecked
            binding.preferenceFullText.isEnabled = binding.switchFull.isChecked
            binding.preferenceFullTextExtra.isEnabled = binding.switchFull.isChecked
            binding.sliderFull.isEnabled = binding.switchFull.isChecked
            binding.sliderFullExtra.isEnabled = binding.switchFull.isChecked
            binding.probabilityFull.isEnabled = binding.switchFull.isChecked
            binding.probabilityFullExtra.isEnabled = binding.switchFull.isChecked
        }
        binding.sliderFull.value = PrefsUtil.fullAdsProbability
        binding.sliderFullExtra.value = PrefsUtil.fullAdsExtraProbability
        binding.probabilityFull.text = "${PrefsUtil.fullAdsProbability.toInt()}%"
        binding.probabilityFullExtra.text = "${PrefsUtil.fullAdsExtraProbability.toInt()}%"
        binding.sliderFull.addOnChangeListener { _, value, _ ->
            binding.probabilityFull.text = "${value.toInt()}%"
            PrefsUtil.fullAdsProbability = value
        }
        binding.sliderFullExtra.addOnChangeListener { _, value, _ ->
            binding.probabilityFullExtra.text = "${value.toInt()}%"
            PrefsUtil.fullAdsExtraProbability = value
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}