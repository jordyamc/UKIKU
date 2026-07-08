package knf.kuma.tv.directory

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import knf.kuma.pojos.av1.DirectoryAV1Calendar
import knf.kuma.tv.cards.DirCardView

class CalendarPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(DirCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        if (item == null) return
        (viewHolder.view as DirCardView).bind((item as DirectoryAV1Calendar).asMin())
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
