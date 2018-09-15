package knf.kuma.tv.details

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.Observer
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import knf.kuma.R
import knf.kuma.commons.PatternUtil
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.FavoriteObject
import knf.kuma.retrofit.Repository
import knf.kuma.tv.TVServersFactory
import knf.kuma.tv.anime.ChapterPresenter
import knf.kuma.tv.anime.RelatedPresenter
import java.util.*

class TVAnimesDetailsFragment : DetailsSupportFragment(), OnItemViewClickedListener, OnActionClickedListener {

    private var mRowsAdapter: ArrayObjectAdapter? = null
    private var favoriteObject: FavoriteObject? = null
    private var currentChapter: AnimeObject.WebInfo.AnimeChapter? = null
    private var chapters: MutableList<AnimeObject.WebInfo.AnimeChapter> = ArrayList()
    private var actionAdapter: SparseArrayObjectAdapter? = null
    private var listRowAdapter: ArrayObjectAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildDetails()
        onItemViewClickedListener = this
    }

    private fun getLastSeen(chapters: MutableList<AnimeObject.WebInfo.AnimeChapter>): Int {
        if (chapters.isNotEmpty()) {
            val chapter = CacheDB.INSTANCE.chaptersDAO().getLast(PatternUtil.getEids(chapters))
            if (chapter != null) {
                val position = chapters.indexOf(chapter)
                if (position >= 0)
                    return position
            }
        }
        return 0
    }

    private fun buildDetails() {
        Repository().getAnime(context!!, arguments!!.getString("url")!!, true).observe(activity!!, Observer { animeObject ->
            Glide.with(activity!!).asBitmap().load(PatternUtil.getCover(animeObject.aid!!)).into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Palette.from(resource).generate { palette ->
                        val swatch = palette!!.darkMutedSwatch
                        favoriteObject = FavoriteObject(animeObject)
                        chapters = animeObject.chapters!!
                        chapters.reversed()
                        val selector = ClassPresenterSelector()
                        val rowPresenter = CustomFullWidthDetailsOverviewRowPresenter(
                                if (swatch == null)
                                    DetailsDescriptionPresenter()
                                else
                                    DetailsDescriptionPresenter(swatch.titleTextColor, swatch.bodyTextColor))
                        if (swatch != null) {
                            rowPresenter.backgroundColor = swatch.rgb
                            val hsv = FloatArray(3)
                            val color = swatch.rgb
                            Color.colorToHSV(color, hsv)
                            hsv[2] *= 0.8f
                            rowPresenter.actionsBackgroundColor = Color.HSVToColor(hsv)
                        }
                        selector.addClassPresenter(DetailsOverviewRow::class.java, rowPresenter)
                        selector.addClassPresenter(ChaptersListRow::class.java, ChaptersListPresenter(getLastSeen(chapters)))
                        selector.addClassPresenter(ListRow::class.java, ListRowPresenter())
                        mRowsAdapter = ArrayObjectAdapter(selector)
                        val detailsOverview = DetailsOverviewRow(animeObject)

                        // Add images and action buttons to the details view
                        detailsOverview.setImageBitmap(context!!, resource)
                        detailsOverview.isImageScaleUpAllowed = true
                        actionAdapter = SparseArrayObjectAdapter()
                        if (CacheDB.INSTANCE.favsDAO().isFav(animeObject.key)) {
                            actionAdapter!!.set(1, Action(1, "Quitar favorito", null, ContextCompat.getDrawable(context!!, R.drawable.heart_full)))
                        } else {
                            actionAdapter!!.set(1, Action(1, "Añadir favorito", null, ContextCompat.getDrawable(context!!, R.drawable.heart_empty)))
                        }
                        detailsOverview.actionsAdapter = actionAdapter
                        rowPresenter.onActionClickedListener = this@TVAnimesDetailsFragment
                        mRowsAdapter!!.add(detailsOverview)

                        // Add a Chapters items row
                        if (chapters.isNotEmpty()) {
                            listRowAdapter = ArrayObjectAdapter(
                                    ChapterPresenter())
                            for (chapter in chapters)
                                listRowAdapter!!.add(chapter)
                            val header = HeaderItem(0, "Episodios")
                            mRowsAdapter!!.add(ChaptersListRow(header, listRowAdapter as ArrayObjectAdapter))
                        }

                        // Add a Related items row
                        if (animeObject.related!!.isNotEmpty()) {
                            val listRowAdapter = ArrayObjectAdapter(
                                    RelatedPresenter())
                            for (related in animeObject.related!!)
                                listRowAdapter.add(related)
                            val header = HeaderItem(0, "Relacionados")
                            mRowsAdapter!!.add(ListRow(header, listRowAdapter))
                        }

                        adapter = mRowsAdapter!!
                    }
                }
            })
        })
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any, rowViewHolder: RowPresenter.ViewHolder, row: Row) {
        if (item is AnimeObject.WebInfo.AnimeRelated) {
            TVAnimesDetails.start(context!!, "https://animeflv.net" + item.link!!)
        } else if (item is AnimeObject.WebInfo.AnimeChapter) {
            currentChapter = item
            TVServersFactory.start(activity as Activity, item.link, item, itemViewHolder, activity as TVServersFactory.ServersInterface)
        }
    }

    fun onStartStreaming() {
        if (currentChapter != null)
            listRowAdapter!!.notifyArrayItemRangeChanged(chapters.indexOf(currentChapter as AnimeObject.WebInfo.AnimeChapter), 1)
    }

    override fun onActionClicked(action: Action) {
        actionAdapter!!.clear()
        if (CacheDB.INSTANCE.favsDAO().isFav(favoriteObject!!.key)) {
            CacheDB.INSTANCE.favsDAO().deleteFav(favoriteObject as FavoriteObject)
            action.label1 = "Añadir favorito"
            action.icon = ContextCompat.getDrawable(context!!, R.drawable.heart_empty)
        } else {
            CacheDB.INSTANCE.favsDAO().addFav(favoriteObject as FavoriteObject)
            action.label1 = "Quitar favorito"
            action.icon = ContextCompat.getDrawable(context!!, R.drawable.heart_full)
        }
        actionAdapter!!.set(1, action)
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
