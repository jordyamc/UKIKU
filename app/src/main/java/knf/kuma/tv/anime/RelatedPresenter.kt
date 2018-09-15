package knf.kuma.tv.anime

import android.view.ViewGroup

import androidx.leanback.widget.Presenter
import knf.kuma.pojos.AnimeObject
import knf.kuma.tv.cards.RelatedCardView

class RelatedPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        return Presenter.ViewHolder(RelatedCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        (viewHolder.view as RelatedCardView).bind(item as AnimeObject.WebInfo.AnimeRelated)
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {

    }
}
