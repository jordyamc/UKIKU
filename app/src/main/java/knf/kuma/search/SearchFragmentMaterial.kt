package knf.kuma.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.DrawableRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.isFullMode
import knf.kuma.commons.noCrash
import knf.kuma.commons.showSnackbar
import knf.kuma.commons.verifyManager
import knf.kuma.directory.DirectoryAV1PageAdapter
import knf.kuma.pojos.av1.Genre
import knf.kuma.recommended.RankType
import knf.kuma.recommended.RecommendHelper
import knf.kuma.retrofit.Repository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.anko.find

class SearchFragmentMaterial : BottomFragment() {
    lateinit var recyclerView: RecyclerView
    lateinit var fab: ExtendedFloatingActionButton
    lateinit var progressBar: ProgressBar
    private lateinit var errorView: View

    private val model: SearchViewModel by activityViewModels()
    private val searchAdapter: DirectoryAV1PageAdapter by lazy { DirectoryAV1PageAdapter(requireActivity()) }
    private var manager: RecyclerView.LayoutManager? = null
    private var waitingScroll = false

    private var query: String = ""

    private var selected: List<Genre> = emptyList()

    private val fabIcon: Int
        @DrawableRes
        get() {
            return when (selected.size) {
                0 -> R.drawable.ic_genres_0
                1 -> R.drawable.ic_genres_1
                2 -> R.drawable.ic_genres_2
                3 -> R.drawable.ic_genres_3
                4 -> R.drawable.ic_genres_4
                5 -> R.drawable.ic_genres_5
                6 -> R.drawable.ic_genres_6
                7 -> R.drawable.ic_genres_7
                8 -> R.drawable.ic_genres_8
                9 -> R.drawable.ic_genres_9
                else -> R.drawable.ic_genres_more
            }
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        noCrash {
            model.queryListener.observe(viewLifecycleOwner, {
                setSearch(it?.trim() ?: "")
            })
            searchAdapter.addLoadStateListener {
                if (it.append != LoadState.Loading) {
                    progressBar.visibility = View.GONE
                }
                if (it.append.endOfPaginationReached) {
                    errorView.visibility =
                        if (searchAdapter.itemCount == 0) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(
                if (PrefsUtil.layType == "0")
                    R.layout.fragment_search
                else
                    R.layout.fragment_search_grid, container, false)
        recyclerView = view.find(R.id.recycler)
        fab = view.find(R.id.fab)
        progressBar = view.find(R.id.progress)
        errorView = view.find(R.id.error)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            fab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom * 2
            }
            WindowInsetsCompat.CONSUMED
        }
        recyclerView.verifyManager()
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0)
                    fab.shrink()
                else if (dy < 0)
                    fab.extend()
            }
        })
        manager = recyclerView.layoutManager
        lifecycleScope.launch {
            model.pagingDataFlow.collectLatest {
                searchAdapter.submitData(it)
            }
        }
        searchAdapter.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                if (toPosition == 0 && waitingScroll) {
                    manager?.smoothScrollToPosition(recyclerView, null, 0)
                    waitingScroll = false
                }
            }
        })
        recyclerView.adapter = searchAdapter
        fab.setOnClickListener {
            val dialog = GenresDialog()
            dialog.init(genres, selected, object : GenresDialog.MultiChoiceListener {
                override fun onOkay(selected: List<Genre>) {
                    this@SearchFragmentMaterial.selected = selected
                    setFabIcon()
                    setSearchNormal(query)
                }
            })
            dialog.show(childFragmentManager, "genres")
        }
    }

    fun setSearch(q: String) = setSearchNormal(q)

    private fun setSearchNormal(q: String) {
        Log.e("Search", "On search: $q")
        waitingScroll = true
        this.query = q.trim()
        RecommendHelper.registerAll(selected, RankType.SEARCH)
        model.setSearch(q.trim(), selected.map { it.slug })
    }

    private fun setFabIcon() {
        fab.post { fab.setIconResource(fabIcon) }
    }

    override fun onReselect() {

    }

    companion object {

        @JvmOverloads
        operator fun get(query: String = ""): SearchFragmentMaterial {
            val fragment = SearchFragmentMaterial()
            fragment.query = query
            return fragment
        }

        val genres: MutableList<Genre>
            get() = mutableListOf(
                Genre("Acción", "accion"),
                Genre("Antropomórfico", "antropomorfico"),
                Genre("Artes Marciales", "artes-marciales"),
                Genre("Aventura", "aventura"),
                Genre("Carreras", "carreras"),
                Genre("Ciencia Ficción", "ciencia-ficcion"),
                Genre("Comedia", "comedia"),
                Genre("Deportes", "deportes"),
                Genre("Detectives", "detectives"),
                Genre("Drama", "drama"),
                Genre("Ecchi", "ecchi"),
                Genre("Elenco Adulto", "elenco-adulto"),
                Genre("Escolares", "escolares"),
                Genre("Espacial", "espacial"),
                Genre("Fantasía", "fantasia"),
                Genre("Gore", "gore"),
                Genre("Gourmet", "gourmet"),
                Genre("Harem", "harem"),
                Genre("Histórico", "historico"),
                Genre("Idols (Hombre)", "idols-hombre"),
                Genre("Idols (Mujer)", "idols-mujer"),
                Genre("Infantil", "infantil"),
                Genre("Isekai", "isekai"),
                Genre("Josei", "josei"),
                Genre("Juegos Estrategia", "juegos-estrategia"),
                Genre("Mahou Shoujo", "mahou-shoujo"),
                Genre("Mecha", "mecha"),
                Genre("Militar", "militar"),
                Genre("Misterio", "misterio"),
                Genre("Mitología", "mitologia"),
                Genre("Música", "musica"),
                Genre("Parodia", "parodia"),
                Genre("Psicológico", "psicologico"),
                Genre("Recuentos de la Vida", "recuentos-de-la-vida"),
                Genre("Romance", "romance"),
                Genre("Samurai", "samurai"),
                Genre("Seinen", "seinen"),
                Genre("Shoujo", "shoujo"),
                Genre("Shoujo Ai", "shoujo-ai"),
                Genre("Shounen", "shounen"),
                Genre("Shounen Ai", "shounen-ai"),
                Genre("Sobrenatural", "sobrenatural"),
                Genre("Suspenso", "suspenso"),
                Genre("Terror", "terror"),
                Genre("Vampiros", "vampiros"),
            )
    }
}
