package knf.kuma.seeing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.material.snackbar.Snackbar
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.safeDismiss
import knf.kuma.database.CacheDB
import knf.kuma.pojos.SeeingObject

class SeeingActivity : AppCompatActivity() {
    @BindView(R.id.toolbar)
    lateinit var toolbar: Toolbar
    @BindView(R.id.recycler)
    lateinit var recyclerView: RecyclerView
    @BindView(R.id.error)
    lateinit var error: View
    private var adapter: SeeingAdapter? = null
    private var internalMove = false
    private var isFirst = true

    private val layout: Int
        @LayoutRes
        get() = if (PreferenceManager.getDefaultSharedPreferences(this).getString("lay_type", "0") == "0") {
            R.layout.activity_seening
        } else {
            R.layout.activity_seening_grid
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(layout)
        ButterKnife.bind(this)
        toolbar.title = "Siguiendo"
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }
        adapter = SeeingAdapter(this)
        recyclerView.adapter = adapter
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.RIGHT, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val seeingObject = adapter!!.list[position]
                internalMove = true
                adapter!!.remove(position)
                val snackbar = Snackbar.make(recyclerView, "Anime dropeado", Snackbar.LENGTH_LONG)
                snackbar.setAction("Deshacer") {
                    error.post { error.visibility = View.GONE }
                    snackbar.safeDismiss()
                    internalMove = true
                    adapter!!.undo(seeingObject, position)
                }
                snackbar.show()
            }
        })
        touchHelper.attachToRecyclerView(recyclerView)
        CacheDB.INSTANCE.seeingDAO().all.observe(this, Observer { list ->
            error.visibility = if (list!!.isEmpty()) View.VISIBLE else View.GONE
            if (!internalMove) {
                adapter!!.update(list as MutableList<SeeingObject>)
                if (isFirst) {
                    isFirst = false
                    recyclerView.scheduleLayoutAnimation()
                }
            } else {
                internalMove = false
            }
        })
    }

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, SeeingActivity::class.java))
        }
    }
}
