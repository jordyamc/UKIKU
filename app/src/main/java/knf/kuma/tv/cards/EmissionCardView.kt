package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import knf.kuma.R
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.load
import knf.kuma.search.SearchObject
import knf.kuma.tv.BindableCardView
import kotlinx.android.synthetic.main.item_tv_card.view.*

class EmissionCardView(context: Context) : BindableCardView<SearchObject>(context) {

    override val imageView: ImageView
        get() = img
    override val layoutResource: Int
        get() = R.layout.item_tv_card

    override fun bind(data: SearchObject) {
        imageView.load(PatternUtil.getCover(data.aid))
        title?.text = data.name
    }
}
