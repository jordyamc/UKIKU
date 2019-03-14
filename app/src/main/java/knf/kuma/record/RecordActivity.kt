package knf.kuma.record

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import org.jetbrains.anko.doAsync

class RecordActivity : AppCompatActivity() {
    val toolbar: Toolbar by bind(R.id.toolbar)
    val recyclerView: RecyclerView by bind(R.id.recycler)
    val progressBar: ProgressBar by bind(R.id.progress)
    val error: View by bind(R.id.error)
    private var adapter: RecordsAdapter? = null
    private var isFirst = true

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            R.layout.recycler_records
        } else {
            R.layout.recycler_records_grid
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(layout)
        toolbar.title = "Historial"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        adapter = RecordsAdapter(this)
        recyclerView.verifyManager()
        recyclerView.adapter = adapter
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.RIGHT, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                adapter?.remove(viewHolder.adapterPosition)
                recyclerView.showSnackbar("Elemento eliminado")
            }
        })
        touchHelper.attachToRecyclerView(recyclerView)
        CacheDB.INSTANCE.recordsDAO().allLive.observe(this, Observer { recordObjects ->
            adapter?.update(recordObjects)
            if (isFirst) {
                isFirst = false
                recyclerView.scheduleLayoutAnimation()
            }
            if (recordObjects.isEmpty())
                error.visibility = View.VISIBLE
            else
                error.visibility = View.GONE
            progressBar.visibility = View.GONE
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_records, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_clear ->
                MaterialDialog(this@RecordActivity).safeShow {
                    message(text = "¿Limpiar el historial?")
                    positiveButton(text = "Continuar") { CacheDB.INSTANCE.recordsDAO().clear() }
                    negativeButton(text = "cancelar")
                }
            R.id.action_status -> doAsync {
                val count = CacheDB.INSTANCE.chaptersDAO().count
                doOnUI { "$count episodios vistos".toast() }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, RecordActivity::class.java))
            AchievementManager.onRecordsOpened()
        }
    }
}
