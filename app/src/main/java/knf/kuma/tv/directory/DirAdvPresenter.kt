package knf.kuma.tv.directory

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import knf.kuma.directory.DirObject
import knf.kuma.tv.cards.DirAdvCardView

class DirAdvPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(DirAdvCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        if (item == null) return
        (viewHolder.view as DirAdvCardView).bind(item as DirObject)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
