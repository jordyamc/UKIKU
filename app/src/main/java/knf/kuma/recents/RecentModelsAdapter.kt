package knf.kuma.recents

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.gms.ads.formats.UnifiedNativeAdView
import com.google.android.material.chip.Chip
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.ProgressIndicator
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.backup.firestore.syncData
import knf.kuma.cast.CastMedia
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadManager
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.RecordObject
import knf.kuma.pojos.SeenObject
import knf.kuma.queue.QueueManager
import knf.kuma.videoservers.FileActions
import kotlinx.android.synthetic.main.item_ad_recents_material.view.*
import kotlinx.android.synthetic.main.item_recents_material.view.*
import kotlinx.coroutines.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.*

class RecentModelsAdapter(private val fragment: Fragment) : ListAdapter<RecentModel, RecyclerView.ViewHolder>(RecentModel.DIFF) {

    private val lifecycleScope = fragment.lifecycleScope
    private val chaptersDAO by lazy { CacheDB.INSTANCE.seenDAO() }
    private val recordsDAO by lazy { CacheDB.INSTANCE.recordsDAO() }
    private var adsList = emptyList<RecentModelAd>()

    override fun getItemId(position: Int): Long = getItem(position).let {
        if (it is RecentModelAd)
            it.id.toLong()
        else
            it.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int = getItem(position).let {
        if (it is RecentModelAd)
            0
        else
            1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            if (viewType == 0)
                AdsViewHolder(fragment.lifecycleScope, parent.inflate(R.layout.item_ad_recents_material))
            else
                ModelsViewHolder(fragment.viewLifecycleOwner, parent.inflate(R.layout.item_recents_material))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is ModelsViewHolder) {
            holder.apply {
                image.load(PatternUtil.getCover(item.aid))
                chapter.text = item.chapter
                name.text = item.name
                newIndicator.isVisible = item.extras.isNewChapter && !item.state.isFavorite
                seenIndicator.isVisible = item.state.isSeen
                favIndicator.isVisible = item.state.isFavorite
                setUp(item)
                if (BuildConfig.BUILD_TYPE != "playstore")
                    actionMenu.onClickMenu(R.menu.menu_download_info, true, { item.menuHideList }) {
                        when (it.itemId) {
                            R.id.download -> {
                                FileActions.download(fragment.requireContext(), fragment.viewLifecycleOwner, item, fragment.view) { state, _ ->
                                    if (state == FileActions.CallbackState.START_DOWNLOAD)
                                        item.state.isDownloaded = true
                                }
                            }
                            R.id.streaming -> {
                                FileActions.stream(fragment.requireContext(), fragment.viewLifecycleOwner, item, fragment.view) { state, extra ->
                                    when (state) {
                                        FileActions.CallbackState.START_STREAM -> {
                                            setAsSeen(item)
                                        }
                                        FileActions.CallbackState.START_CAST -> {
                                            CastUtil.get().play(fragment.requireView(), CastMedia.create(item, extra as? String))
                                            setAsSeen(item)
                                        }
                                        else -> {
                                        }
                                    }
                                }
                            }
                            R.id.delete -> {
                                MaterialDialog(fragment.requireContext()).safeShow {
                                    lifecycleOwner(fragment.viewLifecycleOwner)
                                    message(text = "¿Eliminar el ${item.chapter.toLowerCase(Locale.ENGLISH)} de ${item.name}?")
                                    positiveButton(text = "CONFIRMAR") {
                                        GlobalScope.launch(Dispatchers.IO) {
                                            fragment.lifecycleScope.launch(Dispatchers.Main) {
                                                item.state.isDownloaded = false
                                                this@apply.downloadedChip.isVisibleAnimate = false
                                            }
                                            item.state.isDeleting = true
                                            FileAccessHelper.deletePath(item.extras.filePath, false)
                                            item.state.isDeleting = false
                                            item.state.checkIsDownloaded
                                            DownloadManager.cancel(item.extras.eid)
                                            QueueManager.remove(item.extras.eid)
                                        }
                                    }
                                    negativeButton(text = "CANCELAR")
                                }
                            }
                            R.id.info -> {
                                item.openInfo(fragment.requireContext())
                            }
                        }
                    }
                root.setOnClickListener {
                    if (BuildConfig.BUILD_TYPE == "playstore") {
                        item.openInfo(fragment.requireContext())
                        return@setOnClickListener
                    }
                    if (item.state.isDownloaded) {
                        if (CastUtil.get().connected())
                            CastUtil.get().play(fragment.requireView(), CastMedia.create(item))
                        else
                            FileActions.startPlay(fragment.requireContext(), item.extras.chapterTitle, item.extras.fileWrapper.name())
                        setAsSeen(item)
                    } else {
                        val callback: (FileActions.CallbackState, Any?) -> Unit = { state, extra ->
                            when (state) {
                                FileActions.CallbackState.START_STREAM -> {
                                    setAsSeen(item)
                                }
                                FileActions.CallbackState.START_CAST -> {
                                    CastUtil.get().play(fragment.requireView(), CastMedia.create(item, extra as? String))
                                    setAsSeen(item)
                                }
                                FileActions.CallbackState.START_DOWNLOAD -> {
                                    item.state.isDownloaded = true
                                }
                                else -> {
                                }
                            }
                        }
                        if (PrefsUtil.recentActionType == "0")
                            FileActions.stream(fragment.requireContext(), fragment.viewLifecycleOwner, item, fragment.view, callback)
                        else
                            FileActions.download(fragment.requireContext(), fragment.viewLifecycleOwner, item, fragment.view, callback)
                    }
                }
                root.setOnLongClickListener {
                    item.toggleSeen(lifecycleScope, chaptersDAO)
                    true
                }
            }
        } else if (holder is AdsViewHolder) {
            holder.setAd(item as RecentModelAd)
        }
    }

