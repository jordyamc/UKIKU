package knf.kuma.random

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.github.stephenvinouze.materialnumberpickercore.MaterialNumberPicker
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.EAHelper
import knf.kuma.commons.JsExtractor
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.bind
import knf.kuma.commons.safeShow
import knf.kuma.commons.verifyManager
import knf.kuma.custom.BannerContainerView
import knf.kuma.custom.GenericActivity
import knf.kuma.pojos.av1.DirectoryAV1Min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.find
import org.nield.kotlinstatistics.randomDistinct
import kotlin.math.min

class RandomActivity : GenericActivity(), SwipeRefreshLayout.OnRefreshListener {
    val toolbar: Toolbar by bind(R.id.toolbar)
    private val refreshLayout: SwipeRefreshLayout by bind(R.id.refresh)
    val recyclerView: RecyclerView by bind(R.id.recycler)
    private var adapter: RandomAdapter? = null
    var counter = PrefsUtil.randomRefresh
    var isLoading = false

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            if (DesignUtils.isFlat) R.layout.recycler_refresh_material else R.layout.recycler_refresh
        } else {
            if (DesignUtils.isFlat) R.layout.recycler_refresh_grid_material else R.layout.recycler_refresh_grid
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setContentView(layout)
        toolbar.title = "Random"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        refreshLayout.setOnRefreshListener(this)
        adapter = RandomAdapter(this)
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
        if (isLoading) return
        counter ++
        PrefsUtil.randomRefresh = counter
        if (counter >= 15)
            AchievementManager.unlock(listOf(32))
        lifecycleScope.launch {
            val list = createList()
            withContext(Dispatchers.Main) {
                refreshLayout.isRefreshing = false
                adapter?.update(list)
                recyclerView.scheduleLayoutAnimation()
            }
        }
    }

    private suspend fun createList(): List<DirectoryAV1Min> {
        val list = mutableListOf<DirectoryAV1Min>()
        val checked = mutableListOf<Int>()
        val randomLimit = PrefsUtil.randomLimit
        while (list.size < randomLimit) {
            val page = (1..50).random()
            if (checked.contains(page)) continue
            checked.add(page)
            val data = JsExtractor.processLink("https://animeav1.com/catalogo?page=$page") ?: continue
            val indexes = (0..<data.length()).toList().randomDistinct(min(5, randomLimit - list.size))
            Log.e("Random", "Take $indexes from page $page")
            for (i in indexes) {
                list.add(DirectoryAV1Min.fromJson(data.getJSONObject(i)))
            }
        }
        return list.shuffled()
    }

    override fun onRefresh() {
        refreshList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_random, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val picker = MaterialNumberPicker(
                this, 5, 100,
                PrefsUtil.randomLimit,
                ContextCompat.getColor(this, R.color.colorAccent),
                ContextCompat.getColor(this, R.color.textPrimary),
                resources.getDimensionPixelSize(R.dimen.num_picker))
        MaterialDialog(this@RandomActivity).safeShow {
            title(text = "Numero de resultados")
            customView(view = picker, scrollable = false)
            positiveButton(text = "OK") {
                PrefsUtil.randomLimit = picker.value
                refreshLayout.post { refreshLayout.isRefreshing = true }
                refreshList()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, RandomActivity::class.java))
        }
    }
}
