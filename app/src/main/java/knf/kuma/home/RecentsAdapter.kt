package knf.kuma.home

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.*
import knf.kuma.custom.SeenAnimeOverlay
import knf.kuma.database.CacheDB
import knf.kuma.pojos.RecentObject
import knf.kuma.pojos.SeenObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onLongClick

class RecentsAdapter(val fragment: HomeFragment, private val isLarge: Boolean = true, private val showSeen: Boolean = true) : UpdateableAdapter<RecentsAdapter.RecentViewHolder>() {

    private var list: List<RecentObject> = emptyList()

    override fun updateList(list: List<Any>) {
        doAsync {
            this@RecentsAdapter.list = list.transform()
            doOnUI { notifyDataSetChanged() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder = RecentViewHolder(parent.inflate(if (isLarge) R.layout.item_fav_grid_card else R.layout.item_fav_grid_card_simple))


    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        val item = list[position]
        holder.img.load(item.img)
        holder.title.text = item.name
        holder.type?.text = item.chapter
        holder.root.onClick {
            if (item.animeObject != null) {
                ActivityAnime.open(fragment, item.animeObject, holder.img)
            } else {
                fragment.lifecycleScope.launch(Dispatchers.Main) {
                    val animeObject = withContext(Dispatchers.IO) { CacheDB.INSTANCE.animeDAO().getByAid(item.aid) }
                    if (animeObject != null) {
                        ActivityAnime.open(fragment, animeObject, holder.img)
                    } else {
                        ActivityAnime.open(fragment, item, holder.img)
                    }
                }
            }
        }
        if (showSeen) {
            holder.seenOverlay.setSeen(item.isSeen, false)
            holder.root.onLongClick(returnValue = true) {
                if (item.isSeen) {
                    doAsync {
                        CacheDB.INSTANCE.seenDAO().deleteChapter(item.aid, item.chapter)
                    }
                    item.isSeen = false
                    holder.seenOverlay.setSeen(seen = false, animate = true)
                } else {
                    doAsync {
                        CacheDB.INSTANCE.seenDAO().addChapter(SeenObject.fromRecent(item))
                    }
                    item.isSeen = true
                    holder.seenOverlay.setSeen(seen = true, animate = true)
                }
                syncData { seen() }
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