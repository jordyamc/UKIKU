package knf.kuma.tv.details

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DetailsOverviewRow
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnActionClickedListener
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.leanback.widget.SparseArrayObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import knf.kuma.App
import knf.kuma.R
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.noCrash
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.ChapterWID
import knf.kuma.pojos.av1.FavoriteAV1
import knf.kuma.pojos.av1.Relation
import knf.kuma.tv.TVRepository
import knf.kuma.tv.TVServersFactory
import knf.kuma.tv.anime.ChapterPresenter
import knf.kuma.tv.anime.RelatedPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TVAnimesDetailsFragment : DetailsSupportFragment(), OnItemViewClickedListener, OnActionClickedListener {

    private var mRowsAdapter: ArrayObjectAdapter? = null
    private var favoriteObject: FavoriteAV1? = null
    private var currentChapter: ChapterWID? = null
    private var chapters: List<ChapterWID>? = ArrayList()
    private val actionAdapter by lazy { SparseArrayObjectAdapter() }
    private var listRowAdapter: ArrayObjectAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onItemViewClickedListener = this
        buildDetails()
    }

    private suspend fun getLastSeen(aid: Int): Int {
        if (chapters?.isNotEmpty() == true) {
            val last = withContext(Dispatchers.IO) { CacheDB.INSTANCE.recordAV1DAO().getLastByAid(aid) }
            if (last != null) {
                val position = chapters?.indexOf(chapters?.find { it.eid == last.eid })
                if (position != null && position >= 0)
                    return position
            }
        }
        return 0
    }

    private fun buildDetails() {
        val activity = activity ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val animeObject = TVRepository.getAnime(arguments?.getString("url") ?: "")
            if (animeObject != null) {
                Glide.with(App.context).asBitmap().load(animeObject.imageUrl)
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            Palette.from(resource).generate { palette ->
                                val swatch = palette?.darkMutedSwatch
                                favoriteObject = animeObject.asFavorite()
                                chapters = animeObject.chapters.sortedBy { it.number }.map { it.withID(animeObject) }
                                val selector = ClassPresenterSelector()
                                val rowPresenter = FullWidthDetailsOverviewRowPresenter(
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
                                viewLifecycleOwner.lifecycleScope.launch {
                                    selector.addClassPresenter(
                                        ChaptersListRow::class.java,
                                        ChaptersListPresenter(getLastSeen(animeObject.aid))
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
                                    if (withContext(Dispatchers.IO) { CacheDB.INSTANCE.favoriteAV1DAO().isFav(animeObject.aid) }) {
                                        actionAdapter.set(
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
                                        actionAdapter.set(
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
                                    actionAdapter.apply {
                                        set(
                                            2,
                                            Action(
                                                2,
                                                "${String.format("%.1f", animeObject.rateStars)}/5.0 (${animeObject.rateCount})",
                                                null,
                                                ContextCompat.getDrawable(
                                                    App.context,
                                                    R.drawable.ic_seeing
                                                )
                                            )
                                        )
                                        detailsOverview.actionsAdapter = this
                                    }
                                    rowPresenter.onActionClickedListener =
                                        this@TVAnimesDetailsFragment
                                    mRowsAdapter?.add(detailsOverview)
                                    // Add a Chapters items row
                                    if (chapters?.isNotEmpty() == true) {
                                        chapters?.let {
                                            listRowAdapter = ArrayObjectAdapter(
                                                ChapterPresenter(viewLifecycleOwner.lifecycleScope)
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
                                    if (animeObject.relations.isNotEmpty()) {
                                        val listRowAdapter = ArrayObjectAdapter(
                                            RelatedPresenter()
                                        )
                                        for (related in animeObject.relations)
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
        if (item is Relation) {
            TVAnimesDetails.start(activity, item.animeUrl)
        } else if (item is ChapterWID) {
            currentChapter = item
            TVServersFactory.start(activity, item.link, item.name, item.episodeName, item.asRecord() , itemViewHolder, activity as? TVServersFactory.ServersInterface)
        }
    }

    fun onStartStreaming() {
        currentChapter?.let { listRowAdapter?.notifyArrayItemRangeChanged(chapters?.indexOf(it)?:0, 1) }
    }

    override fun onActionClicked(action: Action) {
        if (action.id == 1L) {
            favoriteObject?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    val updatedAction = Action(1, "Añadir favorito", null, null)
                    if (CacheDB.INSTANCE.favoriteAV1DAO().isFav(it.aid)) {
                        CacheDB.INSTANCE.favoriteAV1DAO().delete(it)
                        updatedAction.label1 = "Añadir favorito"
                        updatedAction.icon = ContextCompat.getDrawable(App.context, R.drawable.heart_empty)
                    } else {
                        CacheDB.INSTANCE.favoriteAV1DAO().addFav(it)
                        updatedAction.label1 = "Quitar favorito"
                        updatedAction.icon = ContextCompat.getDrawable(App.context, R.drawable.heart_full)
                    }
                    syncData { favs() }
                    withContext(Dispatchers.Main) {
                        actionAdapter.set(1, updatedAction)
                    }
                }
            }
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
