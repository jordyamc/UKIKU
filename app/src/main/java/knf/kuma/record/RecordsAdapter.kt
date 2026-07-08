package knf.kuma.record

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.bind
import knf.kuma.commons.load
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.Record
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecordsAdapter(private val activity: AppCompatActivity) : PagingDataAdapter<Record, RecordsAdapter.RecordItem>(Record.DIFF) {

    private val dao = CacheDB.INSTANCE.recordAV1DAO()

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            if (DesignUtils.isFlat) R.layout.item_record_material else R.layout.item_record
        } else {
            if (DesignUtils.isFlat) R.layout.item_record_grid_material else R.layout.item_record_grid
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordItem {
        return RecordItem(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: RecordItem, position: Int) {
        val item = getItem(position) ?: return
        holder.imageView.load(item.imageUrl)
        holder.title.text = item.name
        holder.chapter.text = item.chapter
        holder.cardView.setOnClickListener {
            ActivityAnime.open(activity, item, holder.imageView)
        }
    }

    fun remove(position: Int) {
        activity.lifecycleScope.launch(Dispatchers.IO){
            getItem(position)?.let {
                dao.delete(it)
            }
            syncData { history() }
        }
    }

    class RecordItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
        val chapter: TextView by itemView.bind(R.id.chapter)
    }
}
