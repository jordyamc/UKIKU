package knf.kuma.news

import android.annotation.SuppressLint
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
import com.afollestad.materialdialogs.bottomsheets.setPeekHeight
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration
import com.google.android.material.snackbar.Snackbar
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.ads.showRandomInterstitial
import knf.kuma.commons.*
import knf.kuma.custom.GenericActivity
import kotlinx.android.synthetic.main.activity_news_material.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MaterialNewsActivity : GenericActivity(), SwipeRefreshLayout.OnRefreshListener {
    val model: NewsViewModel by viewModels()
    val adapter: MaterialNewsAdapter by lazy { MaterialNewsAdapter(this) }
    var snack: Snackbar? = null

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
        snack?.dismiss()
        if (Network.isConnected)
            lifecycleScope.launch {
                NewsRepository.getNews(getCategory()) { isEmpty, cause ->
                    if (isEmpty) {
                        error.visibility = View.VISIBLE
                        snack = recycler.showSnackbar("Error al cargar noticias: $cause", Snackbar.LENGTH_INDEFINITE, "reintentar") {
                            loadList()
                        }
                    } else {
                        error.visibility = View.GONE
                        recycler.scheduleLayoutAnimation()
                    }
                    runOnUiThread { refresh.isRefreshing = false }
                }.collect {
                    adapter.submitData(it)
                }
            }
        else {
            snack = recycler.showSnackbar("Sin internet", Snackbar.LENGTH_INDEFINITE)
        }
    }

    private fun getCategory(): String {
        return when (model.selectedFilter) {
            1 -> "categoria/noticias/anime"
            2 -> "categoria/noticias/cultura-otaku"
            3 -> "categoria/noticias/japon"
            4 -> "categoria/noticias/live-action"
            5 -> "categoria/noticias/manga"
            6 -> "categoria/noticias/mercancia-de-anime"
            7 -> "categoria/noticias/novelas-ligeras"
            8 -> "categoria/noticias/videojuegos"
            9 -> "categoria/resenas"
            else -> "noticias"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_news_filters, menu)
        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("CheckResult")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            lifecycleOwner(this@MaterialNewsActivity)
            title(text = "Filtros")
            setPeekHeight(99999999)
            listItemsSingleChoice(
                items = model.filtersList,
                initialSelection = model.selectedFilter,
                waitForPositiveButton = false
            ) { _, index, name ->
                supportActionBar?.title = if (index == 0) "Noticias" else name
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