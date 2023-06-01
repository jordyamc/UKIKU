package knf.kuma.queue

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.commons.notSameContent
import knf.kuma.database.CacheDB
import knf.kuma.pojos.QueueObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.find
import java.util.*

internal class QueueListAdapter(val callback: () -> Unit) : RecyclerView.Adapter<QueueListAdapter.ListItemHolder>() {
    private var current = "0000"
    var list: MutableList<QueueObject> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemHolder {
        return ListItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_queue, parent, false))
    }

    override fun onBindViewHolder(holder: ListItemHolder, position: Int) {
        val queueObject = list[position]
        holder.chapter.text = queueObject.chapter.number
        holder.icon.setImageResource(if (queueObject.isFile) R.drawable.ic_chap_down else R.drawable.ic_web)
        holder.actionDelete.setOnClickListener { remove(holder.adapterPosition) }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update(aid: String, list: MutableList<QueueObject>) {
        if (current != aid && this.list notSameContent list) {
            current = aid
            this.list = list
            notifyDataSetChanged()
        }
    }

    fun remove(position: Int) {
        if (position != -1) {
            GlobalScope.launch(Dispatchers.Main){
                withContext(Dispatchers.IO) { CacheDB.INSTANCE.queueDAO().remove(list[position]) }
                list.removeAt(position)
                notifyItemRemoved(position)
                if (list.isEmpty()) callback.invoke()
            }
        }
    }

    internal inner class ListItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chapter: TextView = itemView.find(R.id.chapter)
        val icon: ImageView = itemView.find(R.id.icon)
        val actionDelete: ImageButton = itemView.find(R.id.action_delete)
    }
}
