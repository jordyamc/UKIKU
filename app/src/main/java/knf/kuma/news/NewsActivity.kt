package knf.kuma.news

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.asPx
import knf.kuma.custom.GenericActivity
import knf.kuma.databinding.ActivityNewsBinding

class NewsActivity : GenericActivity(), SwipeRefreshLayout.OnRefreshListener {
    private val binding by lazy { ActivityNewsBinding.inflate(layoutInflater) }
    val adapter: NewsAdapter by lazy { NewsAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
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
        binding.recycler.addItemDecoration(SpacingItemDecoration(0, 10.asPx))
        NewsCreator.createNews().observe(this) {
            if (it == null || it.isEmpty())
                binding.error.visibility = View.VISIBLE
            else {
                binding.error.visibility = View.GONE

                adapter.update(it)
                binding.recycler.scheduleLayoutAnimation()
            }
            binding.refresh.isRefreshing = false
        }
        if (!PrefsUtil.isNativeAdsEnabled)
            binding.adContainer.implBanner(AdsType.NEWS_BANNER)
    }

    override fun onRefresh() {
        binding.refresh.isRefreshing = true
        NewsCreator.reload()
    }

    override fun onDestroy() {
        super.onDestroy()
        NewsCreator.destroy()
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, NewsActivity::class.java))
        }
    }
}