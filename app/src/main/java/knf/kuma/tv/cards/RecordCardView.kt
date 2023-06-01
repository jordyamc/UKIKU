package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import knf.kuma.R
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.load
import knf.kuma.pojos.RecordObject
import knf.kuma.tv.BindableCardView
import org.jetbrains.anko.find

class RecordCardView(context: Context) : BindableCardView<RecordObject>(context) {

    override val imageView: ImageView
        get() = find(R.id.img)
    override val layoutResource: Int
        get() = R.layout.item_tv_card_chapter

    override fun bind(data: RecordObject) {
        imageView.load(PatternUtil.getCover(data.aid))
        find<TextView>(R.id.title).text = data.name
        find<TextView>(R.id.chapter).text = data.chapter
    }
}
