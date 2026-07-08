package knf.kuma.home

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.bind
import knf.kuma.commons.doOnUIGlobal
import knf.kuma.commons.inflate
import knf.kuma.commons.load
import knf.kuma.commons.optionalBind
import knf.kuma.commons.transform
import knf.kuma.pojos.av1.DirectoryAV1Min
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick

class RecommendedAdapter(val activity: Activity) : UpdateableAdapter<RecommendedAdapter.RecentViewHolder>() {

    private var list: List<DirectoryAV1Min> = emptyList()

    override fun updateList(list: List<Any>) {
        doAsync {
            this@RecommendedAdapter.list = list.transform()
            doOnUIGlobal { notifyDataSetChanged() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder = RecentViewHolder(parent.inflate(if (DesignUtils.isFlat) R.layout.item_fav_grid_card_material else R.layout.item_fav_grid_card))


    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        val item = list[position]
        holder.img.load(item.imageUrl)
        holder.title.text = item.name
        holder.type?.text = item.type
        holder.root.onClick { ActivityAnime.open(activity, item, holder.img, true, true) }
    }

    class RecentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View by itemView.bind(R.id.card)
        val img: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
        val type: TextView? by itemView.optionalBind(R.id.type)
    }
}