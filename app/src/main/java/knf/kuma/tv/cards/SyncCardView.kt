package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import knf.kuma.R
import knf.kuma.commons.PicassoSingle
import knf.kuma.tv.BindableCardView
import knf.kuma.tv.sync.SyncObject
import kotlinx.android.synthetic.main.item_tv_card_sync.view.*

class SyncCardView(context: Context) : BindableCardView<SyncObject>(context) {

    override val layoutResource: Int
        get() = R.layout.item_tv_card_sync

    override val imageView: ImageView
        get() = img

    override fun bind(data: SyncObject) {
        PicassoSingle.get().load(data.image).into(img)
        title.text = data.title
    }
}
