package knf.kuma.record

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.pojos.RecordObject
import xdroid.toaster.Toaster
import java.util.*

class RecordsAdapter(private val activity: Activity) : RecyclerView.Adapter<RecordsAdapter.RecordItem>() {
    private var items: MutableList<RecordObject> = ArrayList()

    private val dao = CacheDB.INSTANCE.recordsDAO()

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
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
        holder.chapter.text = getCardText(item)
        holder.cardView.setOnClickListener {
            if (item.animeObject != null)
                ActivityAnime.open(activity, item, holder.imageView)
            else
                Toaster.toast("Error al abrir")
        }
    }

    private fun getCardText(recordObject: RecordObject): String {
        return if (!recordObject.chapter.startsWith("Episodio "))
            "Episodio ${recordObject.chapter}"
        else
            recordObject.chapter
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
        if (this.items notSameContent items) {
            this.items = items
            notifyDataSetChanged()
        }
    }

    inner class RecordItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
        val chapter: TextView by itemView.bind(R.id.chapter)
    }
}
