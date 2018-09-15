package knf.kuma.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import androidx.annotation.DrawableRes
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.SearchEvent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.recommended.RankType
import knf.kuma.recommended.RecommendHelper
import java.util.*

class SearchFragment : BottomFragment() {
    @BindView(R.id.recycler)
    lateinit var recyclerView: RecyclerView
    @BindView(R.id.fab)
    lateinit var fab: FloatingActionButton
    @BindView(R.id.progress)
    lateinit var progressBar: ProgressBar
    @BindView(R.id.error)
    lateinit var errorView: View

    private var model: SearchViewModel? = null
    private var adapter: SearchAdapter? = null
    private var manager: LinearLayoutManager? = null

    private var isFirst = true

    private var query: String? = ""

    private var selected: MutableList<String> = ArrayList()

    private val genresString: String
        get() {
            return if (selected.size == 0) {
                ""
            } else {
                RecommendHelper.registerAll(selected, RankType.SEARCH)
                val builder = StringBuilder("%")
                for (genre in selected) {
                    builder.append(genre)
                            .append("%")
                }
                builder.toString()
            }
        }

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

    private val genres: MutableList<String>
        get() = Arrays.asList(
                "Acción",
                "Artes Marciales",
                "Aventuras",
                "Carreras",
                "Comedia",
                "Demencia",
                "Demonios",
                "Deportes",
                "Drama",
                "Ecchi",
                "Escolares",
                "Espacial",
                "Fantasía",
                "Ciencia Ficción",
                "Harem",
                "Historico",
                "Infantil",
                "Josei",
                "Juegos",
                "Magia",
                "Mecha",
                "Militar",
                "Misterio",
                "Musica",
                "Parodia",
                "Policía",
                "Psicológico",
                "Recuentos de la vida",
                "Romance",
                "Samurai",
                "Seinen",
                "Shoujo",
                "Shounen",
                "Sin Generos",
                "Sobrenatural",
                "Superpoderes",
                "Suspenso",
                "Terror",
                "Vampiros",
                "Yaoi",
                "Yuri")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model = ViewModelProviders.of(activity!!).get(SearchViewModel::class.java)
        model!!.setSearch(query!!, "", this, Observer { animeObjects ->
            adapter!!.submitList(animeObjects)
            errorView.visibility = if (animeObjects.size == 0) View.VISIBLE else View.GONE
            if (isFirst) {
                progressBar.visibility = View.GONE
                isFirst = false
                recyclerView.scheduleLayoutAnimation()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        ButterKnife.bind(this, view)
        manager = LinearLayoutManager(context)
        recyclerView.layoutManager = manager
        recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_fall_down)
        adapter = SearchAdapter(this)
        adapter!!.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                if (toPosition == 0)
                    manager!!.smoothScrollToPosition(recyclerView, null, 0)
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == 0)
                    manager!!.smoothScrollToPosition(recyclerView, null, 0)
            }
        })
        recyclerView.adapter = adapter
        fab.setOnClickListener {
            val dialog = GenresDialog()
            dialog.init(genres, selected, object : GenresDialog.MultiChoiceListener {
                override fun onOkay(selected: MutableList<String>) {
                    this@SearchFragment.selected = selected
                    setFabIcon()
                    setSearch(query)
                }
            })
            dialog.show(childFragmentManager, "genres")
        }
        return view
    }

    fun setSearch(q: String?) {
        this.query = q!!.trim { it <= ' ' }
        if (model != null)
            model!!.setSearch(q.trim { it <= ' ' }, genresString, this, Observer { animeObjects ->
                if (animeObjects != null) {
                    adapter!!.submitList(animeObjects)
                    errorView.visibility = if (animeObjects.isEmpty()) View.VISIBLE else View.GONE
                    Answers.getInstance().logSearch(SearchEvent().putQuery(query))
                    if (genresString != "")
                        Answers.getInstance().logSearch(SearchEvent().putQuery(genresString))
                }
                if (isFirst) {
                    progressBar.visibility = View.GONE
                    isFirst = false
                    recyclerView.scheduleLayoutAnimation()
                }
            })
    }

    private fun setFabIcon() {
        fab.post { fab.setImageResource(fabIcon) }
    }

    override fun onReselect() {

    }

    companion object {

        @JvmOverloads
        operator fun get(query: String? = ""): SearchFragment {
            val fragment = SearchFragment()
            fragment.query = query
            return fragment
        }
    }
}
