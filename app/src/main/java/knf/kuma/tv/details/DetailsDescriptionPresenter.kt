package knf.kuma.tv.details

import androidx.annotation.ColorInt
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import knf.kuma.pojos.AnimeObject

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

    override fun onBindDescription(viewHolder: AbstractDetailsDescriptionPresenter.ViewHolder, itemData: Any) {
        val animeObject = itemData as AnimeObject
        viewHolder.title.text = animeObject.name
        viewHolder.subtitle.text = animeObject.genresString
        viewHolder.body.text = animeObject.description
        if (titleColor != 0)
            viewHolder.title.setTextColor(titleColor)
        if (bodyColor != 0) {
            viewHolder.subtitle.setTextColor(bodyColor)
            viewHolder.body.setTextColor(bodyColor)
        }
    }
}
