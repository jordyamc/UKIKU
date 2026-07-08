package knf.kuma.tv.details

import androidx.annotation.ColorInt
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import knf.kuma.pojos.av1.DirectoryAV1

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter {
    @ColorInt
    private var titleColor: Int = 0
    @ColorInt
    private var bodyColor: Int = 0

    internal constructor(titleColor: Int, bodyColor: Int) {
        this.titleColor = titleColor
        this.bodyColor = bodyColor
    }

    internal constructor() {
        this.titleColor = 0
        this.bodyColor = 0
    }

    override fun onBindDescription(viewHolder: ViewHolder, itemData: Any) {
        val animeObject = itemData as DirectoryAV1
        viewHolder.title.text = animeObject.name
        viewHolder.subtitle.text = animeObject.genres.joinToString { it.name }.trim()
        viewHolder.body.text = animeObject.description
        if (titleColor != 0)
            viewHolder.title.setTextColor(titleColor)
        if (bodyColor != 0) {
            viewHolder.subtitle.setTextColor(bodyColor)
            viewHolder.body.setTextColor(bodyColor)
        }
    }
}
