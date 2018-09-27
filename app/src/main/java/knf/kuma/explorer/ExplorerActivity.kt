package knf.kuma.explorer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import knf.kuma.R
import knf.kuma.commons.CastUtil
import knf.kuma.commons.EAHelper
import kotlinx.android.synthetic.main.activity_explorer.*

class ExplorerActivity : AppCompatActivity(), OnFileStateChange {
    private var adapter: ExplorerPagerAdapter? = null
    private var isExplorerFiles = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explorer)
        toolbar.title = "Explorador"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        toolbar?.setNavigationOnClickListener { onBackPressed() }
        if (savedInstanceState == null)
            ExplorerCreator.onDestroy()
        pager.offscreenPageLimit = 2
        adapter = ExplorerPagerAdapter(this, supportFragmentManager)
        pager.adapter = adapter
        tabs.setupWithViewPager(pager)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_explorer_connected, menu)
        if (isExplorerFiles)
            menu.findItem(R.id.delete_all).isVisible = false
        if (!CastUtil.get().connected())
            menu.findItem(R.id.casting).isVisible = false
        else {
            CastUtil.get().casting.observe(this, Observer { s ->
                try {
                    if (s == CastUtil.NO_PLAYING) {
                        menu.findItem(R.id.casting).isEnabled = false
                    } else {
                        menu.findItem(R.id.casting).isEnabled = true
                        menu.findItem(R.id.casting).setOnMenuItemClickListener {
                            CastUtil.get().openControls()
                            true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_all -> adapter!!.onRemoveAllClicked()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onChange(isFile: Boolean) {
        isExplorerFiles = isFile
        invalidateOptionsMenu()
    }

    override fun onBackPressed() {
        if (adapter == null || !(adapter!!.getItem(pager!!.currentItem) as FragmentBase).onBackPressed())
            super.onBackPressed()
    }

    companion object {
        @JvmStatic
        fun open(context: Context) {
            context.startActivity(Intent(context, ExplorerActivity::class.java))
        }
    }
}
