package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import knf.kuma.R
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.load
import knf.kuma.pojos.RecentObject
import knf.kuma.tv.BindableCardView
import kotlinx.android.synthetic.main.item_tv_card_chapter.view.*

class RecentsCardView(context: Context) : BindableCardView<RecentObject>(context) {

    override val imageView: ImageView
        get() = img
    override val layoutResource: Int
        get() = R.layout.item_tv_card_chapter

    override fun bind(data: RecentObject) {
        imageView.load(PatternUtil.getCover(data.aid))
        title?.text = data.name
        chapter?.text = data.chapter
    }
}
