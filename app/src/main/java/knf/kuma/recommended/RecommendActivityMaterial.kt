package knf.kuma.recommended

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.*
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.pojos.GenreStatusObject
import knf.kuma.recommended.sections.MultipleSectionMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import xdroid.toaster.Toaster

/**
 * Created by jordy on 26/03/2018.
 */

class RecommendActivityMaterial : GenericActivity() {
    val toolbar: Toolbar by bind(R.id.toolbar)
    val recyclerView: RecyclerView by bind(R.id.recycler)
    val error: LinearLayout by bind(R.id.error)
    val loading: LinearLayout by bind(R.id.loading)
    val state: TextView by bind(R.id.state)
    private val dao = CacheDB.INSTANCE.animeDAO()

    private val defaultGridColumns = gridColumns()

    private val layout: Int
        @LayoutRes
        get() = if (isGrid) {
            R.layout.recycler_recommends_material
        } else {
            R.layout.recycler_recommends_grid_material
        }

    private val isGrid: Boolean
        get() = PrefsUtil.layType != "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setSurfaceBars()
        setContentView(layout)
        toolbar.title = "Sugeridos"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        find<FrameLayout>(R.id.adContainer).implBanner(AdsType.RECOMMEND_BANNER, true)
        setAdapter()
    }

    private fun setAdapter() {
        doAsync {
            try {
                val excludeList = LinkedHashSet<String>().apply {
                    addAll(CacheDB.INSTANCE.favsDAO().allAids)
                    addAll(CacheDB.INSTANCE.seeingDAO().allAids)
                }.toList()
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
                    removeFavs(excludeList, abc, ab, ac, bc, a, b, c)
                    if (abc.size > 0)
                        sectionedAdapter.addSection(MultipleSectionMaterial(this@RecommendActivityMaterial, getStringTitle(status[0], status[1], status[2]), getAnimeList(abc), isGrid))
                    if (ab.size > 0)
                        sectionedAdapter.addSection(MultipleSectionMaterial(this@RecommendActivityMaterial, getStringTitle(status[0], status[1]), getAnimeList(ab), isGrid))
                    if (ac.size > 0)
                        sectionedAdapter.addSection(MultipleSectionMaterial(this@RecommendActivityMaterial, getStringTitle(status[0], status[2]), getAnimeList(ac), isGrid))
                    if (bc.size > 0)
                        sectionedAdapter.addSection(MultipleSectionMaterial(this@RecommendActivityMaterial, getStringTitle(status[1], status[2]), getAnimeList(bc), isGrid))
                    if (a.size > 0)
                        sectionedAdapter.addSection(MultipleSectionMaterial(this@RecommendActivityMaterial, getStringTitle(status[0]), getAnimeList(a), isGrid))
                    if (b.size > 0)
                        sectionedAdapter.addSection(MultipleSectionMaterial(this@RecommendActivityMaterial, getStringTitle(status[1]), getAnimeList(b), isGrid))
                    if (c.size > 0)
                        sectionedAdapter.addSection(MultipleSectionMaterial(this@RecommendActivityMaterial, getStringTitle(status[2]), getAnimeList(c), isGrid))
                    val layoutManager: RecyclerView.LayoutManager
                    if (isGrid) {
                        val grid = GridLayoutManager(this@RecommendActivityMaterial, defaultGridColumns)
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
                        layoutManager = LinearLayoutManager(this@RecommendActivityMaterial)
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
                FirebaseCrashlytics.getInstance().recordException(e)
                Toaster.toast("Error al cargar recomendados")
                runOnUiThread {
                    loading.visibility = View.GONE
                }
            }
        }
    }

    @SafeVarargs
    private fun removeFavs(excludeList: List<String>, vararg lists: MutableList<String>) {
        lists.forEach { list ->
            list.removeAll(list.filter { excludeList.contains(it) })
        }
    }

    private fun getList(vararg status: GenreStatusObject): MutableList<String> {
        return dao.getAidsByGenres(getString(*status))
    }

    private fun getAnimeList(list: List<String>): MutableList<AnimeShortObject> {
        val chunk = list.chunked(900)
        val animes = mutableListOf<AnimeShortObject>()
        chunk.forEach {
            animes.addAll(CacheDB.INSTANCE.animeDAO().getAnimesByAids(it))
        }
        return animes
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
        lifecycleScope.launch(Dispatchers.Main) {
            val blacklist = withContext(Dispatchers.IO) { GenreStatusObject.names(CacheDB.INSTANCE.genresDAO().blacklist) }
            val dialog = BlacklistDialog()
            dialog.init(blacklist, object : BlacklistDialog.MultiChoiceListener {
                override fun onOkay(selected: MutableList<String>) {
                    setBlacklist(selected)
                }
            })
            dialog.show(supportFragmentManager, "Blacklist")
        }
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
            R.id.rating -> RankingActivityMaterial.open(this)
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
            context.startActivity(Intent(context, RecommendActivityMaterial::class.java))
        }
    }
}
