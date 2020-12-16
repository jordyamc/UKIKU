package knf.kuma.explorer

import androidx.recyclerview.widget.DiffUtil
import knf.kuma.pojos.ExplorerObject

class ExplorerObjectDiff: DiffUtil.ItemCallback<ExplorerObject>() {
    override fun areItemsTheSame(oldItem: ExplorerObject, newItem: ExplorerObject): Boolean = oldItem.key == newItem.key

    override fun areContentsTheSame(oldItem: ExplorerObject, newItem: ExplorerObject): Boolean = oldItem.chapters.size == newItem.chapters.size && oldItem.count == newItem.count
}