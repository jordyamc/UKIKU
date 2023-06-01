package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import knf.kuma.R
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.load
import knf.kuma.pojos.FavoriteObject
import knf.kuma.tv.BindableCardView
import org.jetbrains.anko.find

class FavCardView(context: Context) : BindableCardView<FavoriteObject>(context) {

    override val imageView: ImageView
        get() = find(R.id.img)
    override val layoutResource: Int
        get() = R.layout.item_tv_card

    override fun bind(data: FavoriteObject) {
        imageView.load(PatternUtil.getCover(data.aid))
        find<TextView>(R.id.title).text = data.name
    }
}
