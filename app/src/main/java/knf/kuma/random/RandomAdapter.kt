package knf.kuma.random

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.bind
import knf.kuma.commons.load
import knf.kuma.pojos.av1.DirectoryAV1Min

internal class RandomAdapter(private val activity: Activity) : RecyclerView.Adapter<RandomAdapter.RandomItem>() {
    private var list: List<DirectoryAV1Min> = ArrayList()

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            if (DesignUtils.isFlat) R.layout.item_fav_material else R.layout.item_fav
        } else {
            if (DesignUtils.isFlat) R.layout.item_fav_grid_material else R.layout.item_fav_grid
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RandomItem {
        return RandomItem(LayoutInflater.from(activity).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: RandomItem, position: Int) {
        val animeObject = list[position]
        holder.imageView.load(animeObject.imageUrl)
        holder.title.text = animeObject.name
        holder.type.text = getType(animeObject.category)
        holder.cardView.setOnClickListener { ActivityAnime.open(activity, animeObject, holder.imageView, true, true) }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun getType(category: Int): String {
        return when (category) {
            1 -> "Anime"
            2 -> "Película"
            3 -> "OVA"
            else -> "Especial"
        }
    }

    fun update(list: List<DirectoryAV1Min>) {
        this.list = list
        notifyDataSetChanged()
    }

    internal class RandomItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
        val type: TextView by itemView.bind(R.id.type)
    }
}
