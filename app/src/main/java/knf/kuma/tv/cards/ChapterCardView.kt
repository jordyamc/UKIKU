package knf.kuma.tv.cards

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import knf.kuma.R
import knf.kuma.commons.loadGlide
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.ChapterWID
import knf.kuma.tv.BindableCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.find

class ChapterCardView(context: Context, val scope: LifecycleCoroutineScope) : BindableCardView<ChapterWID>(context) {

    override val imageView: ImageView
        get() = find(R.id.img)
    override val layoutResource: Int
        get() = R.layout.item_tv_card_chapter_preview

    var job: Job? = null

    override fun bind(data: ChapterWID) {
        imageView.loadGlide(data.thumbnail)
        job?.cancel()
        job = scope.launch {
            CacheDB.INSTANCE.recordAV1DAO().chapterIsSeenFlow(data.aid, data.number).collectLatest {
                withContext(Dispatchers.Main) {
                    find<View>(R.id.indicator).visibility = if (it) VISIBLE else GONE
                }
            }
        }
        find<TextView>(R.id.chapter).text = data.episodeName
    }
}
