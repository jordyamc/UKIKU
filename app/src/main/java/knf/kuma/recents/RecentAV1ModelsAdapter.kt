package knf.kuma.recents

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.CircularProgressIndicator
import knf.kuma.R
import knf.kuma.backup.firestore.syncData
import knf.kuma.cast.CastMedia
import knf.kuma.commons.CastUtil
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.inflate
import knf.kuma.commons.isFullMode
import knf.kuma.commons.isVisibleAnimate
import knf.kuma.commons.load
import knf.kuma.commons.onClickMenu
import knf.kuma.commons.safeShow
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadManagerCentral
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.av1.RecentAV1
import knf.kuma.pojos.av1.menuHideList
import knf.kuma.pojos.av1.openInfo
import knf.kuma.pojos.av1.toggleSeen
import knf.kuma.queue.QueueManager
import knf.kuma.videoservers.FileActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.Locale

class RecentAV1ModelsAdapter(private val fragment: Fragment) : ListAdapter<RecentAV1, RecentAV1ModelsAdapter.ModelsViewHolder>(
    RecentAV1.DIFF) {

    private val lifecycleScope = fragment.lifecycleScope
    private val recordsDAO by lazy { CacheDB.INSTANCE.recordAV1DAO() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelsViewHolder =
        ModelsViewHolder(fragment.viewLifecycleOwner, parent.inflate(R.layout.item_recents_material))

    override fun onBindViewHolder(holder:ModelsViewHolder, position: Int) {
        val item = getItem(position)
        holder.apply {
            image.load(item.animeImageUrl)
            chapter.text = item.chapter
            name.text = item.name
            newIndicator.isVisible = item.state.isNew && !item.state.isFavorite
            seenIndicator.isVisible = item.state.isSeen
            favIndicator.isVisible = item.state.isFavorite
            setUp(item, fragment)
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
                                        FileAccessHelper.deletePath(item.getFilePath(), false)
                                        item.state.isDeleting = false
                                        item.state.checkIsDownloaded
                                        DownloadManagerCentral.cancel(item.eid.toString())
                                        QueueManager.remove(item.eid.toString())
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
                            item.nameChapter,
                            item.state.fileWrapper.name()
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
                item.toggleSeen(lifecycleScope, recordsDAO)
                true
            }
        }
    }

    private fun setAsSeen(item: RecentAV1) {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            recordsDAO.addChapter(item.asRecord())
            syncData {
                history()
            }
        }
    }

    override fun onViewRecycled(holder: ModelsViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }

    fun updateList(list: List<RecentAV1>, callback: () -> Unit = {}) {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            launch(Dispatchers.Main) {
                submitList(list)
                callback()
            }
        }
    }

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

        private lateinit var state: RecentAV1.RecentState
        private var checkJob: Job? = null
        private var observersJob: Job? = null
        private val favoriteObserver: suspend (Boolean) -> Unit =  {
            if (state.isFavorite != it) {
                state.isFavorite = it
                favIndicator.isVisibleAnimate = it
                newIndicator.isVisible = !it && state.isFavorite
            }
        }
        private val seenObserver: suspend (Boolean) -> Unit = {
            if (state.isSeen != it) {
                state.isSeen = it
                seenIndicator.isVisibleAnimate = it
            }
        }
        private val downloadObserver: suspend (DownloadObject?) -> Unit =  {
            if (state.downloadObject != it) {
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
        }

        fun setUp(item: RecentAV1, fragment: Fragment) {
            this.state = item.state
            this.observersJob = fragment.viewLifecycleOwner.lifecycleScope.launch {
                launch {
                    state.favoriteFlow.collectLatest(favoriteObserver)
                }
                launch {
                    state.seenFlow.collectLatest(seenObserver)
                }
                launch {
                    state.downloadFlow.collectLatest(downloadObserver)
                }
            }
            setUpDownloadIndicators(item)
        }

        private fun setUpDownloadIndicators(item: RecentAV1) {
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
                            FileAccessHelper.deletePath(item.getFilePath(), true)
                            DownloadManagerCentral.cancel(item.eid.toString())
                            QueueManager.remove(item.eid.toString())
                        }
                    }
                    negativeButton(text = "abortar")
                }
            }
        }

        fun recycle() {
            observersJob?.cancel()
            checkJob?.cancel()
            if (!::state.isInitialized) return
        }
    }
}