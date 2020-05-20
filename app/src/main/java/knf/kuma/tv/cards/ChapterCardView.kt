package knf.kuma.tv.cards

import android.content.Context
import android.view.View
import android.widget.ImageView
import knf.kuma.R
import knf.kuma.commons.PicassoSingle
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.tv.BindableCardView
import kotlinx.android.synthetic.main.item_tv_card_chapter_preview.view.*

class ChapterCardView(context: Context) : BindableCardView<AnimeObject.WebInfo.AnimeChapter>(context) {

    override val imageView: ImageView
        get() = img
    override val layoutResource: Int
        get() = R.layout.item_tv_card_chapter_preview

    override fun bind(data: AnimeObject.WebInfo.AnimeChapter) {
        PicassoSingle.get().load(data.img).into(imageView)
        indicator?.visibility = if (CacheDB.INSTANCE.seenDAO().chapterIsSeen(data.aid, data.number)) View.VISIBLE else View.GONE
        chapter?.text = data.number
    }
}
