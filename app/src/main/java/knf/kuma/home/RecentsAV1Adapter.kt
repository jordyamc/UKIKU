package knf.kuma.home

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.bind
import knf.kuma.commons.doOnUI
import knf.kuma.commons.inflate
import knf.kuma.commons.load
import knf.kuma.commons.optionalBind
import knf.kuma.commons.transform
import knf.kuma.custom.SeenAnimeOverlay
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.RecentAV1
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onLongClick

class RecentsAV1Adapter(val fragment: Fragment, val isMaterial: Boolean, private val isLarge: Boolean = true, private val showSeen: Boolean = true) : UpdateableAdapter<RecentsAV1Adapter.RecentViewHolder>() {

    private var list: List<RecentAV1> = emptyList()

    override fun updateList(list: List<Any>) {
        doAsync {
            this@RecentsAV1Adapter.list = list.transform()
            fragment.doOnUI { notifyDataSetChanged() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder = RecentViewHolder(
        parent.inflate(
            if (isLarge) {
                if (isMaterial) {
                    R.layout.item_fav_grid_card_material
                } else {
                    R.layout.item_fav_grid_card
                }
            } else {
                if (isMaterial) {
                    R.layout.item_fav_grid_card_simple_material
                } else {
                    R.layout.item_fav_grid_card_simple
                }
            })
    )

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        if (list.isEmpty()) return
        val item = list[position]
        holder.img.load(item.animeImageUrl)
        holder.title.text = item.name
        holder.type?.text = item.chapter
        holder.root.onClick {
            ActivityAnime.open(fragment, item, holder.img)
        }
        if (showSeen) {
            holder.seenOverlay.setSeen(item.state.isSeen, false)
            holder.root.onLongClick(returnValue = true) {
                if (item.state.isSeen) {
                    doAsync {
                        CacheDB.INSTANCE.recordAV1DAO().deleteChapter(item.aid, item.number)
                    }
                    item.state.isSeen = false
                    holder.seenOverlay.setSeen(seen = false, animate = true)
                } else {
                    doAsync {
                        CacheDB.INSTANCE.recordAV1DAO().addChapter(item.asRecord())
                    }
                    item.state.isSeen = true
                    holder.seenOverlay.setSeen(seen = true, animate = true)
                }
                syncData { history() }
            }
        }
    }

    class RecentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View by itemView.bind(R.id.card)
        val img: ImageView by itemView.bind(R.id.img)
        val seenOverlay: SeenAnimeOverlay by itemView.bind(R.id.seenOverlay)
        val title: TextView by itemView.bind(R.id.title)
        val type: TextView? by itemView.optionalBind(R.id.type)
    }
}