package knf.kuma.tv.anime

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import knf.kuma.database.CacheDB
import knf.kuma.pojos.RecentObject
import knf.kuma.tv.cards.RecentsCardView
import knf.kuma.tv.details.TVAnimesDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecentsPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(RecentsCardView(parent.context))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        (viewHolder.view as RecentsCardView).bind(item as RecentObject)
        viewHolder.view.setOnLongClickListener { v ->
            GlobalScope.launch(Dispatchers.Main){
                val animeObject = withContext(Dispatchers.IO) { CacheDB.INSTANCE.animeDAO().getByAid(item.aid) }
                animeObject?.let { TVAnimesDetails.start(v.context, it.link) }
            }
            true
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {

    }
}
