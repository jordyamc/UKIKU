package knf.kuma.random

import android.app.Activity
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PicassoSingle
import knf.kuma.pojos.AnimeObject
import java.util.*

internal class RandomAdapter(private val activity: Activity) : RecyclerView.Adapter<RandomAdapter.RandomItem>() {
    private var list: MutableList<AnimeObject> = ArrayList()

    private val layout: Int
        @LayoutRes
        get() = if (PreferenceManager.getDefaultSharedPreferences(activity).getString("lay_type", "0") == "0") {
            R.layout.item_fav
        } else {
            R.layout.item_fav_grid
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RandomItem {
        return RandomItem(LayoutInflater.from(activity).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: RandomItem, position: Int) {
        val animeObject = list[position]
        PicassoSingle[activity].load(PatternUtil.getCover(animeObject.aid!!)).into(holder.imageView)
        holder.title.text = animeObject.name
        holder.type.text = animeObject.type
        holder.cardView.setOnClickListener { ActivityAnime.open(activity, animeObject, holder.imageView, false, true) }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update(list: MutableList<AnimeObject>) {
        this.list = list
        notifyDataSetChanged()
    }

    internal inner class RandomItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @BindView(R.id.card)
        lateinit var cardView: CardView
        @BindView(R.id.img)
        lateinit var imageView: ImageView
        @BindView(R.id.title)
        lateinit var title: TextView
        @BindView(R.id.type)
        lateinit var type: TextView

        init {
            ButterKnife.bind(this, itemView)
        }
    }
}
