package knf.kuma.tv.details

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import knf.kuma.App
import knf.kuma.R
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.noCrash
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.FavoriteObject
import knf.kuma.retrofit.Repository
import knf.kuma.tv.TVServersFactory
import knf.kuma.tv.anime.ChapterPresenter
import knf.kuma.tv.anime.RelatedPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts

@ExperimentalCoroutinesApi
@ExperimentalContracts
class TVAnimesDetailsFragment : DetailsSupportFragment(), OnItemViewClickedListener, OnActionClickedListener {

    private var mRowsAdapter: ArrayObjectAdapter? = null
    private var favoriteObject: FavoriteObject? = null
    private var currentChapter: AnimeObject.WebInfo.AnimeChapter? = null
    private var chapters: MutableList<AnimeObject.WebInfo.AnimeChapter>? = ArrayList()
    private var actionAdapter: SparseArrayObjectAdapter? = null
    private var listRowAdapter: ArrayObjectAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildDetails()
        onItemViewClickedListener = this
    }

    private suspend fun getLastSeen(chapters: MutableList<AnimeObject.WebInfo.AnimeChapter>?): Int {
        if (chapters?.isNotEmpty() == true) {
            val eids =
                chapters.sortedBy { it.number.substringAfterLast(" ").toFloat() }.map { it.eid }
            eids.chunked(50).forEach { list ->
                val chapter =
                    withContext(Dispatchers.IO) { CacheDB.INSTANCE.seenDAO().getLast(list) }
                if (chapter != null) {
                    val position = chapters.indexOf(chapters.find { it.eid == chapter.eid })
                    if (position >= 0)
                        return position
                }
            }
        }
        return 0
    }

    private fun buildDetails() {
        val activity = activity ?: return
        Repository().getAnime(
            App.context, arguments?.getString("url")
                ?: "", true
        ).observe(activity) { animeObject ->
            if (animeObject != null) {
                Glide.with(App.context).asBitmap().load(PatternUtil.getCoverGlide(animeObject.aid))
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            Palette.from(resource).generate { palette ->
                                val swatch = palette?.darkMutedSwatch
                                favoriteObject = FavoriteObject(animeObject)
                                chapters = animeObject.chapters
                                chapters?.reversed()
                                val selector = ClassPresenterSelector()
                                val rowPresenter = CustomFullWidthDetailsOverviewRowPresenter(
                                    if (swatch == null)
                                        DetailsDescriptionPresenter()
                                    else
                                        DetailsDescriptionPresenter(
                                            swatch.titleTextColor,
                                            swatch.bodyTextColor
                                        )
                                )
                                if (swatch != null) {
                                    rowPresenter.backgroundColor = swatch.rgb
                                    val hsv = FloatArray(3)
                                    val color = swatch.rgb
                                    Color.colorToHSV(color, hsv)
                                    hsv[2] *= 0.8f
                                    rowPresenter.actionsBackgroundColor = Color.HSVToColor(hsv)
                                }
                                selector.addClassPresenter(
                                    DetailsOverviewRow::class.java,
                                    rowPresenter
                                )
                                lifecycleScope.launch {
                                    selector.addClassPresenter(
                                        ChaptersListRow::class.java,
                                        ChaptersListPresenter(getLastSeen(chapters))
                                    )
                                    selector.addClassPresenter(
                                        ListRow::class.java,
                                        ListRowPresenter()
                                    )
                                    mRowsAdapter = ArrayObjectAdapter(selector)
                                    val detailsOverview = DetailsOverviewRow(animeObject)

                                    // Add images and action buttons to the details view
                                    detailsOverview.setImageBitmap(activity, resource)
                                    detailsOverview.isImageScaleUpAllowed = true
                                    actionAdapter = SparseArrayObjectAdapter()
                                    if (withContext(Dispatchers.IO) {
                                            CacheDB.INSTANCE.favsDAO().isFav(animeObject.key)
                                        }) {
                                        actionAdapter?.set(
                                            1,
                                            Action(
                                                1,
                                                "Quitar favorito",
                                                null,
                                                ContextCompat.getDrawable(
                                                    App.context,
                                                    R.drawable.heart_full
                                                )
                                            )
                                        )
                                    } else {
                                        actionAdapter?.set(
                                            1,
                                            Action(
                                                1,
                                                "Añadir favorito",
                                                null,
                                                ContextCompat.getDrawable(
                                                    App.context,
                                                    R.drawable.heart_empty
                                                )
                                            )
                                        )
                                    }
                                    actionAdapter?.set(
                                        2,
                                        Action(
                                            2,
                                            "${animeObject.rate_stars}/5.0 (${animeObject.rate_count})",
                                            null,
                                            ContextCompat.getDrawable(
                                                App.context,
                                                R.drawable.ic_seeing
                                            )
                                        )
                                    )
                                    detailsOverview.actionsAdapter = actionAdapter
                                    rowPresenter.onActionClickedListener =
                                        this@TVAnimesDetailsFragment
                                    mRowsAdapter?.add(detailsOverview)
                                    // Add a Chapters items row
                                    if (chapters?.isNotEmpty() == true) {
                                        chapters?.let {
                                            listRowAdapter = ArrayObjectAdapter(
                                                ChapterPresenter()
                                            )
                                            for (chapter in it)
                                                listRowAdapter?.add(chapter)
                                            val header = HeaderItem(0, "Episodios")
                                            mRowsAdapter?.add(
                                                ChaptersListRow(
                                                    header, listRowAdapter
                                                        ?: ArrayObjectAdapter()
                                                )
                                            )
                                        }
                                    }

                                    // Add a Related items row
                                    if (animeObject.related?.isNotEmpty() == true) {
                                        val listRowAdapter = ArrayObjectAdapter(
                                            RelatedPresenter()
                                        )
                                        for (related in animeObject.related ?: listOf())
                                            listRowAdapter.add(related)
                                        val header = HeaderItem(0, "Relacionados")
                                        mRowsAdapter?.add(ListRow(header, listRowAdapter))
                                    }

                                    noCrash { adapter = mRowsAdapter }
                                }
                            }
                        }
                    })
            }
        }
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any, rowViewHolder: RowPresenter.ViewHolder, row: Row) {
        val activity = activity ?: return
        if (item is AnimeObject.WebInfo.AnimeRelated) {
            TVAnimesDetails.start(activity, "https://animeflv.net" + item.link)
        } else if (item is AnimeObject.WebInfo.AnimeChapter) {
            currentChapter = item
            TVServersFactory.start(activity, item.link, item, itemViewHolder, activity as? TVServersFactory.ServersInterface)
        }
    }

    fun onStartStreaming() {
        currentChapter?.let { listRowAdapter?.notifyArrayItemRangeChanged(chapters?.indexOf(it)?:0, 1) }
    }

    override fun onActionClicked(action: Action) {
        if (action.id == 1L) {
            actionAdapter?.clear()
            favoriteObject?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (CacheDB.INSTANCE.favsDAO().isFav(it.key)) {
                        CacheDB.INSTANCE.favsDAO().deleteFav(it)
                        launch(Dispatchers.Main){
                            action.label1 = "Añadir favorito"
                            action.icon = ContextCompat.getDrawable(App.context, R.drawable.heart_empty)
                        }
                    } else {
                        CacheDB.INSTANCE.favsDAO().addFav(it)
                        launch(Dispatchers.Main){
                            action.label1 = "Quitar favorito"
                            action.icon = ContextCompat.getDrawable(App.context, R.drawable.heart_full)
                        }
                    }
                    syncData { favs() }
                }
            }
            actionAdapter?.set(1, action)
        }
    }

    companion object {

        operator fun get(url: String): TVAnimesDetailsFragment {
            val fragment = TVAnimesDetailsFragment()
            val bundle = Bundle()
            bundle.putString("url", url)
            fragment.arguments = bundle
            return fragment
        }
    }
}
