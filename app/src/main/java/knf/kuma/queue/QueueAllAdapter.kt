package knf.kuma.queue

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import knf.kuma.R
import knf.kuma.commons.notSameContent
import knf.kuma.pojos.QueueObject
import kotlinx.android.synthetic.main.item_queue_full.view.*
import java.util.*

internal class QueueAllAdapter internal constructor(activity: Activity) : RecyclerView.Adapter<QueueAllAdapter.AnimeHolder>(), ItemTouchHelperAdapter {

    private val dragListener: OnStartDragListener = activity as OnStartDragListener
    var list: MutableList<QueueObject> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeHolder {
        return AnimeHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_queue_full, parent, false))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: AnimeHolder, position: Int) {
        val queueObject = list[position]
        holder.title.text = queueObject.chapter.name
        holder.chapter.text = queueObject.chapter.number
        holder.state.setImageResource(if (queueObject.isFile) R.drawable.ic_queue_file else R.drawable.ic_web)
        holder.dragView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                dragListener.onStartDrag(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun update(list: MutableList<QueueObject>) {
        if (this.list notSameContent list) {
            this.list = list
            notifyDataSetChanged()
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                val fromTime = list[i].time
                list[i].time = list[i + 1].time
                list[i + 1].time = fromTime
                QueueManager.update(list[i], list[i + 1])
                Collections.swap(list, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                val fromTime = list[i].time
                list[i].time = list[i - 1].time
                list[i - 1].time = fromTime
                QueueManager.update(list[i], list[i - 1])
                Collections.swap(list, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onItemDismiss(position: Int) {
        QueueManager.remove(list[position])
        list.removeAt(position)
        notifyItemRemoved(position)
        if (list.size == 0)
            dragListener.onListCleared()
    }

    internal interface OnStartDragListener {
        fun onStartDrag(holder: RecyclerView.ViewHolder)

        fun onListCleared()
    }

    internal inner class AnimeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.card
        val dragView: ImageView = itemView.drag
        val title: TextView = itemView.title
        val chapter: TextView = itemView.chapter
        val state: ImageView = itemView.state
    }
}
