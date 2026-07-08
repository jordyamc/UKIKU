package knf.kuma.directory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentActivity
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnimeMaterial
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.bind
import knf.kuma.commons.load
import knf.kuma.pojos.av1.DirectoryAV1Min

class DirectoryAV1PageAdapter(private val context: FragmentActivity) : PagingDataAdapter<DirectoryAV1Min, DirectoryAV1PageAdapter.ItemHolder>(
    DirectoryAV1Min.DIFF), FastScrollRecyclerView.SectionedAdapter {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(LayoutInflater.from(parent.context).inflate(getLayType(), parent, false))
    }

    @LayoutRes
    private fun getLayType(): Int {
        return if (PrefsUtil.layType == "0") {
            if (DesignUtils.isFlat) {
                R.layout.item_dir_material
            } else {
                R.layout.item_dir
            }
        } else {
            if (DesignUtils.isFlat) {
                R.layout.item_dir_grid_material
            } else {
                R.layout.item_dir_grid
            }
        }
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val animeObject = getItem(position)
        if (animeObject?.aid != null) {
            holder.imageView.load(animeObject.imageUrl)
            holder.progressView.visibility = View.GONE
            holder.textView.text = animeObject.name
            holder.cardView.setOnClickListener { ActivityAnimeMaterial.open(context, animeObject, holder.imageView, true) }
        } else {
            holder.progressView.visibility = View.VISIBLE
            holder.textView.text = null
        }
    }

    override fun getSectionName(position: Int): String {
        return getItem(position)?.name?.first()?.uppercaseChar()?.toString() ?: ""
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val progressView: ProgressBar by itemView.bind(R.id.progress)
        val textView: TextView by itemView.bind(R.id.title)
    }
}
