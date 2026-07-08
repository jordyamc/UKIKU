package knf.kuma.animeinfo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.commons.load
import knf.kuma.databinding.ItemRelatedBinding
import knf.kuma.pojos.av1.Relation

internal class AnimeRelatedAdapterMaterial(private val fragment: Fragment, private val list: List<Relation>) : RecyclerView.Adapter<AnimeRelatedAdapterMaterial.RelatedHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelatedHolder {
        return RelatedHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_related, parent, false))
    }

    override fun onBindViewHolder(holder: RelatedHolder, position: Int) {
        val related = list[position]
        holder.textView.text = related.name
        holder.relation.text = getRelation(related.relation)
        holder.imageView.visibility = View.VISIBLE
        holder.imageView.load(related.imageUrl)
        holder.cardView.setOnClickListener { ActivityAnimeMaterial.open(fragment, related, holder.imageView) }
    }

    fun getRelation(id: Int): String {
        return when(id) {
            8 -> "Historia Principal"
            1 -> "Precuela"
            2 -> "Secuela"
            4 -> "Versión Alternativa"
            3 -> "Ambientación Alternativa"
            7 -> "Historia Completa"
            5 -> "Historia Paralela"
            6 -> "Resumen"
            else -> "Otro"
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    internal class RelatedHolder(itemView: View, binding: ItemRelatedBinding = ItemRelatedBinding.bind(itemView)) : RecyclerView.ViewHolder(itemView) {
        val cardView: LinearLayout = binding.card
        val imageView: ImageView = binding.img
        val textView: TextView = binding.title
        val relation: TextView = binding.relation
    }
}
