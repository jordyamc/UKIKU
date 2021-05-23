package knf.kuma.seeing

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnimeMaterial
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.pojos.SeeingObject
import org.jetbrains.anko.doAsync

internal class SeeingAdapterMaterial(private val activity: Activity, private val isFullList: Boolean) : PagingDataAdapter<SeeingObject, RecyclerView.ViewHolder>(SeeingObject.diffCallback), FastScrollRecyclerView.SectionedAdapter {

    private val seeingDAO = CacheDB.INSTANCE.seeingDAO()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> SeeingItem(LayoutInflater.from(parent.context).inflate(R.layout.item_record_grid_material, parent, false))
            else -> SeeingItemNormal(LayoutInflater.from(parent.context).inflate(R.layout.item_dir_grid_material, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty())
            super.onBindViewHolder(holder, position, payloads)
        else if (holder is SeeingItem) {
            holder.chapter.text = getCardText(getItem(position)!!)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val seeingObject = getItem(position) ?: return
        if (holder is SeeingItem)
            holder.chapter.text = getCardText(seeingObject)
        (holder as? SeeingItemNormal)?.apply {
            PicassoSingle.get().load(PatternUtil.getCover(seeingObject.aid)).into(imageView)
            title.text = seeingObject.title
            progressView?.visibility = View.GONE
            cardView.setOnClickListener { ActivityAnimeMaterial.open(activity, seeingObject) }
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
        return getItem(position)?.title?.substring(0, 1) ?: ""
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
        val seeingObject = getItem(position) ?: return 0
        return when {
            isFullList || seeingObject.state in 0..1 -> 0
            else -> 1
        }
    }

    internal inner class SeeingItem(itemView: View) : SeeingItemNormal(itemView) {
        val chapter: TextView by itemView.bind(R.id.chapter)
    }

    internal open inner class SeeingItemNormal(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val progressView: ProgressBar? by itemView.optionalBind(R.id.progress)
        val title: TextView by itemView.bind(R.id.title)
    }
}