    private fun setAsSeen(item: RecentModel) {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            chaptersDAO.addChapter(SeenObject.fromRecentModel(item))
            recordsDAO.add(RecordObject.fromRecentModel(item))
            syncData {
                history()
                seen()
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ModelsViewHolder)
            holder.recycle()
    }

    fun updateList(list: List<RecentModel>, aList: List<RecentModelAd> = adsList, callback: () -> Unit = {}) {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            list.forEach { if (it !is RecentModelAd) it.prepare() }
            val fList = list.toMutableList()
            if (list.isNotEmpty() && aList.isNotEmpty()) {
                adsList = aList
                fList.add(0, adsList[0])
                if (adsList.size >= 2)
                    fList.add(8, adsList[1])
                if (adsList.size >= 3)
                    fList.add(16, adsList[2])
            }
            launch(Dispatchers.Main) {
                submitList(fList)
                callback()
            }
        }
    }

    fun hasAds(): Boolean = adsList.isNotEmpty()

    class ModelsViewHolder(private val lifecycleOwner: LifecycleOwner, view: View) : RecyclerView.ViewHolder(view) {
        val root: View = itemView.root
        val image: ImageView = itemView.image
        val chapter: TextView = itemView.chapter
        val name: TextView = itemView.name
        val newIndicator: ImageView = itemView.newIndicator
        val seenIndicator: View = itemView.seenIndicator
        val favIndicator: ImageView = itemView.favIndicator
        val actionMenu: View = itemView.actionMenu
        val downloadedChip: Chip = itemView.downloadedChip
        private val layDownloading: View = itemView.layDownloading
        private val progressIndicator: ProgressIndicator = itemView.progressIndicator
        private val actionCancel: View = itemView.actionCancel

        private lateinit var state: RecentState
        private val favoriteObserver = Observer<Boolean> {
            if (state.isFavorite == it) return@Observer
            state.isFavorite = it
            favIndicator.isVisibleAnimate = it
            newIndicator.isVisible = !it && state.isFavorite
        }
        private val seenObserver = Observer<Int> {
            if (state.isSeen == it > 0) return@Observer
            state.isSeen = it > 0
            seenIndicator.isVisibleAnimate = it > 0
        }
        private val downloadObserver = Observer<DownloadObject?> {
            if (state.downloadObject == it) return@Observer
            state.downloadObject = it
            if (it != null && it.isDownloadingOrPaused) {
                if (!layDownloading.isVisible)
                    layDownloading.isVisibleAnimate = true
                when (it.state) {
                    DownloadObject.DOWNLOADING, DownloadObject.PAUSED -> {
                        progressIndicator.isVisible = false
                        progressIndicator.isIndeterminate = false
                        progressIndicator.isVisible = true
                        if (it.getEta() == -2L || PrefsUtil.downloaderType == 0) {
                            var progress = it.progress
                            if (it.getEta() == -2L && PrefsUtil.downloaderType != 0) {
                                progressIndicator.max = 200
                                progress += 100
                            } else {
                                progressIndicator.max = 100
                            }
                            progressIndicator.setProgressCompat(progress, true)
                        } else {
                            progressIndicator.max = 200
                            progressIndicator.setProgressCompat(it.progress, true)
                        }
                    }
                    DownloadObject.PENDING -> {
                        progressIndicator.isVisible = false
                        progressIndicator.isIndeterminate = true
                        progressIndicator.isVisible = true
                    }
                }
            } else {
                lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    if (layDownloading.isVisible)
                        layDownloading.isVisibleAnimate = false
                    state.checkIsDownloaded
                    if (withContext(Dispatchers.IO) { state.canPlay }) {
                        if (!downloadedChip.isVisible)
                            downloadedChip.isVisibleAnimate = true
                    }
                }
            }
        }

        fun setUp(item: RecentModel) {
            this.state = item.state
            state.favoriteLive.observe(lifecycleOwner, favoriteObserver)
            state.seenLive.observe(lifecycleOwner, seenObserver)
            state.downloadLive.observe(lifecycleOwner, downloadObserver)
            setUpDownloadIndicators(item)
        }

        private fun setUpDownloadIndicators(item: RecentModel) {
            when {
                state.downloadObject?.isDownloadingOrPaused == true -> {
                    layDownloading.isVisible = true
                    downloadedChip.isVisible = false
                    state.downloadObject?.let {
                        when (it.state) {
                            DownloadObject.DOWNLOADING, DownloadObject.PAUSED -> {
                                progressIndicator.isVisible = false
                                progressIndicator.isIndeterminate = false
                                progressIndicator.isVisible = true
                                if (it.getEta() == -2L || PrefsUtil.downloaderType == 0) {
                                    var progress = it.progress
                                    if (it.getEta() == -2L && PrefsUtil.downloaderType != 0) {
                                        progressIndicator.max = 200
                                        progress += 100
                                    } else {
                                        progressIndicator.max = 100
                                    }
                                    progressIndicator.setProgressCompat(progress, true)
                                } else {
                                    progressIndicator.max = 200
                                    progressIndicator.setProgressCompat(it.progress, true)
                                }
                            }
                            DownloadObject.PENDING -> {
                                progressIndicator.isVisible = false
                                progressIndicator.isIndeterminate = true
                                progressIndicator.isVisible = true
                            }
                        }
                    }
                }
                state.isDownloaded -> {
                    layDownloading.isVisible = false
                    downloadedChip.isVisible = true
                }
                else -> {
                    layDownloading.isVisible = false
                    downloadedChip.isVisible = false
                }
            }
            actionCancel.onClick {
                MaterialDialog(itemView.context).safeShow {
                    lifecycleOwner(lifecycleOwner)
                    message(text = "¿Deseas cancelar esta descarga?")
                    positiveButton(text = "confirmar") {
                        item.state.isDownloaded = false
                        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            item.state.isDownloaded = false
                            FileAccessHelper.deletePath(item.extras.filePath, true)
                            DownloadManager.cancel(item.extras.eid)
                            QueueManager.remove(item.extras.eid)
                        }
                    }
                    negativeButton(text = "abortar")
                }
            }
        }

        fun recycle() {
            if (!::state.isInitialized) return
            state.favoriteLive.removeObserver(favoriteObserver)
            state.seenLive.removeObserver(seenObserver)
            state.downloadLive.removeObserver(downloadObserver)
        }
    }

    class AdsViewHolder(private val scope: CoroutineScope, view: View) : RecyclerView.ViewHolder(view) {
        private val nativeAdView: UnifiedNativeAdView = itemView.nativeAdView
        private val iconView: ShapeableImageView = itemView.icon
        private val primary: TextView = itemView.primary
        private val secondary: TextView = itemView.secondary
        private val cta: Chip = itemView.cta

        init {
            nativeAdView.apply {
                iconView = icon
                headlineView = primary
                bodyView = secondary
                callToActionView = cta
            }
        }

        @SuppressLint("DefaultLocale")
        fun setAd(modelAd: RecentModelAd) {
            modelAd.unifiedNativeAd.apply {
                scope.launch(Dispatchers.Main) {
                    if (icon == null)
                        iconView.setImageDrawable(ColorDrawable(ContextCompat.getColor(App.context, EAHelper.getThemeColorLight())))
                    else {
                        iconView.setImageDrawable(icon.drawable)
                    }
                    primary.text = headline
                    secondary.text = body
                    cta.text = callToAction.toLowerCase().capitalize()
                    nativeAdView.setNativeAd(modelAd.unifiedNativeAd)
                }
            }
        }
    }
}