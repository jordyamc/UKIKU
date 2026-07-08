package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import knf.kuma.R
import knf.kuma.commons.loadGlide
import knf.kuma.pojos.av1.DirectoryAV1Min
import knf.kuma.tv.BindableCardView
import org.jetbrains.anko.find

class DirCardView(context: Context) : BindableCardView<DirectoryAV1Min>(context) {

    override val imageView: ImageView
        get() = find(R.id.img)
    override val layoutResource: Int
        get() = R.layout.item_tv_card

    override fun bind(data: DirectoryAV1Min) {
        imageView.loadGlide(data.imageUrl)
        find<TextView>(R.id.title).text = data.name
    }
}
