package knf.kuma.random

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.github.stephenvinouze.materialnumberpickercore.MaterialNumberPicker
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.EAHelper
import knf.kuma.commons.bind
import knf.kuma.commons.safeShow
import knf.kuma.commons.verifyManager
import knf.kuma.database.CacheDB

class RandomActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    val toolbar: Toolbar by bind(R.id.toolbar)
    private val refreshLayout: SwipeRefreshLayout by bind(R.id.refresh)
    val recyclerView: RecyclerView by bind(R.id.recycler)
    private var adapter: RandomAdapter? = null
    private var counter = 0

    private val layout: Int
        @LayoutRes
        get() = if (PreferenceManager.getDefaultSharedPreferences(this).getString("lay_type", "0") == "0") {
            R.layout.recycler_refresh
        } else {
            R.layout.recycler_refresh_grid
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
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
        refreshLayout.setColorSchemeResources(EAHelper.getThemeColor(this), EAHelper.getThemeColorLight(this), R.color.colorPrimary)
        refreshList()
    }

    private fun refreshList() {
        counter++
        if (counter >= 15)
            AchievementManager.unlock(32)
        Handler().postDelayed({
            CacheDB.INSTANCE.animeDAO().getRandom(PreferenceManager.getDefaultSharedPreferences(this@RandomActivity).getInt("random_limit", 25))
                    .observe(this@RandomActivity, Observer { animeObjects ->
                        refreshLayout.isRefreshing = false
                        adapter?.update(animeObjects)
                        recyclerView.scheduleLayoutAnimation()
                    })
        }, 1200)
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
                PreferenceManager.getDefaultSharedPreferences(this).getInt("random_limit", 25),
                ContextCompat.getColor(this, R.color.colorAccent),
                ContextCompat.getColor(this, R.color.textPrimary),
                resources.getDimensionPixelSize(R.dimen.num_picker))
        MaterialDialog(this@RandomActivity).safeShow {
            title(text = "Numero de resultados")
            customView(view = picker, scrollable = false)
            positiveButton(text = "OK") {
                PreferenceManager.getDefaultSharedPreferences(this@RandomActivity).edit().putInt("random_limit", picker.value).apply()
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
