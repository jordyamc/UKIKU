package knf.kuma.queue

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.bind
import knf.kuma.commons.doOnUIGlobal
import knf.kuma.commons.load
import knf.kuma.commons.notSameContent
import knf.kuma.pojos.QueueObject
import java.util.Locale

internal class QueueAnimesAdapter internal constructor(private val activity: Activity) : RecyclerView.Adapter<QueueAnimesAdapter.AnimeHolder>() {
    private var listener: OnAnimeSelectedListener? = null
    private var list: MutableList<QueueObject> = ArrayList()

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0")
            if (DesignUtils.isFlat) R.layout.item_anim_queue_material else R.layout.item_anim_queue
        else
            if (DesignUtils.isFlat) R.layout.item_anim_queue_grid_material else R.layout.item_anim_queue_grid

    init {
        this.listener = activity as OnAnimeSelectedListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeHolder {
        return AnimeHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: AnimeHolder, position: Int) {
        val queueObject = list[position]
        holder.imageView.load("https://cdn.animeav1.com/covers/${queueObject.chapter.aid}.jpg")
        holder.title.text = queueObject.chapter.name
        holder.type.text = String.format(Locale.getDefault(), if (queueObject.count == 1) "%d episodio" else "%d episodios", queueObject.count)
        holder.cardView.setOnClickListener { listener?.onSelect(queueObject) }
        holder.cardView.setOnLongClickListener {
            ActivityAnime.open(activity, queueObject, holder.imageView)
            true
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update(list: MutableList<QueueObject>) {
        if (this.list notSameContent list) {
            this.list = list
            doOnUIGlobal { notifyDataSetChanged() }
        }
    }

    fun clear() {
        listener = null
    }

    internal interface OnAnimeSelectedListener {
        fun onSelect(queueObject: QueueObject)
    }

    internal class AnimeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
        val type: TextView by itemView.bind(R.id.type)
    }
}
