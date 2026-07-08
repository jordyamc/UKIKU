package knf.kuma.tv.directory

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import knf.kuma.pojos.av1.Recommended
import knf.kuma.tv.cards.RecommendedCardView

class RecommendedPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(RecommendedCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        if (item == null) return
        (viewHolder.view as RecommendedCardView).bind(item as Recommended)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
