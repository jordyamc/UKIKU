package knf.kuma.home

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnimeMaterial
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.bind
import knf.kuma.commons.doOnUI
import knf.kuma.commons.inflate
import knf.kuma.commons.load
import knf.kuma.commons.optionalBind
import knf.kuma.commons.transform
import knf.kuma.pojos.av1.DirectoryAV1Min
import knf.kuma.pojos.av1.Recommended
import knf.kuma.search.SearchAdvObject
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick

class SearchAdapterMaterial(val fragment: Fragment) : UpdateableAdapter<SearchAdapterMaterial.RecentViewHolder>() {

    private var list: List<Recommended> = emptyList()

    override fun updateList(list: List<Any>) {
        doAsync {
            this@SearchAdapterMaterial.list = list.transform()
            fragment.doOnUI { notifyDataSetChanged() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder = RecentViewHolder(parent.inflate(if (DesignUtils.isFlat) R.layout.item_fav_grid_card_material else R.layout.item_fav_grid_card))


    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        val item = list[position]
        holder.img.load(item.imageUrl)
        holder.title.text = item.name
        holder.type?.text = item.typeText
        holder.root.onClick { ActivityAnimeMaterial.open(fragment, item, holder.img, true, true) }
    }

    class RecentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View by itemView.bind(R.id.card)
        val img: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
        val type: TextView? by itemView.optionalBind(R.id.type)
    }
}