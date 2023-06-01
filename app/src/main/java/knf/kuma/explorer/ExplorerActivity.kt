package knf.kuma.explorer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import knf.kuma.R
import knf.kuma.ads.showRandomInterstitial
import knf.kuma.commons.CastUtil
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.custom.GenericActivity
import knf.kuma.databinding.ActivityExplorerBinding
import knf.kuma.download.FileAccessHelper
import kotlinx.coroutines.launch
import xdroid.toaster.Toaster

class ExplorerActivity : GenericActivity(), OnFileStateChange {
    private val binding by lazy { ActivityExplorerBinding.inflate(layoutInflater) }
    private var adapter: ExplorerPagerAdapter? = null
    private var isExplorerFiles = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.toolbar.title = "Explorador"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
        if (savedInstanceState == null)
            ExplorerCreator.onDestroy()
        binding.pager.offscreenPageLimit = 2
        adapter = ExplorerPagerAdapter(this, supportFragmentManager)
        binding.pager.adapter = adapter
        binding.tabs.setupWithViewPager(binding.pager)
        lifecycleScope.launch {
            if (!FileAccessHelper.isStoragePermissionEnabledAsync())
                Toaster.toastLong("¡Se necesita el permiso de almacenamiento!")
        }
        showRandomInterstitial(this, PrefsUtil.fullAdsExtraProbability)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_explorer_connected, menu)
        if (isExplorerFiles)
            menu.findItem(R.id.delete_all).isVisible = false
        CastUtil.registerActivity(this, menu, R.id.castMenu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_all -> adapter?.onRemoveAllClicked()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onChange(isFile: Boolean) {
        isExplorerFiles = isFile
        invalidateOptionsMenu()
    }

    override fun onDestroy() {
        super.onDestroy()
        ThumbServer.stop()
    }

    override fun onBackPressed() {
        adapter?.let {
            if ((it.getItem(binding.pager.currentItem) as? FragmentBase)?.onBackPressed() == false)
                super.onBackPressed()
        } ?: super.onBackPressed()
    }

    companion object {
        @JvmStatic
        fun open(context: Context) {
            context.startActivity(Intent(context, ExplorerActivity::class.java))
        }
    }
}
