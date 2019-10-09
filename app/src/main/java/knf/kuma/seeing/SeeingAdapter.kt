package knf.kuma.seeing

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.pojos.SeeingObject
import org.jetbrains.anko.doAsync
import java.util.*

internal class SeeingAdapter(private val activity: Activity, private val isFullList: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), FastScrollRecyclerView.SectionedAdapter {

    var list: List<SeeingObject> = ArrayList()
    private val seeingDAO = CacheDB.INSTANCE.seeingDAO()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> SeeingItem(LayoutInflater.from(parent.context).inflate(R.layout.item_record_grid, parent, false))
            else -> SeeingItemNormal(LayoutInflater.from(parent.context).inflate(R.layout.item_dir_grid, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty())
            super.onBindViewHolder(holder, position, payloads)
        else if (holder is SeeingItem) {
            holder.chapter.text = getCardText(list[position])
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val seeingObject = list[position]
        if (holder is SeeingItem)
            holder.chapter.text = getCardText(seeingObject)
        (holder as? SeeingItemNormal)?.apply {
            PicassoSingle.get().load(PatternUtil.getCover(seeingObject.aid)).into(imageView)
            title.text = seeingObject.title
            progressView?.visibility = View.GONE
            cardView.setOnClickListener { ActivityAnime.open(activity, seeingObject, imageView) }
            cardView.setOnLongClickListener { view ->
                val popupMenu = PopupMenu(activity, view)
                popupMenu.inflate(R.menu.menu_seeing)
                when (seeingObject.state) {
                    SeeingObject.STATE_WATCHING -> popupMenu.menu.findItem(R.id.watching).isVisible = false
                    SeeingObject.STATE_CONSIDERING -> popupMenu.menu.findItem(R.id.considering).isVisible = false
                    SeeingObject.STATE_COMPLETED -> popupMenu.menu.findItem(R.id.completed).isVisible = false
                    SeeingObject.STATE_DROPPED -> popupMenu.menu.findItem(R.id.droped).isVisible = false
                }
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    doAsync {
                        when (menuItem.itemId) {
                            R.id.watching -> seeingDAO.update(seeingObject.also { it.state = 1 })
                            R.id.considering -> seeingDAO.update(seeingObject.also { it.state = 2 })
                            R.id.completed -> seeingDAO.update(seeingObject.also { it.state = 3 })
                            R.id.droped -> seeingDAO.update(seeingObject.also { it.state = 4 })
                            R.id.paused -> seeingDAO.update(seeingObject.also { it.state = 5 })
                        }
                        syncData { seeing() }
                        if (isFullList)
                            doOnUI {
                                (holder as? SeeingItem)?.chapter?.text = getCardText(seeingObject)
                            }
                    }
                    true
                }
                popupMenu.show()
                true
            }
        }
    }

    override fun getSectionName(position: Int): String {
        return list[position].title.substring(0, 1)
    }

    private fun getCardText(seeingObject: SeeingObject): String {
        return if (isFullList) {
            getStateText(seeingObject.state)
        } else {
            val lastChapter = seeingObject.lastChapter
            val number = lastChapter?.number
            if (number == null)
                "No empezado"
            else if (!lastChapter.number.startsWith("Episodio "))
                "Episodio ${lastChapter.number}"
            else
                lastChapter.number
        }
    }

    private fun getStateText(state: Int): String {
        return when (state) {
            1 -> "Viendo"
            2 -> "Considerando"
            3 -> "Completado"
            4 -> "Dropeado"
            else -> "Pausado"
        }
    }

    override fun getItemViewType(position: Int): Int {
        val seeingObject = list[position]
        return when {
            isFullList || seeingObject.state in 0..1 -> 0
            else -> 1
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update(newList: List<SeeingObject>) {
        if (this.list notSameContent newList) {
            doAsync {
                val result = DiffUtil.calculateDiff(SeeingDiff(list, newList))
                doOnUI {
                    list = newList
                    try {
                        result.dispatchUpdatesTo(this@SeeingAdapter)
                    } catch (e: Exception) {
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }

    internal inner class SeeingItem(itemView: View) : SeeingItemNormal(itemView) {
        val chapter: TextView by itemView.bind(R.id.chapter)
    }

    internal open inner class SeeingItemNormal(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val progressView: ProgressBar? by itemView.optionalBind(R.id.progress)
        val title: TextView by itemView.bind(R.id.title)
    }

    internal inner class SeeingDiff(private val oldList: List<SeeingObject>, private val newList: List<SeeingObject>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].aid == newList[newItemPosition].aid
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            return if (isFullList) {
                getStateText(newList[newItemPosition].state)
            } else {
                newList[newItemPosition].chapter
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return when {
                isFullList -> oldList[oldItemPosition].state == newList[newItemPosition].state
                oldList[oldItemPosition].state == 1 -> oldList[oldItemPosition].chapter == newList[newItemPosition].chapter
                else -> true
            }
        }
    }
}
