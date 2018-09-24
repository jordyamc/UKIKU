package knf.kuma.explorer

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
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.PicassoSingle
import knf.kuma.commons.bind
import knf.kuma.commons.notSameContent
import knf.kuma.pojos.ExplorerObject
import java.util.*

class ExplorerFilesAdapter internal constructor(private val fragment: Fragment, private var listener: FragmentFiles.SelectedListener?) : RecyclerView.Adapter<ExplorerFilesAdapter.FileItem>() {

    private var list: MutableList<ExplorerObject> = ArrayList()

    private val layout: Int
        @LayoutRes
        get() = if (PreferenceManager.getDefaultSharedPreferences(fragment.context).getString("lay_type", "0") == "0") {
            R.layout.item_explorer
        } else {
            R.layout.item_explorer_grid
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileItem {
        return FileItem(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    fun setListener(listener: FragmentFiles.SelectedListener) {
        this.listener = listener
    }

    override fun onBindViewHolder(holder: FileItem, position: Int) {
        val explorerObject = list[position]
        PicassoSingle[fragment.context!!].load(explorerObject.img).into(holder.imageView)
        holder.title.text = explorerObject.name
        holder.chapter.text = String.format(Locale.getDefault(), if (explorerObject.count == 1) "%d archivo" else "%d archivos", explorerObject.count)
        holder.cardView.setOnClickListener { listener!!.onSelected(explorerObject) }
        holder.cardView.setOnLongClickListener {
            ActivityAnime.open(fragment, explorerObject, holder.imageView)
            true
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update(list: MutableList<ExplorerObject>) {
        if (this.list notSameContent list) {
            this.list = list
            notifyDataSetChanged()
        }
    }

    inner class FileItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val title: TextView by itemView.bind(R.id.title)
        val chapter: TextView by itemView.bind(R.id.chapter)
    }
}
