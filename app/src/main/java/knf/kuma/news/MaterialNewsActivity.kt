package knf.kuma.news

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.EAHelper
import knf.kuma.commons.asPx
import knf.kuma.custom.GenericActivity
import kotlinx.android.synthetic.main.activity_news.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MaterialNewsActivity : GenericActivity(), SwipeRefreshLayout.OnRefreshListener {
    val adapter: MaterialNewsAdapter by lazy { MaterialNewsAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)
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
    }

    private fun loadList() {
        adapter.submitList(NewsRepository.getNews {
            if (it) {
                error.visibility = View.VISIBLE
            } else {
                error.visibility = View.GONE
                recycler.scheduleLayoutAnimation()
            }
            runOnUiThread { refresh.isRefreshing = false }
        })
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