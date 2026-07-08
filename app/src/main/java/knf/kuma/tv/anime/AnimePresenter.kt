package knf.kuma.tv.anime

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import knf.kuma.pojos.av1.DirectoryAV1Min
import knf.kuma.tv.cards.AnimeCardView

class AnimePresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(AnimeCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        if (item == null) return
        (viewHolder.view as AnimeCardView).bind(item as DirectoryAV1Min)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
