package knf.kuma.recommended

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.bind
import knf.kuma.commons.verifyManager
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirectoryAV1PageAdapter
import knf.kuma.pojos.av1.GenreRecord
import knf.kuma.search.SearchFragmentMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.anko.find
import xdroid.toaster.Toaster

/**
 * Created by jordy on 26/03/2018.
 */

class RecommendActivity : GenericActivity() {
    val toolbar: Toolbar by bind(R.id.toolbar)
    val recyclerView: RecyclerView by bind(R.id.recycler)
    val error: LinearLayout by bind(R.id.error)
    val loading: LinearLayout by bind(R.id.loading)
    val state: TextView by bind(R.id.state)
    val adapter by lazy { DirectoryAV1PageAdapter(this@RecommendActivity as FragmentActivity) }

    private val layout: Int
        @LayoutRes
        get() = if (!isGrid) {
            if (DesignUtils.isFlat) R.layout.recycler_recommends_material else R.layout.recycler_recommends
        } else {
            if (DesignUtils.isFlat) R.layout.recycler_recommends_grid_material else R.layout.recycler_recommends_grid
        }

    private val isGrid: Boolean
        get() = PrefsUtil.layType != "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setContentView(layout)
        toolbar.title = "Sugeridos"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        find<FrameLayout>(R.id.adContainer).implBanner(AdsType.RECOMMEND_BANNER, true)
        recyclerView.verifyManager()
        recyclerView.adapter = adapter
        setAdapter()
    }

    private fun setAdapter() {
        lifecycleScope.launch {
            CacheDB.INSTANCE.genreRecordDAO().allFlow.distinctUntilChanged().collectLatest {
                try {
                    error.isVisible = false
                    loading.isVisible = true
                    val list = RecommendHelper.createRecommended()
                    loading.isVisible = false
                    error.isVisible = list.isEmpty()
                    adapter.submitData(PagingData.from(list))
                } catch (e: Exception) {
                    e.printStackTrace()
                    FirebaseCrashlytics.getInstance().recordException(e)
                    Toaster.toast("Error al cargar recomendados")
                    loading.isVisible = false
                    error.isVisible = true
                }
            }
        }
    }

    private fun showBlacklist() {
        lifecycleScope.launch(Dispatchers.Main) {
            val list = SearchFragmentMaterial.genres.map {
                CacheDB.INSTANCE.genreRecordDAO().findBySlug(it.slug) ?: GenreRecord(it.slug, it.name, 0)
            }
            val dialog = BlacklistDialog()
            dialog.init(list, object : BlacklistDialog.MultiChoiceListener {
                override fun onOkay(selected: List<GenreRecord>) {
                    setBlacklist(selected)
                }
            })
            dialog.show(supportFragmentManager, "Blacklist")
        }
    }

    private fun setBlacklist(selected: List<GenreRecord>) {
        lifecycleScope.launch {
            RecommendHelper.block(selected)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_suggestions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.blacklist -> showBlacklist()
            R.id.rating -> if (DesignUtils.isFlat) RankingActivityMaterial.open(this) else RankingActivity.open(this)
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, RecommendActivity::class.java))
        }
    }
}
