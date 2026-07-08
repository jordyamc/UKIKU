package knf.kuma.emision

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.load
import knf.kuma.custom.HiddenOverlay
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.DirectoryAV1Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.find


class EmissionAdapter internal constructor(private val fragment: Fragment) : RecyclerView.Adapter<EmissionAdapter.EmissionItem>() {
    var list: List<DirectoryAV1Calendar> = emptyList()
    private val showHeart = PrefsUtil.emissionShowFavs

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmissionItem {
        return EmissionItem(LayoutInflater.from(parent.context).inflate(if (DesignUtils.isFlat) R.layout.item_emision_material else R.layout.item_emision, parent, false))
    }

    override fun onBindViewHolder(holder: EmissionItem, position: Int) {
        val animeObject = list[position]
        holder.imageView.load(animeObject.imageUrl)
        holder.title.text = animeObject.name
        holder.hiddenOverlay.setHidden(animeObject.isHidden, false)
        holder.heart.isVisible = showHeart && animeObject.isFavorite
        holder.cardView.setOnClickListener { ActivityAnime.open(fragment, animeObject, holder.imageView, false, animate = true) }
        holder.cardView.setOnLongClickListener {
            fragment.viewLifecycleOwner.lifecycleScope.launch {
                if (animeObject.isHidden) {
                    CacheDB.INSTANCE.calendarBlacklistDAO().remove(animeObject)
                } else {
                    CacheDB.INSTANCE.calendarBlacklistDAO().add(animeObject)
                }
                animeObject.isHidden = !animeObject.isHidden
                if (PrefsUtil.emissionShowHidden) {
                    withContext(Dispatchers.Main) {
                        holder.hiddenOverlay.setHidden(animeObject.isHidden, true)
                    }
                }
            }
            true
        }
        if (showHeart) {
            holder.favJob = fragment.viewLifecycleOwner.lifecycleScope.launch {
                CacheDB.INSTANCE.favoriteAV1DAO().isFavFlow(animeObject.aid).collectLatest {
                    holder.heart.isVisible = it
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onViewRecycled(holder: EmissionItem) {
        super.onViewRecycled(holder)
        holder.favJob?.cancel()
    }

    fun update(newList: List<DirectoryAV1Calendar>, animate: Boolean = true) {
        if (PrefsUtil.useSmoothAnimations && newList.isNotEmpty())
            fragment.viewLifecycleOwner.lifecycleScope.launch {
                val result = if (animate) DiffUtil.calculateDiff(EmissionDiff(list, newList), true) else null
                list = newList
                launch(Dispatchers.Main) {
                    try {
                        if (animate)
                            result?.dispatchUpdatesTo(this@EmissionAdapter)
                        else
                            notifyDataSetChanged()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        notifyDataSetChanged()
                    }
                }
            }
        else {
            list = newList
            notifyDataSetChanged()
        }
    }

    class EmissionItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View = itemView.find(R.id.card)
        val imageView: ImageView = itemView.find(R.id.img)
        val hiddenOverlay: HiddenOverlay = itemView.find(R.id.hidden)
        val heart: View = itemView.find(R.id.heart)
        val title: TextView = itemView.find(R.id.title)
        var favJob: Job? = null
    }
}

internal class EmissionDiff(
    private val oldList: List<DirectoryAV1Calendar>,
    private val newList: List<DirectoryAV1Calendar>) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].aid == newList[newItemPosition].aid
    }

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].isHidden == newList[newItemPosition].isHidden
    }

    override fun getChangePayload(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Any {
        return true
    }
}
