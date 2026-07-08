package knf.kuma.home

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.bind
import knf.kuma.commons.doOnUI
import knf.kuma.commons.inflate
import knf.kuma.commons.load
import knf.kuma.commons.noCrash
import knf.kuma.commons.transform
import knf.kuma.pojos.SeeingObject
import knf.kuma.pojos.av1.OrganizerWRecord
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick

class WaitingAdapter(val fragment: Fragment) : UpdateableAdapter<WaitingAdapter.RecentViewHolder>() {

    private var list: List<OrganizerWRecord> = emptyList()

    override fun updateList(list: List<Any>) {
        doAsync {
            this@WaitingAdapter.list = list.transform()
            fragment.doOnUI { notifyDataSetChanged() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder = RecentViewHolder(parent.inflate(if (DesignUtils.isFlat) R.layout.item_fav_grid_card_simple_material else R.layout.item_fav_grid_card_simple))

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        noCrash {
            val item = list[position]
            holder.img.load(item.organizer.imageUrl)
            holder.title.text = item.organizer.name
            holder.root.onClick { ActivityAnime.open(fragment.activity, item.organizer) }
        }
    }

    class RecentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View by itemView.bind(R.id.card)
        val img: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
    }
}