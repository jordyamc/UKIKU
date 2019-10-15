package knf.kuma.profile

import androidx.recyclerview.widget.DiffUtil
import knf.kuma.backup.firestore.data.TopData

data class TopItem(val position: Int, val data: TopData) {
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<TopItem>() {
            override fun areItemsTheSame(oldItem: TopItem, newItem: TopItem): Boolean = oldItem.data.uid == newItem.data.uid

            override fun areContentsTheSame(oldItem: TopItem, newItem: TopItem): Boolean {
                return oldItem.position == newItem.position && oldItem.data.name == newItem.data.name && oldItem.data.number == newItem.data.number
            }
        }
    }
}