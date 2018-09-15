package knf.kuma.recommended

import android.view.View
import android.widget.ImageView
import android.widget.TextView

import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import knf.kuma.R

/**
 * Created by jordy on 26/03/2018.
 */

class RIHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @BindView(R.id.card)
    lateinit var cardView: CardView
    @BindView(R.id.img)
    lateinit var img: ImageView
    @BindView(R.id.title)
    lateinit var title: TextView
    @BindView(R.id.type)
    lateinit var type: TextView

    init {
        ButterKnife.bind(this, itemView)
    }
}
