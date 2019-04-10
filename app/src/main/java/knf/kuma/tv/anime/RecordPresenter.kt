package knf.kuma.tv.anime

import android.view.ViewGroup

import androidx.leanback.widget.Presenter
import knf.kuma.pojos.RecordObject
import knf.kuma.tv.cards.RecordCardView

class RecordPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(RecordCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        (viewHolder.view as RecordCardView).bind(item as RecordObject)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
