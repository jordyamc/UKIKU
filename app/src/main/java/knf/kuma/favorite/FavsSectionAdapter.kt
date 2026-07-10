package knf.kuma.favorite

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import knf.kuma.R
import knf.kuma.ads.AdCallback
import knf.kuma.ads.AdCardItemHolder
import knf.kuma.ads.AdsUtilsMob
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.bind
import knf.kuma.commons.load
import knf.kuma.pojos.av1.FavoriteAV1
import knf.kuma.pojos.av1.FavoriteBase
import knf.kuma.pojos.av1.FavoriteSectionAV1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FavsSectionAdapter(private val fragment: Fragment, private val recyclerView: FastScrollRecyclerView, private val showSections: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), FastScrollRecyclerView.SectionedAdapter {

    private val context: Context?
    private val listener: OnMoveListener
    private val orderType = PrefsUtil.favsOrder
    var list: List<FavoriteBase> = ArrayList()

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            if (DesignUtils.isFlat) R.layout.item_fav_material else R.layout.item_fav
        } else {
            if (DesignUtils.isFlat) R.layout.item_fav_grid_material else R.layout.item_fav_grid
        }

    init {
        this.listener = fragment as OnMoveListener
        this.context = fragment.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
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
        if (holder is HeaderHolder && favoriteObject is FavoriteSectionAV1) {
            holder.header.text = favoriteObject.name.let {
                if (it == FavoriteAV1.CATEGORY_NONE) "Sin categoria" else it
            }
            if (favoriteObject.name == FavoriteAV1.CATEGORY_NONE) {
                holder.action.isVisible = false
            } else {
                holder.action.isVisible = true
                holder.action.setOnClickListener { listener.onEdit(favoriteObject.name) }
            }
        } else if (holder is ItemHolder && favoriteObject is FavoriteAV1) {
            holder.imageView.load(favoriteObject.imageUrl())
            holder.title.text = favoriteObject.name
            holder.type.text = favoriteObject.typeText()
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
        return try {
            if (list[position] is FavoriteSectionAV1) TYPE_HEADER else TYPE_ITEM
        } catch (_: Exception) {
            TYPE_ITEM
        }
    }

    override fun getSectionName(position: Int): String {
        return try {
            if (showSections)
                ""
            else {
                val item = list[position] as FavoriteAV1
                when (orderType) {
                    0 -> {
                        val name = item.name
                        if (name.isNotEmpty())
                            name.substring(0, 1).uppercase()
                        else
                            name
                    }

                    else -> item.aid.toString()
                }
            }
        } catch (_: IllegalStateException) {
            ""
        }
    }

    fun updateList(list: List<FavoriteBase>) {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            this@FavsSectionAdapter.list = list
            recyclerView.post { this@FavsSectionAdapter.notifyDataSetChanged() }
        }
    }

    internal interface OnMoveListener {
        fun onSelect(favoriteObject: FavoriteAV1)

        fun onEdit(category: String)
    }

    internal class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
        val type: TextView by itemView.bind(R.id.type)
    }

    internal class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val header: TextView by itemView.bind(R.id.header)
        val action: ImageButton by itemView.bind(R.id.action)
    }

    companion object {
        internal const val TYPE_HEADER = 0
        internal const val TYPE_ITEM = 1
        internal const val TYPE_AD = 2
    }
}
