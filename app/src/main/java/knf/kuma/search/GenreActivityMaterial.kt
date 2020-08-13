package knf.kuma.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.pagedConfig
import knf.kuma.commons.setSurfaceBars
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import kotlinx.android.synthetic.main.recycler_genre.*

class GenreActivityMaterial : GenericActivity() {
    private val adapter: GenreAdapterMaterial by lazy { GenreAdapterMaterial(this) }
    private var isFirst = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setSurfaceBars()
        setContentView(R.layout.recycler_genre_material)
        toolbar.title = intent.getStringExtra("name")
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_fall_down)
        recycler.adapter = adapter
        LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getAllGenre("%" + intent.getStringExtra("name") + "%"), pagedConfig(25)).build().observe(this, Observer { animeObjects ->
            adapter.submitList(animeObjects)
            if (isFirst) {
                progress.visibility = View.GONE
                isFirst = false
                recycler.scheduleLayoutAnimation()
            }
        })
    }

    companion object {

        fun open(context: Context, name: String) {
            val intent = Intent(context, GenreActivityMaterial::class.java)
            intent.putExtra("name", name)
            context.startActivity(intent)
        }
    }
}
