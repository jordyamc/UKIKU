package knf.kuma.search

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.load
import org.jetbrains.anko.find

internal class GenreAdapter(private val activity: Activity) : PagingDataAdapter<SearchObject, GenreAdapter.ItemHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_dir, parent, false))
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val animeObject = getItem(position)
        animeObject?.let {
            holder.imageView.load(PatternUtil.getCover(animeObject.aid))
            holder.textView.text = animeObject.name
            holder.cardView.setOnClickListener { ActivityAnime.open(activity, animeObject, holder.imageView, false, true) }
        }
    }

    internal inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.find(R.id.card)
        val imageView: ImageView = itemView.find(R.id.img)
        val textView: TextView = itemView.find(R.id.title)
    }

    companion object {

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchObject>() {
            override fun areItemsTheSame(oldItem: SearchObject, newItem: SearchObject): Boolean {
                return oldItem.key == newItem.key
            }

            override fun areContentsTheSame(oldItem: SearchObject, newItem: SearchObject): Boolean {
                return oldItem.name == newItem.name
            }
        }
    }
}
