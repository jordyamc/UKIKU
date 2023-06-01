package knf.kuma.profile

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.backup.firestore.FirestoreManager
import knf.kuma.commons.inflate
import org.jetbrains.anko.find

class TopAdapter : ListAdapter<TopItem, TopAdapter.ItemHolder>(TopItem.diffCallback) {

    private val uid = FirestoreManager.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            when (viewType) {
                1 -> ItemHolder(parent.inflate(R.layout.item_top))
                else -> ItemHolder(parent.inflate(R.layout.item_top_current))
            }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    override fun getItemViewType(position: Int): Int = if (getItem(position).data.uid == uid) 0 else 1

    class ItemHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: TopItem, position: Int) {
            view.find<TextView>(R.id.ranking).text = "#${item.position}"
            view.find<TextView>(R.id.name).text = item.data.name
            view.find<TextView>(R.id.counter).text = item.data.number.toString()
            view.find<ImageView>(R.id.trophy).apply {
                visibility = if (position in 0..2) {
                    setImageResource(when (position) {
                        0 -> R.drawable.ic_trophy_gold
                        1 -> R.drawable.ic_trophy_silver
                        else -> R.drawable.ic_trophy_bronze
                    })
                    View.VISIBLE
                } else View.INVISIBLE
            }
        }

    }
}