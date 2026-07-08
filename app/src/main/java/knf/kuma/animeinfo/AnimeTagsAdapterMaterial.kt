package knf.kuma.animeinfo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.pojos.av1.Genre
import knf.kuma.search.GenreActivityMaterial
import org.jetbrains.anko.find

internal class AnimeTagsAdapterMaterial(private val context: Context, private val list: List<Genre>) : RecyclerView.Adapter<AnimeTagsAdapterMaterial.TagHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagHolder {
        return TagHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chip, parent, false))
    }

    override fun onBindViewHolder(holder: TagHolder, position: Int) {
        holder.chip.text = list[position].name
        holder.chip.setOnClickListener { GenreActivityMaterial.open(context, list[position]) }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    internal class TagHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var chip: TextView = itemView.find(R.id.chip)
    }
}
