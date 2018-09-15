package knf.kuma.queue

import android.app.Activity
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PicassoSingle
import knf.kuma.database.CacheDB
import knf.kuma.pojos.QueueObject
import java.util.*

internal class QueueAnimesAdapter internal constructor(private val activity: Activity) : RecyclerView.Adapter<QueueAnimesAdapter.AnimeHolder>() {
    private var listener: OnAnimeSelectedListener? = null
    private var list: MutableList<QueueObject> = ArrayList()

    private val layout: Int
        @LayoutRes
        get() = if (PreferenceManager.getDefaultSharedPreferences(activity).getString("lay_type", "0") == "0")
            R.layout.item_anim_queue
        else
            R.layout.item_anim_queue_grid

    init {
        this.listener = activity as OnAnimeSelectedListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeHolder {
        return AnimeHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: AnimeHolder, position: Int) {
        val queueObject = list[position]
        val img = PatternUtil.getCover(queueObject.chapter.aid)
        PicassoSingle[activity].load(img).into(holder.imageView)
        holder.title.text = queueObject.chapter.name
        val count = CacheDB.INSTANCE.queueDAO().countAlone(queueObject.chapter.aid)
        holder.type.text = String.format(Locale.getDefault(), if (count == 1) "%d episodio" else "%d episodios", count)
        holder.cardView.setOnClickListener { if (listener != null) listener!!.onSelect(queueObject) }
        holder.cardView.setOnLongClickListener {
            ActivityAnime.open(activity, queueObject, holder.imageView)
            true
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update(list: MutableList<QueueObject>) {
        this.list = list
        notifyDataSetChanged()
    }

    fun clear() {
        listener = null
    }

    internal interface OnAnimeSelectedListener {
        fun onSelect(queueObject: QueueObject)
    }

    internal inner class AnimeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @BindView(R.id.card)
        lateinit var cardView: CardView
        @BindView(R.id.img)
        lateinit var imageView: ImageView
        @BindView(R.id.title)
        lateinit var title: TextView
        @BindView(R.id.type)
        lateinit var type: TextView

        init {
            ButterKnife.bind(this, itemView)
        }
    }
}
