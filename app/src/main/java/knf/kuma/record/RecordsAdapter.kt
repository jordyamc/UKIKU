package knf.kuma.record

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
import knf.kuma.pojos.RecordObject
import xdroid.toaster.Toaster
import java.util.*

class RecordsAdapter(private val activity: Activity) : RecyclerView.Adapter<RecordsAdapter.RecordItem>() {
    private var items: MutableList<RecordObject> = ArrayList()

    private val dao = CacheDB.INSTANCE.recordsDAO()

    private val layout: Int
        @LayoutRes
        get() = if (PreferenceManager.getDefaultSharedPreferences(activity).getString("lay_type", "0") == "0") {
            R.layout.item_record
        } else {
            R.layout.item_record_grid
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordItem {
        return RecordItem(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: RecordItem, position: Int) {
        val item = items[position]
        val animeObject = item.animeObject
        if (animeObject != null)
            PicassoSingle[activity].load(PatternUtil.getCover(animeObject.aid!!)).into(holder.imageView)
        holder.title.text = item.name
        holder.chapter.text = item.chapter
        holder.cardView.setOnClickListener {
            if (item.animeObject != null)
                ActivityAnime.open(activity, item, holder.imageView)
            else
                Toaster.toast("Error al abrir")
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun remove(position: Int) {
        dao.delete(items[position])
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun update(items: MutableList<RecordObject>) {
        this.items = items
        notifyDataSetChanged()
    }

    inner class RecordItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
