package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import knf.kuma.R
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.load
import knf.kuma.directory.DirObject
import knf.kuma.tv.BindableCardView
import kotlinx.android.synthetic.main.item_tv_card_adv.view.*

class DirAdvCardView(context: Context) : BindableCardView<DirObject>(context) {

    override val imageView: ImageView
        get() = img
    override val layoutResource: Int
        get() = R.layout.item_tv_card_adv

    override fun bind(data: DirObject) {
        imageView.load(PatternUtil.getCover(data.aid))
        title?.text = data.name
        rating?.text = "\u2605${data.rate_stars ?: "?.?"}"
        type?.text = data.type
    }
}
