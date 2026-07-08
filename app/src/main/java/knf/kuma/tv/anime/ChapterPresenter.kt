package knf.kuma.tv.anime

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import androidx.lifecycle.LifecycleCoroutineScope
import knf.kuma.pojos.av1.ChapterWID
import knf.kuma.tv.cards.ChapterCardView

class ChapterPresenter(val scope: LifecycleCoroutineScope) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(ChapterCardView(parent.context, scope))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        if (item == null) return
        (viewHolder.view as ChapterCardView).bind(item as ChapterWID)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        (viewHolder.view as ChapterCardView).unbind()
    }
}
