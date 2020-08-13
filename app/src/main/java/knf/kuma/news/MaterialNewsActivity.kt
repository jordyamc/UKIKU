package knf.kuma.news

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.ads.showRandomInterstitial
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.asPx
import knf.kuma.commons.setSurfaceBars
import knf.kuma.custom.GenericActivity
import kotlinx.android.synthetic.main.activity_news.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MaterialNewsActivity : GenericActivity(), SwipeRefreshLayout.OnRefreshListener {
    val model: NewsViewModel by viewModels()
    val adapter: MaterialNewsAdapter by lazy { MaterialNewsAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setSurfaceBars()
        setContentView(R.layout.activity_news_material)
        toolbar.title = "Noticias"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        refresh.setColorSchemeResources(EAHelper.getThemeColor(), EAHelper.getThemeColorLight(), R.color.colorPrimary)
        refresh.setOnRefreshListener(this)
        refresh.isRefreshing = true
        recycler.adapter = adapter
        recycler.addItemDecoration(SpacingItemDecoration(0, 20.asPx))
        loadList()
        lifecycleScope.launch(Dispatchers.IO) {
            delay(500)
            adContainer.implBanner(AdsType.NEWS_BANNER, true)
        }
        showRandomInterstitial(this, PrefsUtil.fullAdsExtraProbability)
    }

    private fun loadList() {
        adapter.submitList(NewsRepository.getNews(getCategory()) {
            if (it) {
                error.visibility = View.VISIBLE
            } else {
                error.visibility = View.GONE
                recycler.scheduleLayoutAnimation()
            }
            runOnUiThread { refresh.isRefreshing = false }
        })
    }

    private fun getCategory(): String {
        return when (model.selectedFilter) {
            1 -> "categoria/noticias/anime"
            2 -> "categoria/noticias/cine"
            3 -> "categoria/noticias/cultura-otaku"
            4 -> "categoria/noticias/japon"
            5 -> "categoria/noticias/live-action"
            6 -> "categoria/noticias/manga"
            7 -> "categoria/noticias/mercancia-de-anime"
            8 -> "categoria/noticias/musica"
            9 -> "categoria/noticias/novelas-ligeras"
            10 -> "categoria/noticias/videojuegos"
            11 -> "categoria/resenas"
            12 -> "categoria/eventos"
            else -> "listado-noticias"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_news_filters, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        MaterialDialog(this,BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            lifecycleOwner(this@MaterialNewsActivity)
            title(text = "Filtros")
            listItemsSingleChoice(items = model.filtersList, initialSelection = model.selectedFilter, waitForPositiveButton = false) { _, index, _ ->
                model.selectedFilter = index
                loadList()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRefresh() {
        refresh.isRefreshing = true
        loadList()
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, MaterialNewsActivity::class.java))
        }
    }
}