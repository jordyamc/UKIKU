package knf.kuma.emision

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.tabs.TabLayout
import knf.kuma.R
import knf.kuma.ads.showRandomInterstitial
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.setSurfaceBars
import knf.kuma.custom.GenericActivity
import knf.kuma.databinding.ActivityEmisionMaterialBinding
import java.util.Calendar

class EmissionActivityMaterial : GenericActivity(), TabLayout.OnTabSelectedListener {
    private var pagerAdapter: EmissionPagerAdapterMaterial? = null
    private val binding by lazy { ActivityEmisionMaterialBinding.inflate(layoutInflater) }

    private val currentDay: Int
        get() {
            var day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            day--
            return when (day) {
                0 -> 7
                else -> day
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setSurfaceBars()
        setContentView(binding.root)
        binding.toolbar.title = "Emisión"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.pager.offscreenPageLimit = 7
        pagerAdapter = EmissionPagerAdapterMaterial(supportFragmentManager)
        binding.pager.adapter = pagerAdapter
        binding.tabs.setupWithViewPager(binding.pager)
        binding.tabs.addOnTabSelectedListener(this)
        binding.pager.setCurrentItem(currentDay - 1, true)
        EAHelper.clear2()
        showRandomInterstitial(this,PrefsUtil.fullAdsExtraProbability)
    }

    override fun onResume() {
        super.onResume()
        pagerAdapter?.updateChanges()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_emision, menu)
        if (PrefsUtil.emissionShowHidden)
            menu.findItem(R.id.action_hideshow).setIcon(R.drawable.ic_hide_pref)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_hideshow -> {
                Log.e("Emission", "On menu click")
                val show = PrefsUtil.emissionShowHidden
                PrefsUtil.emissionShowHidden = !show
                pagerAdapter?.reloadPages()
            }
        }
        invalidateOptionsMenu()
        return super.onOptionsItemSelected(item)
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        EAHelper.enter2(getDayByPos(tab.position).toString())
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {

    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        EAHelper.enter2(getDayByPos(tab.position).toString())
    }

    private fun getDayByPos(position: Int): Int {
        var pos = position + 2
        if (pos == 8) pos = 1
        return pos
    }

    companion object {

        fun open(context: Activity) {
            context.startActivityForResult(Intent(context, EmissionActivityMaterial::class.java), 4987)
        }
    }
}
