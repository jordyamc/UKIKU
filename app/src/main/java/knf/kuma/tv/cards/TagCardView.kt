package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import knf.kuma.R
import knf.kuma.pojos.av1.Genre
import knf.kuma.tv.BindableCardView
import org.jetbrains.anko.find

class TagCardView(context: Context) : BindableCardView<Genre>(context) {

    override val imageView: ImageView
        get() = find(R.id.img)
    override val layoutResource: Int
        get() = R.layout.item_tv_tag

    override fun bind(data: Genre) {
        find<TextView>(R.id.title).text = data.name
    }
}
