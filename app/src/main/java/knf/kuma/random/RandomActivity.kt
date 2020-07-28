package knf.kuma.random

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
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
import knf.kuma.commons.*
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find

class RandomActivity : GenericActivity(), SwipeRefreshLayout.OnRefreshListener {
    val toolbar: Toolbar by bind(R.id.toolbar)
    private val refreshLayout: SwipeRefreshLayout by bind(R.id.refresh)
    val recyclerView: RecyclerView by bind(R.id.recycler)
    private var adapter: RandomAdapter? = null
    private var counter = 0

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            R.layout.recycler_refresh
        } else {
            R.layout.recycler_refresh_grid
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
            find<FrameLayout>(R.id.adContainer).implBanner(AdsType.RANDOM_BANNER, true)
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
