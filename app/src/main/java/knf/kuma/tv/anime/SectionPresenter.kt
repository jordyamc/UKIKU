package knf.kuma.tv.anime

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import knf.kuma.tv.cards.SectionCardView
import knf.kuma.tv.sections.SectionObject

class SectionPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(SectionCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        (item as? SectionObject)?.let {
            (viewHolder.view as? SectionCardView)?.bind(it)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
