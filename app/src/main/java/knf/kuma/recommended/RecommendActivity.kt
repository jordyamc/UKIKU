package knf.kuma.recommended

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crashlytics.android.Crashlytics
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import knf.kuma.R
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.GenreStatusObject
import knf.kuma.recommended.sections.MultipleSection
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.util.*

/**
 * Created by jordy on 26/03/2018.
 */

class RecommendActivity : AppCompatActivity() {
    val toolbar: Toolbar by bind(R.id.toolbar)
    val recyclerView: RecyclerView by bind(R.id.recycler)
    val error: LinearLayout by bind(R.id.error)
    val loading: LinearLayout by bind(R.id.loading)
    val state: TextView by bind(R.id.state)
    private val dao = CacheDB.INSTANCE.animeDAO()
    private val favsDAO = CacheDB.INSTANCE.favsDAO()
    private val seeingDAO = CacheDB.INSTANCE.seeingDAO()

    private val defaultGridColumns = gridColumns()

    private val layout: Int
        @LayoutRes
        get() = if (isGrid) {
            R.layout.recycler_recommends
        } else {
            R.layout.recycler_recommends_grid
        }

    private val isGrid: Boolean
        get() = PrefsUtil.layType != "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(layout)
        toolbar.title = "Sugeridos"
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowHomeEnabled(false)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        setAdapter()
    }

    private fun setAdapter() {
        doAsync {
            try {
                val status = CacheDB.INSTANCE.genresDAO().top
                setState("Revisando generos")
                if (status.size == 3) {
                    setState("Buscando sugerencias")
                    val sectionedAdapter = SectionedRecyclerViewAdapter()
                    val abc = getList(status[0], status[1], status[2])
                    val ab = getList(status[0], status[1])
                    ab.removeAll(abc)
                    val ac = getList(status[0], status[2])
                    ac.removeAll(abc, ab)
                    val bc = getList(status[1], status[2])
                    bc.removeAll(abc, ab, ac)
                    val a = getList(status[0])
                    a.removeAll(abc, ab, ac, bc)
                    val b = getList(status[1])
                    b.removeAll(abc, ab, ac, bc, a)
                    val c = getList(status[2])
                    c.removeAll(abc, ab, ac, bc, a, b)
                    setState("Filtrando lista")
                    removeFavs(abc, ab, ac, bc, a, b, c)
                    if (abc.size > 0)
                        sectionedAdapter.addSection(MultipleSection(this@RecommendActivity, getStringTitle(status[0], status[1], status[2]), abc, isGrid))
                    if (ab.size > 0)
                        sectionedAdapter.addSection(MultipleSection(this@RecommendActivity, getStringTitle(status[0], status[1]), ab, isGrid))
                    if (ac.size > 0)
                        sectionedAdapter.addSection(MultipleSection(this@RecommendActivity, getStringTitle(status[0], status[2]), ac, isGrid))
                    if (bc.size > 0)
                        sectionedAdapter.addSection(MultipleSection(this@RecommendActivity, getStringTitle(status[1], status[2]), bc, isGrid))
                    if (a.size > 0)
                        sectionedAdapter.addSection(MultipleSection(this@RecommendActivity, getStringTitle(status[0]), a, isGrid))
                    if (b.size > 0)
                        sectionedAdapter.addSection(MultipleSection(this@RecommendActivity, getStringTitle(status[1]), b, isGrid))
                    if (c.size > 0)
                        sectionedAdapter.addSection(MultipleSection(this@RecommendActivity, getStringTitle(status[2]), c, isGrid))
                    val layoutManager: RecyclerView.LayoutManager
                    if (isGrid) {
                        val grid = GridLayoutManager(this@RecommendActivity, defaultGridColumns)
                        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                            override fun getSpanSize(position: Int): Int {
                                return try {
                                    when (sectionedAdapter.getSectionItemViewType(position)) {
                                        SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER -> defaultGridColumns
                                        else -> 1
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    defaultGridColumns
                                }

                            }
                        }
                        layoutManager = grid
                    } else {
                        layoutManager = LinearLayoutManager(this@RecommendActivity)
                    }
                    runOnUiThread {
                        loading.visibility = View.GONE
                        recyclerView.layoutManager = layoutManager
                        recyclerView.adapter = sectionedAdapter
                    }
                } else
                    runOnUiThread {
                        loading.visibility = View.GONE
                        error.visibility = View.VISIBLE
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                Crashlytics.logException(e)
                Toaster.toast("Error al cargar recomendados")
                runOnUiThread {
                    loading.visibility = View.GONE
                }
            }
        }
    }

    @SafeVarargs
    private fun removeFavs(vararg lists: MutableList<AnimeObject>) {
        for (list in lists) {
            val removeList = ArrayList<AnimeObject>()
            for (animeObject in list)
                if (favsDAO.isFav(animeObject.key) || seeingDAO.isSeeingAll(animeObject.aid!!))
                    removeList.add(animeObject)
            list.removeAll(removeList)
        }
    }

    private fun getList(vararg status: GenreStatusObject): MutableList<AnimeObject> {
        val objects = dao.getByGenres(getString(*status))
        objects.sorted()
        return objects
    }

    private fun getString(vararg status: GenreStatusObject): String {
        val builder = StringBuilder("%")
        for (s in status) {
            builder.append(s.name)
                    .append("%")
        }
        return builder.toString()
    }

    private fun getStringTitle(vararg status: GenreStatusObject): String {
        val builder = StringBuilder()
        for (s in status) {
            builder.append(s.name)
                    .append(", ")
        }
        return builder.toString().substring(0, builder.length - 2)
    }

    private fun setState(stateString: String) {
        runOnUiThread {
            state.text = stateString
        }
    }

    private fun showBlacklist() {
        val blacklist = GenreStatusObject.getNames(CacheDB.INSTANCE.genresDAO().blacklist)
        val dialog = BlacklistDialog()
        dialog.init(blacklist, object : BlacklistDialog.MultiChoiceListener {
            override fun onOkay(selected: MutableList<String>) {
                setBlacklist(selected)
            }
        })
        dialog.show(supportFragmentManager, "Blacklist")
    }

    private fun setBlacklist(selected: MutableList<String>) {
        doAsync {
            for (s in selected)
                RecommendHelper.block(s)
            for (statusObject in CacheDB.INSTANCE.genresDAO().all)
                if (statusObject.isBlocked && !selected.contains(statusObject.name))
                    RecommendHelper.reset(statusObject.name)
            resetSuggestions()
        }
    }

    private fun resetSuggestions() {
        setState("Iniciando búsqueda")
        runOnUiThread {
            val adapter = recyclerView.adapter as SectionedRecyclerViewAdapter?
            if (adapter != null) {
                adapter.removeAllSections()
                recyclerView.adapter = adapter
                loading.visibility = View.VISIBLE
                error.visibility = View.GONE
                setAdapter()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_suggestions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.blacklist -> showBlacklist()
            R.id.rating -> RankingActivity.open(this)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 4321)
            resetSuggestions()
    }

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, RecommendActivity::class.java))
        }
    }
}
