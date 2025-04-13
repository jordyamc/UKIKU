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
import com.google.android.material.snackbar.Snackbar
import ir.mahdiparastesh.chlm.SpacingItemDecoration
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.ads.showRandomInterstitial
import knf.kuma.commons.EAHelper
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.asPx
import knf.kuma.commons.setSurfaceBars
import knf.kuma.commons.showSnackbar
import knf.kuma.custom.GenericActivity
import knf.kuma.databinding.ActivityNewsMaterialBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MaterialNewsActivity : GenericActivity(), SwipeRefreshLayout.OnRefreshListener {
    val model: NewsViewModel by viewModels()
    private val binding by lazy { ActivityNewsMaterialBinding.inflate(layoutInflater) }
    val adapter: MaterialNewsAdapter by lazy { MaterialNewsAdapter(this) }
    var snack: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setSurfaceBars()
        setContentView(binding.root)
        binding.toolbar.title = "Noticias"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
        binding.refresh.setColorSchemeResources(EAHelper.getThemeColor(), EAHelper.getThemeColorLight(), R.color.colorPrimary)
        binding.refresh.setOnRefreshListener(this)
        binding.refresh.isRefreshing = true
        binding.recycler.adapter = adapter
        binding.recycler.addItemDecoration(SpacingItemDecoration(0, 20.asPx))
        loadList()
        lifecycleScope.launch(Dispatchers.IO) {
            delay(500)
            binding.adContainer.implBanner(AdsType.NEWS_BANNER, true)
        }
        showRandomInterstitial(this, PrefsUtil.fullAdsExtraProbability)
    }

    private fun loadList() {
        snack?.dismiss()
        if (Network.isConnected)
            lifecycleScope.launch {
                NewsRepository.getNews(getCategory()) { isEmpty, cause ->
                    if (isEmpty) {
                        binding.error.visibility = View.VISIBLE
                        snack = binding.recycler.showSnackbar("Error al cargar noticias: $cause", Snackbar.LENGTH_INDEFINITE, "reintentar") {
                            loadList()
                        }
                    } else {
                        binding.error.visibility = View.GONE
                        binding.recycler.scheduleLayoutAnimation()
                    }
                    runOnUiThread { binding.refresh.isRefreshing = false }
                }.collect {
                    adapter.submitData(it)
                }
            }
        else {
            snack = binding.recycler.showSnackbar("Sin internet", Snackbar.LENGTH_INDEFINITE)
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
        binding.refresh.isRefreshing = true
        loadList()
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, MaterialNewsActivity::class.java))
        }
    }
}