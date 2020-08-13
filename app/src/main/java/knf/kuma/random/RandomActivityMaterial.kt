package knf.kuma.random

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.*
import knf.kuma.custom.BannerContainerView
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import kotlinx.android.synthetic.main.dialog_random_picker.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find

class RandomActivityMaterial : GenericActivity(), SwipeRefreshLayout.OnRefreshListener {
    val toolbar: Toolbar by bind(R.id.toolbar)
    private val refreshLayout: SwipeRefreshLayout by bind(R.id.refresh)
    val recyclerView: RecyclerView by bind(R.id.recycler)
    private var adapter: RandomAdapterMaterial? = null
    private var counter = 0

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            R.layout.recycler_refresh_material
        } else {
            R.layout.recycler_refresh_grid_material
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setSurfaceBars()
        setContentView(layout)
        toolbar.title = "Random"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        refreshLayout.setOnRefreshListener(this)
        adapter = RandomAdapterMaterial(this)
        recyclerView.verifyManager()
        recyclerView.adapter = adapter
        refreshLayout.isRefreshing = true
        refreshLayout.setColorSchemeResources(EAHelper.getThemeColor(), EAHelper.getThemeColorLight(), R.color.colorPrimary)
        refreshList()
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            find<BannerContainerView>(R.id.adContainer).implBanner(AdsType.RANDOM_BANNER, true)
        }
    }

    private fun refreshList() {
        counter++
        if (counter >= 15)
            AchievementManager.unlock(listOf(32))
        doAsync {
            val list = CacheDB.INSTANCE.animeDAO().getRandom(PrefsUtil.randomLimit)
            doOnUI {
                refreshLayout.isRefreshing = false
                adapter?.update(list)
                recyclerView.scheduleLayoutAnimation()
            }
        }
    }

    override fun onRefresh() {
        refreshList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_random, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        AlertDialog.Builder(this).apply {
            setTitle("Numero de resultados")
            val view = inflate(this@RandomActivityMaterial, R.layout.dialog_random_picker)
            view.picker.value = PrefsUtil.randomLimit
            setView(view)
            setPositiveButton("OK") { _, _ ->
                PrefsUtil.randomLimit = view.picker.value
                refreshLayout.post { refreshLayout.isRefreshing = true }
                refreshList()
            }
        }.show()
        return super.onOptionsItemSelected(item)
    }

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, RandomActivityMaterial::class.java))
        }
    }
}
