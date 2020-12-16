package knf.kuma.explorer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnimeMaterial
import knf.kuma.commons.PicassoSingle
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.bind
import knf.kuma.pojos.ExplorerObject
import java.util.*

class ExplorerFilesAdapterMaterial internal constructor(private val fragment: Fragment, private var listener: FragmentFilesMaterial.SelectedListener?) : ListAdapter<ExplorerObject,ExplorerFilesAdapterMaterial.FileItem>(ExplorerObjectDiff()) {

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            R.layout.item_explorer_material
        } else {
            R.layout.item_explorer_grid_material
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileItem {
        return FileItem(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    fun setListener(listener: FragmentFilesMaterial.SelectedListener) {
        this.listener = listener
    }

    override fun onBindViewHolder(holder: FileItem, position: Int) {
        val explorerObject = getItem(position)
        PicassoSingle.get().load(explorerObject.img).into(holder.imageView)
        holder.title.text = explorerObject.name
        holder.chapter.text = String.format(Locale.getDefault(), if (explorerObject.count == 1) "%d archivo" else "%d archivos", explorerObject.count)
        holder.cardView.setOnClickListener { listener?.onSelected(explorerObject) }
        holder.cardView.setOnLongClickListener {
            ActivityAnimeMaterial.open(fragment, explorerObject, holder.imageView)
            true
        }
    }

    fun update(list: List<ExplorerObject>) {
        submitList(list)
    }

    inner class FileItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
        val chapter: TextView by itemView.bind(R.id.chapter)
    }
}
