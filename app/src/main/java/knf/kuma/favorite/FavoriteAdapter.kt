package knf.kuma.favorite

import android.content.Context
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PicassoSingle
import knf.kuma.pojos.FavoriteObject
import java.util.*

class FavoriteAdapter(private val fragment: Fragment, private val recyclerView: RecyclerView) : RecyclerView.Adapter<FavoriteAdapter.ItemHolder>() {

    private val context: Context? = fragment.context
    private var list: MutableList<FavoriteObject> = ArrayList()

    private val layout: Int
        @LayoutRes
        get() = if (PreferenceManager.getDefaultSharedPreferences(context).getString("lay_type", "0") == "0") {
            R.layout.item_fav
        } else {
            R.layout.item_fav_grid
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val favoriteObject = list[position]
        PicassoSingle[context!!].load(PatternUtil.getCover(favoriteObject.aid!!)).into(holder.imageView)
        holder.title.text = favoriteObject.name
        holder.type.text = favoriteObject.type
        holder.cardView.setOnClickListener { ActivityAnime.open(fragment, favoriteObject, holder.imageView) }
    }

    fun updateList(list: MutableList<FavoriteObject>) {
        this.list = list
        recyclerView.post { notifyDataSetChanged() }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
