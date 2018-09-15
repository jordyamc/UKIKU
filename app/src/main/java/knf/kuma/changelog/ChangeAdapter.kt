package knf.kuma.changelog

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.changelog.objects.Release
import kotlinx.android.synthetic.main.item_release.view.*

internal class ChangeAdapter(release: Release) : RecyclerView.Adapter<ChangeAdapter.ChangeItem>() {

    private val changes = release.changes

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChangeItem {
        return ChangeItem(LayoutInflater.from(parent.context).inflate(R.layout.item_release, parent, false))
    }

    override fun onBindViewHolder(holder: ChangeItem, position: Int) {
        val change = changes[position]
        setType(holder.type, change.type)
        holder.description.text = change.text
    }

    @SuppressLint("SetTextI18n")
    private fun setType(textView: TextView?, type: String) {
        when (type) {
            "new" -> {
                textView!!.text = "Nuevo"
                textView.setBackgroundResource(R.drawable.chip_new)
            }
            "change" -> {
                textView!!.text = "Cambio"
                textView.setBackgroundResource(R.drawable.chip_change)
            }
            "fix" -> {
                textView!!.text = "Arreglo"
                textView.setBackgroundResource(R.drawable.chip_error)
            }
            else -> {
                textView!!.text = "Cambio"
                textView.setBackgroundResource(R.drawable.chip_change)
            }
        }
    }

    override fun getItemCount(): Int {
        return changes.size
    }

    internal inner class ChangeItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var type: TextView = itemView.type
        var description: TextView = itemView.description
    }
}
