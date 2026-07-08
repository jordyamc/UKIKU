package knf.kuma.seeing

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.inmobi.media.De
import com.inmobi.media.se
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.bind
import knf.kuma.commons.doOnUIGlobal
import knf.kuma.commons.load
import knf.kuma.commons.optionalBind
import knf.kuma.commons.roundedString
import knf.kuma.database.CacheDB
import knf.kuma.pojos.SeeingObject
import knf.kuma.pojos.av1.Organizer
import knf.kuma.pojos.av1.OrganizerWRecord
import org.jetbrains.anko.doAsync

internal class SeeingAdapter(private val activity: Activity, private val isFullList: Boolean) : PagingDataAdapter<OrganizerWRecord, RecyclerView.ViewHolder>(
    OrganizerWRecord.DIFF
), FastScrollRecyclerView.SectionedAdapter {

    private val seeingDAO = CacheDB.INSTANCE.organizerDAO()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> SeeingItem(LayoutInflater.from(parent.context).inflate(if (DesignUtils.isFlat) R.layout.item_record_grid_material else R.layout.item_record_grid, parent, false))
            else -> SeeingItemNormal(LayoutInflater.from(parent.context).inflate(if (DesignUtils.isFlat) R.layout.item_dir_grid_material else R.layout.item_dir_grid, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)
        if (payloads.isEmpty() || item == null)
            super.onBindViewHolder(holder, position, payloads)
        else if (holder is SeeingItem) {
            holder.chapter.text = getCardText(item)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val seeingObject = getItem(position) ?: return
        if (holder is SeeingItem)
            holder.chapter.text = getCardText(seeingObject)
        (holder as? SeeingItemNormal)?.apply {
            imageView.load(seeingObject.organizer.imageUrl)
            title.text = seeingObject.organizer.name
            progressView?.visibility = View.GONE
            cardView.setOnClickListener { ActivityAnime.open(activity, seeingObject.organizer) }
            cardView.setOnLongClickListener { view ->
                val popupMenu = PopupMenu(activity, view)
                popupMenu.inflate(R.menu.menu_seeing)
                when (seeingObject.organizer.state) {
                    SeeingObject.STATE_WATCHING -> popupMenu.menu.findItem(R.id.watching).isVisible = false
                    SeeingObject.STATE_CONSIDERING -> popupMenu.menu.findItem(R.id.considering).isVisible = false
                    SeeingObject.STATE_COMPLETED -> popupMenu.menu.findItem(R.id.completed).isVisible = false
                    SeeingObject.STATE_DROPPED -> popupMenu.menu.findItem(R.id.droped).isVisible = false
                }
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    doAsync {
                        when (menuItem.itemId) {
                            R.id.watching -> seeingDAO.update(seeingObject.organizer.also { it.state = 1 })
                            R.id.considering -> seeingDAO.update(seeingObject.organizer.also { it.state = 2 })
                            R.id.completed -> seeingDAO.update(seeingObject.organizer.also { it.state = 3 })
                            R.id.droped -> seeingDAO.update(seeingObject.organizer.also { it.state = 4 })
                            R.id.paused -> seeingDAO.update(seeingObject.organizer.also { it.state = 5 })
                        }
                        syncData { seeing() }
                        if (isFullList)
                            doOnUIGlobal {
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
        return getItem(position)?.organizer?.name?.substring(0, 1) ?: ""
    }

    private fun getCardText(seeingObject: OrganizerWRecord): String {
        return if (isFullList) {
            getStateText(seeingObject.organizer.state)
        } else {
            val lastChapter = seeingObject.lastChapter
            val number = lastChapter?.number
            if (number == null)
                "No empezado"
            else
                "Episodio ${lastChapter.number.roundedString()}"
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
        val seeingObject = getItem(position) ?: return 0
        return when {
            isFullList || seeingObject.organizer.state in 0..1 -> 0
            else -> 1
        }
    }

    internal class SeeingItem(itemView: View) : SeeingItemNormal(itemView) {
        val chapter: TextView by itemView.bind(R.id.chapter)
    }

    internal open class SeeingItemNormal(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val progressView: ProgressBar? by itemView.optionalBind(R.id.progress)
        val title: TextView by itemView.bind(R.id.title)
    }
}
