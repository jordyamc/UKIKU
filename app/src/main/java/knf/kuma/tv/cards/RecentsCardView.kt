package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import knf.kuma.R
import knf.kuma.commons.loadGlide
import knf.kuma.pojos.av1.RecentAV1
import knf.kuma.tv.BindableCardView
import org.jetbrains.anko.find

class RecentsCardView(context: Context) : BindableCardView<RecentAV1>(context) {

    override val imageView: ImageView
        get() = find(R.id.img)
    override val layoutResource: Int
        get() = R.layout.item_tv_card_chapter

    override fun bind(data: RecentAV1) {
        imageView.loadGlide(data.animeImageUrl)
        find<TextView>(R.id.title).text = data.name
        find<TextView>(R.id.chapter).text = data.chapter
    }
}
