package knf.kuma.random

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.*
import knf.kuma.pojos.AnimeObject
import java.util.*

internal class RandomAdapter(private val activity: Activity) : RecyclerView.Adapter<RandomAdapter.RandomItem>() {
    private var list: MutableList<AnimeObject> = ArrayList()

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            R.layout.item_fav
        } else {
            R.layout.item_fav_grid
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RandomItem {
        return RandomItem(LayoutInflater.from(activity).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: RandomItem, position: Int) {
        val animeObject = list[position]
        holder.imageView.load(PatternUtil.getCover(animeObject.aid))
        holder.title.text = animeObject.name
        holder.type.text = animeObject.type
        holder.cardView.setOnClickListener { ActivityAnime.open(activity, animeObject, holder.imageView, false, true) }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update(list: MutableList<AnimeObject>) {
        if (this.list notSameContent list) {
            this.list = list
            notifyDataSetChanged()
        }
    }

    internal inner class RandomItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
        val type: TextView by itemView.bind(R.id.type)
    }
}
