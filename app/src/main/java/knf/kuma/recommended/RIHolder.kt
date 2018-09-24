package knf.kuma.recommended

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.commons.bind

/**
 * Created by jordy on 26/03/2018.
 */

class RIHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val cardView: CardView by itemView.bind(R.id.card)
    val img: ImageView by itemView.bind(R.id.img)
    val title: TextView by itemView.bind(R.id.title)
    val type: TextView by itemView.bind(R.id.type)
}
