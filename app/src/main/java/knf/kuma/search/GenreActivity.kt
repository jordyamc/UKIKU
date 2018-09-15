package knf.kuma.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import kotlinx.android.synthetic.main.recycler_genre.*

class GenreActivity : AppCompatActivity() {
    private var adapter: GenreAdapter? = null
    private var isFirst = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recycler_genre)
        toolbar.title = intent.getStringExtra("name")
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_fall_down)
        adapter = GenreAdapter(this)
        recycler.adapter = adapter
        LivePagedListBuilder<Int, AnimeObject>(CacheDB.INSTANCE.animeDAO().getAllGenre("%" + intent.getStringExtra("name") + "%"), 25).build().observe(this, Observer { animeObjects ->
            adapter!!.submitList(animeObjects)
            if (isFirst) {
                progress.visibility = View.GONE
                isFirst = false
                recycler.scheduleLayoutAnimation()
            }
        })
    }

    companion object {

        fun open(context: Context, name: String) {
            val intent = Intent(context, GenreActivity::class.java)
            intent.putExtra("name", name)
            context.startActivity(intent)
        }
    }
}
