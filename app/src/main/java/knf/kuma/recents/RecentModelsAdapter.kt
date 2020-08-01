package knf.kuma.recents

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.ProgressIndicator
import knf.kuma.R
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.pojos.DownloadObject
import kotlinx.android.synthetic.main.item_recents_material.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecentModelsAdapter(private val fragment: Fragment) : ListAdapter<RecentModel, RecentModelsAdapter.ModelsViewHolder>(RecentModel.DIFF) {

    private val lifecycleScope = fragment.lifecycleScope
    private val animeDAO by lazy { CacheDB.INSTANCE.animeDAO() }
    private val chaptersDAO by lazy { CacheDB.INSTANCE.seenDAO() }
    private val recordsDAO by lazy { CacheDB.INSTANCE.recordsDAO() }
    private val downloadsDAO by lazy { CacheDB.INSTANCE.downloadsDAO() }

    override fun getItemId(position: Int): Long = getItem(position).hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelsViewHolder =
            ModelsViewHolder(fragment.viewLifecycleOwner, parent.inflate(R.layout.item_recents_material))

    override fun onBindViewHolder(holder: ModelsViewHolder, position: Int) {
        val item = getItem(position)
        holder.apply {
            image.load(PatternUtil.getCover(item.aid))
            chapter.text = item.chapter
            name.text = item.name
            Log.e(item.aid, "isFav: ${item.state.isFavorite}")
            newIndicator.isVisible = item.extras.isNewChapter
            seenIndicator.isVisible = item.state.isSeen
            favIndicator.isVisible = item.state.isFavorite
            downloadedChip.isVisible = item.state.isDownloaded
            setUp(item.state)
            root.setOnLongClickListener {
                item.toggleSeen(lifecycleScope, chaptersDAO)
                true
            }
        }
    }

    override fun onViewRecycled(holder: ModelsViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }

    fun updateList(list: List<RecentModel>, callback: () -> Unit) {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            list.forEach { it.prepare() }
            launch(Dispatchers.Main) {
                submitList(list)
                callback()
            }
        }
    }

    class ModelsViewHolder(private val lifecycleOwner: LifecycleOwner, view: View) : RecyclerView.ViewHolder(view) {
        val root: View = itemView.root
        val image: ImageView = itemView.image
        val chapter: TextView = itemView.chapter
        val name: TextView = itemView.name
        val newIndicator: ImageView = itemView.newIndicator
        val seenIndicator: View = itemView.seenIndicator
        val favIndicator: ImageView = itemView.favIndicator
        val downloadedChip: Chip = itemView.downloadedChip
        private val layDownloading: View = itemView.layDownloading
        private val progressIndicator: ProgressIndicator = itemView.progressIndicator
        val actionCancel: View = itemView.actionCancel
        val actionMenu: View = itemView.actionMenu

        private lateinit var state: RecentState
        private val favoriteObserver = Observer<Boolean> {
            if (state.isFavorite == it) return@Observer
            state.isFavorite = it
            favIndicator.isVisibleAnimate = it
        }
        private val seenObserver = Observer<Int> {
            if (state.isSeen == it > 0) return@Observer
            state.isSeen = it > 0
            seenIndicator.isVisibleAnimate = it > 0
        }
        private val downloadObserver = Observer<DownloadObject?> {
            if (state.downloadObject == it) return@Observer
            state.downloadObject = it
            if (it != null && it.isDownloading) {
                layDownloading.isVisibleAnimate = true
                when (it.state) {
                    DownloadObject.DOWNLOADING, DownloadObject.PAUSED -> {
                        progressIndicator.isIndeterminate = false
                        if (it.getEta() == -2L || PrefsUtil.downloaderType == 0) {
                            progressIndicator.setProgressCompat(it.progress, true)
                            if (it.getEta() == -2L && PrefsUtil.downloaderType != 0)
                                progressIndicator.secondaryProgress = 100
                        } else {
                            progressIndicator.progress = 100
                            progressIndicator.secondaryProgress = it.progress
                        }
                    }
                    DownloadObject.PENDING -> {
                        progressIndicator.isIndeterminate = true
                    }
                }
            } else
                layDownloading.isVisibleAnimate = false
        }

        fun setUp(state: RecentState) {
            this.state = state
            state.favoriteLive.observe(lifecycleOwner, favoriteObserver)
            state.seenLive.observe(lifecycleOwner, seenObserver)
            state.downloadLive.observe(lifecycleOwner, downloadObserver)
        }

        fun recycle() {
            if (!::state.isInitialized) return
            state.favoriteLive.removeObserver(favoriteObserver)
            state.seenLive.removeObserver(seenObserver)
            state.downloadLive.removeObserver(downloadObserver)
        }
    }
}