package knf.kuma.recents

//import com.google.android.gms.ads.nativead.NativeAdView
import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.chip.Chip
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import knf.kuma.R
import knf.kuma.backup.firestore.syncData
import knf.kuma.cast.CastMedia
import knf.kuma.commons.CastUtil
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.inflate
import knf.kuma.commons.isFullMode
import knf.kuma.commons.isVisibleAnimate
import knf.kuma.commons.load
import knf.kuma.commons.onClickMenu
import knf.kuma.commons.safeShow
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadManager
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.RecordObject
import knf.kuma.pojos.SeenObject
import knf.kuma.queue.QueueManager
import knf.kuma.videoservers.FileActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.Locale

class RecentModelsAdapter(private val fragment: Fragment) : ListAdapter<RecentModel, ViewHolder>(RecentModel.DIFF) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            if (viewType == 0)
                AdsViewHolder(fragment.lifecycleScope, parent.inflate(R.layout.item_ad_recents_material))
            else
                ModelsViewHolder(fragment.viewLifecycleOwner, parent.inflate(R.layout.item_recents_material))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
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
                if (isFullMode)
                    actionMenu.onClickMenu(R.menu.menu_download_info, true, { item.menuHideList }) {
                        when (it.itemId) {
                            R.id.download -> {
                                FileActions.download(
                                    fragment.requireContext(),
                                    fragment.viewLifecycleOwner,
                                    item,
                                    fragment.view
                                ) { state, _ ->
                                    if (state == FileActions.CallbackState.START_DOWNLOAD)
                                        item.state.isDownloaded = true
                                }
                            }
                            R.id.streaming -> {
                                FileActions.stream(
                                    fragment.requireContext(),
                                    fragment.viewLifecycleOwner,
                                    item,
                                    fragment.view
                                ) { state, extra ->
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
                                    message(text = "¿Eliminar el ${item.chapter.lowercase(Locale.ENGLISH)} de ${item.name}?")
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
                else
                    actionMenu.isVisible = false
                root.setOnClickListener {
                    if (!isFullMode) {
                        item.openInfo(fragment.requireContext())
                        return@setOnClickListener
                    }
                    if (item.state.isDownloaded) {
                        if (CastUtil.get().connected())
                            CastUtil.get().play(fragment.requireView(), CastMedia.create(item))
                        else
                            FileActions.startPlay(
                                fragment.requireContext(),
                                item.extras.chapterTitle,
                                item.extras.fileWrapper.name()
                            )
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

    override fun onViewRecycled(holder: ViewHolder) {
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

    class ModelsViewHolder(private val lifecycleOwner: LifecycleOwner, view: View) : ViewHolder(view) {
        val root: View = itemView.find(R.id.root)
        val image: ImageView = itemView.find(R.id.image)
        val chapter: TextView = itemView.find(R.id.chapter)
        val name: TextView = itemView.find(R.id.name)
        val newIndicator: ImageView = itemView.find(R.id.newIndicator)
        val seenIndicator: View = itemView.find(R.id.seenIndicator)
        val favIndicator: ImageView = itemView.find(R.id.favIndicator)
        val actionMenu: View = itemView.find(R.id.actionMenu)
        val downloadedChip: Chip = itemView.find(R.id.downloadedChip)
        private val layDownloading: View = itemView.find(R.id.layDownloading)
        private val progressIndicator: CircularProgressIndicator = itemView.find(R.id.progressIndicator)
        private val actionCancel: View = itemView.find(R.id.actionCancel)

        private lateinit var state: RecentState
        private var checkJob: Job? = null
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
            checkJob = GlobalScope.launch(Dispatchers.Main) {
                layDownloading.isVisible = false
                downloadedChip.isVisible = false
                when {
                    state.downloadObject?.isDownloadingOrPaused == true -> {
                        if (!isActive) return@launch
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
                    withContext(Dispatchers.IO) { state.isDownloaded } -> {
                        if (!isActive) return@launch
                        layDownloading.isVisible = false
                        downloadedChip.isVisible = true
                    }
                    else -> {
                        if (!isActive) return@launch
                        layDownloading.isVisible = false
                        downloadedChip.isVisible = false
                    }
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
            checkJob?.cancel()
            if (!::state.isInitialized) return
            state.favoriteLive.removeObserver(favoriteObserver)
            state.seenLive.removeObserver(seenObserver)
            state.downloadLive.removeObserver(downloadObserver)
        }
    }

    class AdsDealsViewHolder(private val scope: CoroutineScope, view: View): ViewHolder(view)

    class AdsViewHolder(private val scope: CoroutineScope, view: View) : ViewHolder(view) {
        //private val nativeAdView: NativeAdView = itemView.find(R.id.nativeAdView)
        private val iconV: ShapeableImageView = itemView.find(R.id.icon)
        /*private val primary: TextView = itemView.find(R.id.primary)
        private val secondary: TextView = itemView.find(R.id.secondary)
        private val cta: Chip = itemView.find(R.id.cta)*/

        /*init {
            nativeAdView.apply {
                iconView = iconV
                headlineView = primary
                bodyView = secondary
                callToActionView = cta
            }
        }*/

        @SuppressLint("DefaultLocale")
        fun setAd(modelAd: RecentModelAd) {
            /*modelAd.unifiedNativeAd.apply {
                scope.launch(Dispatchers.Main) {
                    if (icon == null)
                        iconV.setImageDrawable(
                            ColorDrawable(
                                ContextCompat.getColor(
                                    App.context,
                                    EAHelper.getThemeColorLight()
                                )
                            )
                        )
                    else {
                        iconV.setImageDrawable(icon!!.drawable)
                    }
                    primary.text = headline
                    secondary.text = body
                    if (callToAction != null) {
                        cta.text = callToAction!!.lowercase(Locale.getDefault())
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    }
                    nativeAdView.setNativeAd(modelAd.unifiedNativeAd)
                }
            }*/
        }
    }
}