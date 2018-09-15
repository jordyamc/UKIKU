package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import knf.kuma.R
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PicassoSingle
import knf.kuma.pojos.AnimeObject
import knf.kuma.tv.BindableCardView
import kotlinx.android.synthetic.main.item_tv_card.view.*

class AnimeCardView(context: Context) : BindableCardView<AnimeObject>(context) {

    override val imageView: ImageView
        get() = img
    override val layoutResource: Int
        get() = R.layout.item_tv_card

    override fun bind(data: AnimeObject) {
        PicassoSingle[context].load(PatternUtil.getCover(data.aid!!)).into(imageView)
        title!!.text = data.name
    }
}
