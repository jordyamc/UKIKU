package knf.kuma.tv.anime

import android.view.ViewGroup

import androidx.leanback.widget.Presenter
import knf.kuma.tv.cards.SyncCardView
import knf.kuma.tv.sync.SyncObject

class SyncPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(SyncCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        (item as? SyncObject)?.let {
            (viewHolder.view as? SyncCardView)?.bind(it)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
