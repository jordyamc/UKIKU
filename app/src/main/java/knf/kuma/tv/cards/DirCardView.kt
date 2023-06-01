package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import knf.kuma.R
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.load
import knf.kuma.directory.DirObject
import knf.kuma.tv.BindableCardView
import org.jetbrains.anko.find

class DirCardView(context: Context) : BindableCardView<DirObject>(context) {

    override val imageView: ImageView
        get() = find(R.id.img)
    override val layoutResource: Int
        get() = R.layout.item_tv_card_rate

    override fun bind(data: DirObject) {
        imageView.load(PatternUtil.getCover(data.aid))
        find<TextView>(R.id.title).text = data.name
        find<TextView>(R.id.rating).text = "\u2605${data.rate_stars ?: "?.?"}"
    }
}
