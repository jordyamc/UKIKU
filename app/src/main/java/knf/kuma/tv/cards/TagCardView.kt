package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import knf.kuma.R
import knf.kuma.tv.BindableCardView
import kotlinx.android.synthetic.main.item_tv_card.view.*

class TagCardView(context: Context) : BindableCardView<String>(context) {

    override val imageView: ImageView
        get() = img
    override val layoutResource: Int
        get() = R.layout.item_tv_tag

    override fun bind(data: String) {
        title?.text = data
    }
}
