package knf.kuma.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import kotlinx.android.synthetic.main.recycler_genre.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GenreActivity : GenericActivity() {
    private var adapter: GenreAdapter? = null
    private var isFirst = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recycler_genre)
        toolbar.title = intent.getStringExtra("name")
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_fall_down)
        adapter = GenreAdapter(this)
        recycler.adapter = adapter
        lifecycleScope.launch {
            Pager(
                config = PagingConfig(25), 0,
                CacheDB.INSTANCE.animeDAO().getAllGenre("%" + intent.getStringExtra("name") + "%").asPagingSourceFactory()
            ).flow.collectLatest {
                adapter?.submitData(it)
                if (isFirst) {
                    progress.visibility = View.GONE
                    isFirst = false
                    recycler.scheduleLayoutAnimation()
                }
            }
        }
    }

    companion object {

        fun open(context: Context, name: String) {
            val intent = Intent(context, GenreActivity::class.java)
            intent.putExtra("name", name)
            context.startActivity(intent)
        }
    }
}
