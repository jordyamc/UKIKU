package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import knf.kuma.R
import knf.kuma.commons.doOnUI
import knf.kuma.tv.BindableCardView
import knf.kuma.tv.sections.SectionObject
import kotlinx.android.synthetic.main.item_tv_card_section.view.*

class SectionCardView(context: Context) : BindableCardView<SectionObject>(context) {

    override val layoutResource: Int
        get() = R.layout.item_tv_card_section

    override val imageView: ImageView
        get() = img

    override fun bind(data: SectionObject) {
        doOnUI { imageView.setImageResource(data.image) }
        title.text = data.title
    }
}
