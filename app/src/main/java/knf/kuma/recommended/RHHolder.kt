package knf.kuma.recommended

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_recommend_header.view.*

/**
 * Created by jordy on 26/03/2018.
 */

class RHHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.title
}
