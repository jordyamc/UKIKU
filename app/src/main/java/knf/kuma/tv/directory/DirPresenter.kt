package knf.kuma.tv.directory

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import knf.kuma.pojos.av1.DirectoryAV1Min
import knf.kuma.tv.cards.DirCardView

class DirPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(DirCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        if (item == null) return
        (viewHolder.view as DirCardView).bind(item as DirectoryAV1Min)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
