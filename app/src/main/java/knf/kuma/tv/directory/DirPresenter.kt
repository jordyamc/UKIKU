package knf.kuma.tv.directory

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import knf.kuma.directory.DirObject
import knf.kuma.tv.cards.DirCardView

class DirPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(DirCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        if (item == null) return
        (viewHolder.view as DirCardView).bind(item as DirObject)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
