package knf.kuma.emision

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.*
import knf.kuma.custom.HiddenOverlay
import knf.kuma.pojos.AnimeObject
import knf.kuma.widgets.emision.WEmisionProvider
import kotlinx.android.synthetic.main.item_emision.view.*
import org.jetbrains.anko.doAsync
import java.util.*


class EmissionAdapter internal constructor(private val fragment: Fragment) : RecyclerView.Adapter<EmissionAdapter.EmissionItem>() {

    var list: MutableList<AnimeObject> = ArrayList()

    private var blacklist: MutableSet<String> = PrefsUtil.emissionBlacklist
    private var showHidden: Boolean = PrefsUtil.emissionShowHidden

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmissionItem {
        return EmissionItem(LayoutInflater.from(parent.context).inflate(R.layout.item_emision, parent, false))
    }

    override fun onBindViewHolder(holder: EmissionItem, position: Int) {
        val animeObject = list[position]
        holder.imageView.load(PatternUtil.getCover(animeObject.aid))
        holder.title.text = animeObject.name
        holder.hiddenOverlay.setHidden(blacklist.contains(animeObject.aid), false)
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

    fun update(newList: MutableList<AnimeObject>, animate: Boolean = true, callback: () -> Unit) {
        if (PrefsUtil.useSmoothAnimations)
            doAsync {
                blacklist = PrefsUtil.emissionBlacklist
                showHidden = PrefsUtil.emissionShowHidden
                val result = if (animate) DiffUtil.calculateDiff(EmissionDiff(list, newList), true) else null
                list = newList
                doOnUI {
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
        else if (list notSameContent newList) {
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
        }
    }

    inner class EmissionItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.card
        val imageView: ImageView = itemView.img
        val hiddenOverlay: HiddenOverlay = itemView.hidden
        val title: TextView = itemView.title
    }
}

internal class EmissionDiff(
        private val oldList: MutableList<AnimeObject>,
        private val newList: MutableList<AnimeObject>) : DiffUtil.Callback() {

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
        return true
    }
}
