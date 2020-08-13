package knf.kuma.home

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.commons.*
import knf.kuma.pojos.QueueObject
import knf.kuma.queue.QueueActivity
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.*

class QueueAdapterMaterial(val fragment: HomeFragmentMaterial) : UpdateableAdapter<QueueAdapterMaterial.RecentViewHolder>() {

    private var list: List<QueueObject> = emptyList()

    override fun updateList(list: List<Any>) {
        doAsync {
            this@QueueAdapterMaterial.list = list.transform()
            doOnUI { notifyDataSetChanged() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder = RecentViewHolder(parent.inflate(R.layout.item_fav_grid_card_material))


    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        val item = list[position]
        holder.img.load(PatternUtil.getCover(item.chapter.aid))
        holder.title.text = item.chapter.name
        holder.type?.text = String.format(Locale.getDefault(), if (item.count == 1) "%d episodio" else "%d episodios", item.count)
        holder.root.onClick { QueueActivity.open(fragment.context, item.chapter.aid) }
    }

    class RecentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View by itemView.bind(R.id.card)
        val img: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
        val type: TextView? by itemView.optionalBind(R.id.type)
    }
}