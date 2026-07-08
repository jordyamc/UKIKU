package knf.kuma.tv.anime

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import knf.kuma.pojos.av1.FavoriteAV1
import knf.kuma.tv.cards.FavCardView

class FavPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(FavCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        if (item == null) return
        (viewHolder.view as FavCardView).bind(item as FavoriteAV1)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
