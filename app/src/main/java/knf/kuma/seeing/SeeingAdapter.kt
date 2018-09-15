package knf.kuma.seeing

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
import knf.kuma.pojos.SeeingObject
import java.util.*

internal class SeeingAdapter(private val activity: Activity) : RecyclerView.Adapter<SeeingAdapter.SeeingItem>() {

    var list: MutableList<SeeingObject> = ArrayList()
    private val seeingDAO = CacheDB.INSTANCE.seeingDAO()

    private val layout: Int
        @LayoutRes
        get() = if (PreferenceManager.getDefaultSharedPreferences(activity).getString("lay_type", "0") == "0") {
            R.layout.item_record
        } else {
            R.layout.item_record_grid
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeeingItem {
        return SeeingItem(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: SeeingItem, position: Int) {
        val seeingObject = list[position]
        val lastChapter = seeingObject.lastChapter
        PicassoSingle[activity].load(PatternUtil.getCover(seeingObject.aid!!)).into(holder.imageView)
        holder.title.text = seeingObject.title
        holder.chapter.text = lastChapter?.number ?: "No empezado"
        holder.cardView.setOnClickListener { ActivityAnime.open(activity, seeingObject, holder.imageView) }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update(list: MutableList<SeeingObject>) {
        this.list = list
        notifyDataSetChanged()
    }

    fun undo(seeingObject: SeeingObject, position: Int) {
        seeingDAO.add(seeingObject)
        list.add(position, seeingObject)
        notifyItemInserted(position)
    }

    fun remove(position: Int) {
        seeingDAO.remove(list[position])
        list.removeAt(position)
        notifyItemRemoved(position)
    }

    internal inner class SeeingItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @BindView(R.id.card)
        lateinit var cardView: CardView
        @BindView(R.id.img)
        lateinit var imageView: ImageView
        @BindView(R.id.title)
        lateinit var title: TextView
        @BindView(R.id.chapter)
        lateinit var chapter: TextView

        init {
            ButterKnife.bind(this, itemView)
        }
    }
}
