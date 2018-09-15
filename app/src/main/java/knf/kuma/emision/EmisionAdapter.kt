package knf.kuma.emision

import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PicassoSingle
import knf.kuma.custom.HiddenOverlay
import knf.kuma.pojos.AnimeObject
import knf.kuma.widgets.emision.WEmisionProvider
import kotlinx.android.synthetic.main.item_emision.view.*
import java.util.*

class EmisionAdapter internal constructor(private val fragment: Fragment) : RecyclerView.Adapter<EmisionAdapter.EmisionItem>() {

    var list: MutableList<AnimeObject> = ArrayList()

    private var blacklist: MutableSet<String>? = null
    private var showHidden: Boolean = false

    init {
        this.blacklist = PreferenceManager.getDefaultSharedPreferences(fragment.context).getStringSet("emision_blacklist", LinkedHashSet())
        this.showHidden = PreferenceManager.getDefaultSharedPreferences(fragment.context).getBoolean("show_hidden", false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmisionItem {
        return EmisionItem(LayoutInflater.from(parent.context).inflate(R.layout.item_emision, parent, false))
    }

    override fun onBindViewHolder(holder: EmisionItem, position: Int) {
        val animeObject = list[position]
        PicassoSingle[fragment.context!!].load(PatternUtil.getCover(animeObject.aid!!)).into(holder.imageView)
        holder.title.text = animeObject.name
        holder.hiddenOverlay.setHidden(blacklist!!.contains(animeObject.aid!!), false)
        holder.cardView.setOnClickListener { ActivityAnime.open(fragment, animeObject, holder.imageView, true, true) }
        holder.cardView.setOnLongClickListener {
            val removed: Boolean = if (blacklist!!.contains(animeObject.aid!!)) {
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

    fun update(list: MutableList<AnimeObject>) {
        this.blacklist = PreferenceManager.getDefaultSharedPreferences(fragment.context).getStringSet("emision_blacklist", LinkedHashSet())
        this.showHidden = PreferenceManager.getDefaultSharedPreferences(fragment.context).getBoolean("show_hidden", false)
        this.list = list
        notifyDataSetChanged()
    }

    private fun updateList(remove: Boolean, aid: String?) {
        this.blacklist = LinkedHashSet(PreferenceManager.getDefaultSharedPreferences(fragment.context).getStringSet("emision_blacklist", LinkedHashSet()))
        if (remove)
            blacklist!!.remove(aid)
        else
            blacklist!!.add(aid!!)
        PreferenceManager.getDefaultSharedPreferences(fragment.context).edit().putStringSet("emision_blacklist", blacklist).apply()
        WEmisionProvider.update(fragment.context!!)
    }

    fun remove(position: Int) {
        if (position >= 0 && position <= list.size - 1) {
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    inner class EmisionItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.card
        val imageView: ImageView = itemView.img
        val hiddenOverlay: HiddenOverlay = itemView.hidden
        val title: TextView = itemView.title
    }
}
