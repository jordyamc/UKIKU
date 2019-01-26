package knf.kuma.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.bind
import knf.kuma.commons.load
import knf.kuma.pojos.AnimeObject

class SearchAdapter internal constructor(private val fragment: Fragment) : PagedListAdapter<AnimeObject, SearchAdapter.ItemHolder>(DIFF_CALLBACK) {

    private val layType = PrefsUtil.layType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(LayoutInflater.from(parent.context).inflate(
                if (layType == "0")
                    R.layout.item_dir
                else
                    R.layout.item_dir_grid
                , parent, false))
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val animeObject = getItem(position)
        if (animeObject != null && fragment.context != null) {
            holder.imageView.load(PatternUtil.getCover(animeObject.aid))
            holder.textView.text = animeObject.name
            holder.cardView.setOnClickListener { ActivityAnime.open(fragment, animeObject, holder.imageView, false, true) }
        }
    }

    inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val textView: TextView by itemView.bind(R.id.title)
    }

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AnimeObject>() {
            override fun areItemsTheSame(oldItem: AnimeObject, newItem: AnimeObject): Boolean {
                return oldItem.key == newItem.key
            }

            override fun areContentsTheSame(oldItem: AnimeObject, newItem: AnimeObject): Boolean {
                return oldItem.name == newItem.name && oldItem.aid == newItem.aid
            }
        }
    }
}
