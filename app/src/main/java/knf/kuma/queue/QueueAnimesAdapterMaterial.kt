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
import knf.kuma.animeinfo.ActivityAnimeMaterial
import knf.kuma.commons.*
import knf.kuma.pojos.QueueObject
import java.util.*

internal class QueueAnimesAdapterMaterial internal constructor(private val activity: Activity) : RecyclerView.Adapter<QueueAnimesAdapterMaterial.AnimeHolder>() {
    private var listener: OnAnimeSelectedListener? = null
    private var list: MutableList<QueueObject> = ArrayList()

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0")
            R.layout.item_anim_queue_material
        else
            R.layout.item_anim_queue_grid_material

    init {
        this.listener = activity as OnAnimeSelectedListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeHolder {
        return AnimeHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: AnimeHolder, position: Int) {
        val queueObject = list[position]
        noCrash {
            PicassoSingle.get().load(PatternUtil.getCover(queueObject.chapter.aid)).into(holder.imageView)
            holder.title.text = PatternUtil.fromHtml(queueObject.chapter.name)
        }
        holder.type.text = String.format(Locale.getDefault(), if (queueObject.count == 1) "%d episodio" else "%d episodios", queueObject.count)
        holder.cardView.setOnClickListener { listener?.onSelect(queueObject) }
        holder.cardView.setOnLongClickListener {
            ActivityAnimeMaterial.open(activity, queueObject, holder.imageView)
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

    internal inner class AnimeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
        val type: TextView by itemView.bind(R.id.type)
    }
}
