package knf.kuma.tv.search

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import knf.kuma.tv.cards.TagCardView

class TagPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(TagCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        if (item == null) return
        (viewHolder.view as TagCardView).bind(item as String)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
