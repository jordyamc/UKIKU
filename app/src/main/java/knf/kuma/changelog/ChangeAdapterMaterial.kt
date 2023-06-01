package knf.kuma.changelog

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import knf.kuma.R
import knf.kuma.changelog.objects.Release
import org.jetbrains.anko.find

internal class ChangeAdapterMaterial(release: Release) : RecyclerView.Adapter<ChangeAdapterMaterial.ChangeItem>() {

    private val changes = release.changes

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChangeItem {
        return ChangeItem(LayoutInflater.from(parent.context).inflate(R.layout.item_release_material, parent, false))
    }

    override fun onBindViewHolder(holder: ChangeItem, position: Int) {
        val change = changes[position]
        setType(holder.type, change.type)
        holder.description.text = change.text
    }

    @SuppressLint("SetTextI18n")
    private fun setType(chip: Chip, type: String) {
        when (type) {
            "new" -> {
                chip.text = "Nuevo"
                chip.setChipBackgroundColorResource(R.color.release_new)
            }
            "fix" -> {
                chip.text = "Arreglo"
                chip.setChipBackgroundColorResource(R.color.release_error)
            }
            else -> {
                chip.text = "Cambio"
                chip.setChipBackgroundColorResource(R.color.release_change)
            }
        }
    }

    override fun getItemCount(): Int {
        return changes.size
    }

    internal inner class ChangeItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var type: Chip = itemView.find(R.id.type)
        var description: TextView = itemView.find(R.id.description)
    }
}
