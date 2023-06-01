package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import knf.kuma.R
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.load
import knf.kuma.pojos.RecentObject
import knf.kuma.tv.BindableCardView
import org.jetbrains.anko.find

class RecentsCardView(context: Context) : BindableCardView<RecentObject>(context) {

    override val imageView: ImageView
        get() = find(R.id.img)
    override val layoutResource: Int
        get() = R.layout.item_tv_card_chapter

    override fun bind(data: RecentObject) {
        imageView.load(PatternUtil.getCover(data.aid))
        find<TextView>(R.id.title).text = data.name
        find<TextView>(R.id.chapter).text = data.chapter
    }
}
