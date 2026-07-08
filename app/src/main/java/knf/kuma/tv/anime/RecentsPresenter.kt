package knf.kuma.tv.anime

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import knf.kuma.pojos.av1.RecentAV1
import knf.kuma.tv.cards.RecentsCardView
import knf.kuma.tv.details.TVAnimesDetails

class RecentsPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(RecentsCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        if (item == null || item !is RecentAV1) return
        (viewHolder.view as RecentsCardView).bind(item)
        viewHolder.view.setOnLongClickListener { v ->
            TVAnimesDetails.start(v.context, item.animeUrl)
            true
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
