package knf.kuma.emision

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.*
import knf.kuma.custom.HiddenOverlay
import knf.kuma.database.CacheDB
import knf.kuma.search.SearchObjectFav
import knf.kuma.widgets.emision.WEmisionProvider
import kotlinx.android.synthetic.main.item_emision.view.*
import org.jetbrains.anko.doAsync


class EmissionAdapter internal constructor(private val fragment: Fragment) : RecyclerView.Adapter<EmissionAdapter.EmissionItem>() {

    val removeListener = fragment as RemoveListener
    var list: MutableList<SearchObjectFav> = ArrayList()

    private var blacklist: MutableSet<String> = PrefsUtil.emissionBlacklist
    private var showHidden: Boolean = PrefsUtil.emissionShowHidden
    private val showHeart = PrefsUtil.emissionShowFavs

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmissionItem {
        return EmissionItem(LayoutInflater.from(parent.context).inflate(R.layout.item_emision, parent, false))
    }

    override fun onBindViewHolder(holder: EmissionItem, position: Int) {
        val animeObject = list[position]
        holder.imageView.load(PatternUtil.getCover(animeObject.aid))
        holder.title.text = animeObject.name
        holder.hiddenOverlay.setHidden(blacklist.contains(animeObject.aid), false)
        holder.heart.visibility = when {
            showHeart && animeObject.isFav -> View.VISIBLE
            else -> View.GONE
        }
        //holder.observeFav(fragment, animeObject.aid, showHeart)
        holder.cardView.setOnClickListener { ActivityAnime.open(fragment, animeObject, holder.imageView, false, animate = true) }
        holder.cardView.setOnLongClickListener {
            val removed: Boolean = if (blacklist.contains(animeObject.aid)) {
                updateList(true, animeObject.aid)
                true
            } else {
                updateList(false, animeObject.aid)
                false
            }
            if (showHidden) {
                holder.hiddenOverlay.setHidden(!removed, true)
            } else if (!removed) {
                remove(holder.adapterPosition)
            }
            true
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update(newList: MutableList<SearchObjectFav>, animate: Boolean = true, callback: () -> Unit) {
        if (list notSameContent newList)
            if (PrefsUtil.useSmoothAnimations && newList.isNotEmpty())
                doAsync {
                    blacklist = PrefsUtil.emissionBlacklist
                    showHidden = PrefsUtil.emissionShowHidden
                    val result = if (animate) DiffUtil.calculateDiff(EmissionDiff(list, newList), true) else null
                    list = newList
                    fragment.doOnUI {
                        try {
                            if (animate)
                                result?.dispatchUpdatesTo(this@EmissionAdapter)
                            else
                                notifyDataSetChanged()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            notifyDataSetChanged()
                        }
                        callback.invoke()
                    }
                }
            else {
                blacklist = PrefsUtil.emissionBlacklist
                showHidden = PrefsUtil.emissionShowHidden
                list = newList
                notifyDataSetChanged()
            }
    }

    private fun updateList(remove: Boolean, aid: String) {
        this.blacklist = LinkedHashSet(PrefsUtil.emissionBlacklist)
        if (remove)
            blacklist.remove(aid)
        else
            blacklist.add(aid)
        PrefsUtil.emissionBlacklist = blacklist
        WEmisionProvider.update(fragment.context)
    }

    fun remove(position: Int) {
        if (position >= 0 && position <= list.size - 1) {
            list.removeAt(position)
            notifyItemRemoved(position)
            removeListener.onRemove(list.size <= 0)
        }
    }

    inner class EmissionItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.card
        val imageView: ImageView = itemView.img
        val hiddenOverlay: HiddenOverlay = itemView.hidden
        val heart: ImageView = itemView.heart
        val title: TextView = itemView.title

        private lateinit var liveData: LiveData<Boolean>
        private lateinit var observer: Observer<Boolean>

        fun observeFav(fragment: Fragment, aid: String, show: Boolean) {
            if (::liveData.isInitialized && ::observer.isInitialized)
                liveData.removeObserver(observer)
            if (!show) {
                heart.visibility = View.GONE
                return
            }
            liveData = CacheDB.INSTANCE.favsDAO().isFavLive(aid.toInt()).distinct
            observer = Observer {
                if (!PrefsUtil.emissionShowFavs)
                    heart.visibility = View.GONE
                else
                    heart.visibility = if (it) View.VISIBLE else View.GONE
            }
            liveData.observe(fragment, observer)
        }
    }
}

internal class EmissionDiff(
        private val oldList: MutableList<SearchObjectFav>,
        private val newList: MutableList<SearchObjectFav>) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].isFav == newList[newItemPosition].isFav
    }
}
