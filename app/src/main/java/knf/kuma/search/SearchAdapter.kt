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
import butterknife.BindView
import butterknife.ButterKnife
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PicassoSingle
import knf.kuma.pojos.AnimeObject

class SearchAdapter internal constructor(private val fragment: Fragment) : PagedListAdapter<AnimeObject, SearchAdapter.ItemHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_dir, parent, false))
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val animeObject = getItem(position)
        if (animeObject != null) {
            PicassoSingle[fragment.context!!].load(PatternUtil.getCover(animeObject.aid!!)).into(holder.imageView)
            holder.textView.text = animeObject.name
            holder.cardView.setOnClickListener { ActivityAnime.open(fragment, animeObject, holder.imageView, false, true) }
        }
    }

    inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @BindView(R.id.card)
        lateinit var cardView: CardView
        @BindView(R.id.img)
        lateinit var imageView: ImageView
        @BindView(R.id.title)
        lateinit var textView: TextView

        init {
            ButterKnife.bind(this, itemView)
        }
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
