package knf.kuma.tv.search

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import knf.kuma.tv.cards.TagCardView

class TagPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        return Presenter.ViewHolder(TagCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        (viewHolder.view as TagCardView).bind(item as String)
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {

    }
}
