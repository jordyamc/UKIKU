package knf.kuma.tv.anime

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import knf.kuma.search.SearchObject
import knf.kuma.tv.cards.EmissionCardView

class EmissionPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(EmissionCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        if (item == null) return
        (viewHolder.view as EmissionCardView).bind(item as SearchObject)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
