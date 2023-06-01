package knf.kuma.recommended

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import org.jetbrains.anko.find

/**
 * Created by jordy on 26/03/2018.
 */

class RHHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.find(R.id.title)
}
