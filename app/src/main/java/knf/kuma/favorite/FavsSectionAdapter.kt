package knf.kuma.favorite

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import knf.kuma.R
import knf.kuma.ads.*
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PicassoSingle
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.bind
import knf.kuma.favorite.objects.InfoContainer
import knf.kuma.pojos.FavoriteObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FavsSectionAdapter(private val fragment: Fragment, private val recyclerView: FastScrollRecyclerView, private val showSections: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), FastScrollRecyclerView.SectionedAdapter {

    private val context: Context?
    private val listener: OnMoveListener
    private val orderType = PrefsUtil.favsOrder
    private var list: MutableList<FavoriteObject> = ArrayList()

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            R.layout.item_fav
        } else {
            R.layout.item_fav_grid
        }

    init {
        this.listener = fragment as OnMoveListener
        this.context = fragment.context
        if (showSections)
            recyclerView.setFastScrollEnabled(false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_fav_header, parent, false))
            TYPE_ITEM -> ItemHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
            TYPE_AD -> AdCardItemHolder(parent, AdCardItemHolder.TYPE_FAV).also {
                it.loadAd(fragment.lifecycleScope, object : AdCallback {
                    override fun getID(): String = AdsUtilsMob.FAVORITE_BANNER
                }, 500)
            }
            else -> HeaderHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_fav_header, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val favoriteObject = list[position]
        if (holder is HeaderHolder) {
            holder.header.text = favoriteObject.name
            holder.action.setOnClickListener { listener.onEdit(favoriteObject.name ?: "") }
        } else if (holder is ItemHolder) {
            PicassoSingle.get().load(PatternUtil.getCover(favoriteObject.aid
                    ?: "")).into(holder.imageView)
            holder.title.text = favoriteObject.name
            holder.type.text = favoriteObject.type
            holder.cardView.setOnClickListener { ActivityAnime.open(fragment, favoriteObject, holder.imageView) }
            if (showSections)
                holder.cardView.setOnLongClickListener {
                    listener.onSelect(favoriteObject)
                    true
                }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (list[position] is AdFavoriteObject) TYPE_AD else if (list[position].isSection) TYPE_HEADER else TYPE_ITEM
    }

    override fun getSectionName(position: Int): String {
        return try {
            when (orderType) {
                0 -> {
                    val name = list[position].name
                    if (name.isNotEmpty())
                        name.substring(0, 1).toUpperCase()
                    else
                        name
                }
                else -> list[position].aid
            }
        } catch (e: IllegalStateException) {
            ""
        }
    }

    fun updatePosition(container: InfoContainer) {
        list = container.updated ?: arrayListOf()
        recyclerView.post { notifyItemMoved(container.from, container.to) }
    }

    fun updateList(list: MutableList<FavoriteObject>) {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            this@FavsSectionAdapter.list = list
            if (PrefsUtil.layType == "0")
                this@FavsSectionAdapter.list.implAdsFavorite()
            recyclerView.post { this@FavsSectionAdapter.notifyDataSetChanged() }
        }
    }

    internal interface OnMoveListener {
        fun onSelect(favoriteObject: FavoriteObject)

        fun onEdit(category: String)
    }

    internal inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
        val type: TextView by itemView.bind(R.id.type)
    }

    internal inner class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val header: TextView by itemView.bind(R.id.header)
        val action: ImageButton by itemView.bind(R.id.action)
    }

    companion object {
        internal const val TYPE_HEADER = 0
        internal const val TYPE_ITEM = 1
        internal const val TYPE_AD = 2
    }
}
