package knf.kuma.recents

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.card.MaterialCardView
import knf.kuma.R
import knf.kuma.ads.AdCallback
import knf.kuma.ads.AdCardItemHolder
import knf.kuma.ads.AdsUtilsMob
import knf.kuma.ads.implAdsRecent
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.backup.firestore.syncData
import knf.kuma.cast.CastMedia
import knf.kuma.commons.CastUtil
import knf.kuma.commons.FileWrapper
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUI
import knf.kuma.commons.isFullMode
import knf.kuma.commons.load
import knf.kuma.commons.noCrash
import knf.kuma.commons.noCrashLet
import knf.kuma.commons.safeShow
import knf.kuma.custom.SeenAnimeOverlay
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadManagerCentral
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.av1.AdRecentAV1
import knf.kuma.pojos.av1.BaseRecentAV1
import knf.kuma.pojos.av1.RecentAV1
import knf.kuma.queue.QueueManager
import knf.kuma.videoservers.ServersFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import xdroid.toaster.Toaster.toast
import java.util.Locale

class RecentsAV1Adapter internal constructor(private val fragment: Fragment, private val view: View) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val context: Context? = fragment.context
    private var list: MutableList<BaseRecentAV1> = mutableListOf()
    private val dao = CacheDB.INSTANCE.favoriteAV1DAO()
    private val recordsDAO = CacheDB.INSTANCE.recordAV1DAO()
    private val downloadsDAO = CacheDB.INSTANCE.downloadsDAO()
    private var isNetworkAvailable: Boolean = Network.isConnected

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1)
            return AdCardItemHolder(parent).also {
                it.loadAd(fragment.lifecycleScope, object : AdCallback {
                    override fun getID(): String = AdsUtilsMob.RECENT_BANNER
                }, 500)
            }
        return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_recents, parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return noCrashLet { if (list[position] is AdRecentAV1) 1 else 0 } ?: 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        noCrash {
            if (context == null || list.isEmpty()) return@noCrash
            val item = noCrashLet { list[position] } ?: return@noCrash
            if (holder is ItemHolder && item is RecentAV1) {
                holder.unsetCastingObserver()
                holder.imageView.load(item.animeImageUrl)
                holder.setNew(item.state.isNew)
                holder.setFav(item.state.isFavorite)
                holder.setSeen(item.state.isSeen)
                holder.setState(isNetworkAvailable, item.state.isDownloading)
                holder.apply {
                    listenersJob?.cancel()
                    listenersJob = fragment.lifecycleScope.launch {
                        launch {
                            dao.favObserver(item.aid).distinctUntilChanged().collectLatest {
                                holder.setFav(it != null)
                                item.state.isFavorite = it != null
                            }
                        }
                        launch {
                            recordsDAO.chapterIsSeenFlow(item.aid, item.number).distinctUntilChanged().collectLatest {
                                holder.setSeen(it)
                                item.state.isSeen = it
                            }
                        }
                        launch {
                            if (!isActive)
                                return@launch
                            holder.setState(isNetworkAvailable, item.state.fileWrapper.exist || item.state.isDownloading)
                            launch {
                                downloadsDAO.getFlowByEid(item.eid.toString()).distinctUntilChanged().collectLatest {
                                    holder.setDownloadState(it)
                                    if (it == null) {
                                        item.state.downloadState = -8
                                        item.state.isDownloading = false
                                    } else {
                                        item.state.isDownloading = it.state == DownloadObject.DOWNLOADING || it.state == DownloadObject.PENDING || it.state == DownloadObject.PAUSED
                                        item.state.downloadState = it.state
                                        if (it.state == DownloadObject.DOWNLOADING || it.state == DownloadObject.PENDING)
                                            holder.downIcon.setImageResource(R.drawable.ic_download)
                                        else if (it.state == DownloadObject.PAUSED)
                                            holder.downIcon.setImageResource(R.drawable.ic_pause_normal)
                                        withContext(Dispatchers.IO) { item.state.fileWrapper.reset() }
                                    }
                                    holder.setState(isNetworkAvailable, item.state.fileWrapper.exist || item.state.isDownloading)
                                }
                            }
                            holder.setCastingObserver(fragment) { s ->
                                if (item.eid.toString() == s) {
                                    holder.setCasting(true, item.state.fileWrapper)
                                    holder.streaming.setOnClickListener {
                                        CastUtil.get().openControls()
                                    }
                                } else {
                                    holder.setCasting(false, item.state.fileWrapper)
                                    holder.streaming.setOnClickListener {
                                        if (item.state.fileWrapper.exist || item.state.isDownloading) {
                                            MaterialDialog(context).safeShow {
                                                message(
                                                    text = "¿Eliminar el ${
                                                        item.chapter.lowercase(
                                                            Locale.ENGLISH
                                                        )
                                                    } de ${item.name}?"
                                                )
                                                positiveButton(text = "CONFIRMAR") {
                                                    FileAccessHelper.deletePath(
                                                        item.getFilePath(),
                                                        true
                                                    )
                                                    DownloadManagerCentral.cancel(item.eid.toString())
                                                    QueueManager.remove(item.eid.toString())
                                                    item.state.fileWrapper.exist = false
                                                    holder.setState(isNetworkAvailable, false)
                                                }
                                                negativeButton(text = "CANCELAR")
                                            }
                                        } else {
                                            holder.setLocked(true)
                                            ServersFactory.start(
                                                context,
                                                item.chapterUrl,
                                                item.asDownload(),
                                                true,
                                                object : ServersFactory.ServersInterface {
                                                    override fun onFinish(
                                                        started: Boolean,
                                                        success: Boolean
                                                    ) {
                                                        if (!started && success) {
                                                            doAsync {
                                                                recordsDAO.addChapter(item.asRecord())
                                                                syncData {
                                                                    history()
                                                                }
                                                            }
                                                            item.state.isSeen = true
                                                        }
                                                        holder.setLocked(false)
                                                    }

                                                    override fun onCast(url: String?) {
                                                        CastUtil.get().play(
                                                            view,
                                                            CastMedia.create(item, url)
                                                        )
                                                        doAsync {
                                                            recordsDAO.addChapter(item.asRecord())
                                                            syncData {
                                                                history()
                                                            }
                                                        }
                                                        item.state.isSeen = true
                                                        holder.setSeen(true)
                                                        holder.setLocked(false)
                                                    }

                                                    override fun onProgressIndicator(boolean: Boolean) {
                                                        fragment.doOnUI {
                                                            if (boolean) {
                                                                holder.progressBar.isIndeterminate =
                                                                    true
                                                                holder.progressBarRoot.visibility =
                                                                    View.VISIBLE
                                                            } else
                                                                holder.progressBarRoot.visibility =
                                                                    View.GONE
                                                        }
                                                    }

                                                    override fun getView(): View {
                                                        return view
                                                    }
                                                })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                holder.title.text = item.name
                holder.chapter.text = item.chapter
                if (!isFullMode) holder.layButtons.visibility = View.INVISIBLE
                holder.cardView.setOnClickListener {
                    ActivityAnime.open(fragment, item, holder.imageView)
                }
                holder.cardView.setOnLongClickListener {
                    if (!item.state.isSeen) {
                        doAsync {
                            recordsDAO.addChapter(item.asRecord())
                        }
                        item.state.isSeen = true
                        holder.animeOverlay.setSeen(seen = true, animate = true)
                    } else {
                        doAsync {
                            recordsDAO.deleteChapter(item.aid, item.number)
                        }
                        item.state.isSeen = false
                        holder.animeOverlay.setSeen(seen = false, animate = true)
                    }
                    syncData { history() }
                    true
                }
                holder.download.setOnClickListener {
                    fragment.lifecycleScope.launch(Dispatchers.Main){
                        val obj = withContext(Dispatchers.IO) { downloadsDAO.getByEid(item.eid.toString()) }
                        if (FileAccessHelper.canDownload(fragment) &&
                                !item.state.fileWrapper.exist &&
                                !item.state.isDownloading &&
                            item.state.downloadState != DownloadObject.PENDING) {
                            holder.setLocked(true)
                            ServersFactory.start(context, item.chapterUrl, item.asChapter(), isStream = false, addQueue = false, serversInterface = object : ServersFactory.ServersInterface {
                                override fun onFinish(started: Boolean, success: Boolean) {
                                    if (started) {
                                        item.state.fileWrapper.exist = true
                                        holder.setState(isNetworkAvailable, true)
                                    }
                                    holder.setLocked(false)
                                }

                                override fun onCast(url: String?) {

                                }

                                override fun onProgressIndicator(boolean: Boolean) {
                                    fragment.doOnUI {
                                        if (boolean) {
                                            holder.progressBar.isIndeterminate = true
                                            holder.progressBarRoot.visibility = View.VISIBLE
                                        } else
                                            holder.progressBarRoot.visibility = View.GONE
                                    }
                                }

                                override fun getView(): View {
                                    return view
                                }
                            })
                        } else if (item.state.fileWrapper.exist && (obj == null || obj.state == DownloadObject.DOWNLOADING || obj.state == DownloadObject.COMPLETED)) {
                            doAsync {
                                recordsDAO.addChapter(item.asRecord())
                                syncData {
                                    history()
                                }
                            }
                            item.state.isSeen = true
                            holder.setSeen(true)
                            ServersFactory.startPlay(context, item.chapter, item.state.fileWrapper.name())
                        } else {
                            toast("Aun no se está descargando")
                        }
                    }
                }
                holder.download.setOnLongClickListener {
                    fragment.lifecycleScope.launch(Dispatchers.Main){
                        val obj = withContext(Dispatchers.IO) { downloadsDAO.getByEid(item.eid.toString()) }
                        if (CastUtil.get().connected() &&
                                item.state.fileWrapper.exist && (obj == null || obj.state == DownloadObject.COMPLETED)) {
                            doAsync {
                                recordsDAO.addChapter(item.asRecord())
                                syncData { history() }
                            }
                            item.state.isSeen = true
                            CastUtil.get().play(view, CastMedia.create(item))
                        }
                    }
                    true
                }
            }
        }
    }

    private fun setOrientation(block: Boolean) {
        noCrash {
            if (block)
                (fragment.activity as? AppCompatActivity)?.requestedOrientation = when {
                    context?.resources?.getBoolean(R.bool.isLandscape) == true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            else (fragment.activity as? AppCompatActivity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is ItemHolder) {
            holder.unsetCastingObserver()
            holder.listenersJob?.cancel()
            holder.listenersJob = null
        }
        super.onViewRecycled(holder)
    }

    internal fun updateList(list: MutableList<RecentAV1>, updateListener: () -> Unit) {
        this.isNetworkAvailable = Network.isConnected
        val wasEmpty = this.list.isEmpty()
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            this@RecentsAV1Adapter.list = list.distinctBy { it.eid }.toMutableList()
            if (PrefsUtil.isNativeAdsEnabled)
                this@RecentsAV1Adapter.list.implAdsRecent()
            if (this@RecentsAV1Adapter.list.isNotEmpty())
                withContext(Dispatchers.Main) {
                    notifyDataSetChanged()
                    if (wasEmpty)
                        updateListener.invoke()
                }
        }
    }

    /*override fun getItemId(position: Int): Long {
        return noCrashLet { list[position].key.toLong() } ?: 0
    }*/

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.find(R.id.card)
        val imageView: ImageView = itemView.find(R.id.img)
        val title: TextView = itemView.find(R.id.title)
        val chapter: TextView = itemView.find(R.id.chapter)
        val streaming: Button = itemView.find(R.id.streaming)
        val download: Button = itemView.find(R.id.download)
        val animeOverlay: SeenAnimeOverlay = itemView.find(R.id.seenOverlay)
        val downIcon: ImageView = itemView.find(R.id.down_icon)
        private val newIcon: ImageView = itemView.find(R.id.new_icon)
        private val favIcon: ImageView = itemView.find(R.id.fav_icon)
        val progressBar: ProgressBar = itemView.find(R.id.progress)
        val progressBarRoot: View = itemView.find(R.id.progress_root)
        val layButtons: View = itemView.find(R.id.lay_buttons)
        var listenersJob: Job? = null
        private var castingObserver: Observer<String>? = null

        fun setCastingObserver(owner: LifecycleOwner, observer: Observer<String>) {
            this.castingObserver = observer
            CastUtil.get().casting.observe(owner, observer)
        }

        fun unsetCastingObserver() {
            castingObserver?.let {
                CastUtil.get().casting.removeObserver(it)
                castingObserver = null
            }
        }

        fun setNew(isNew: Boolean) {
            newIcon.post { newIcon.visibility = if (isNew) View.VISIBLE else View.GONE }
        }

        fun setFav(isFav: Boolean) {
            favIcon.post { favIcon.visibility = if (isFav) View.VISIBLE else View.GONE }
        }

        private fun setDownloaded(isDownloaded: Boolean) {
            downIcon.post { downIcon.visibility = if (isDownloaded) View.VISIBLE else View.GONE }
        }

        fun setSeen(seen: Boolean) {
            animeOverlay.setSeen(seen, false)
        }

        fun setLocked(locked: Boolean) {
            streaming.post { streaming.isEnabled = !locked }
            download.post { download.isEnabled = !locked }
            setOrientation(locked)
        }

        fun setCasting(casting: Boolean, fileWrapper: FileWrapper<*>) {
            streaming.post { streaming.text = if (casting) "CAST" else if (fileWrapper.exist) "ELIMINAR" else "STREAMING" }
        }

        @UiThread
        fun setState(isNetworkAvailable: Boolean, existFile: Boolean) {
            setDownloaded(existFile)
            streaming.post {
                streaming.text = if (existFile) "ELIMINAR" else "STREAMING"
                if (!existFile)
                    streaming.isEnabled = isNetworkAvailable
                else
                    streaming.isEnabled = true
            }
            download.post {
                download.isEnabled = isNetworkAvailable || existFile
                download.text = if (existFile) "REPRODUCIR" else "DESCARGA"
            }
        }

        fun setDownloadState(downloadObject: DownloadObject?) {
            progressBar.post {
                if (downloadObject != null && PrefsUtil.showProgress())
                    when (downloadObject.state) {
                        DownloadObject.PENDING -> {
                            progressBarRoot.visibility = View.VISIBLE
                            progressBar.isIndeterminate = true
                        }
                        DownloadObject.DOWNLOADING -> {
                            progressBarRoot.visibility = View.VISIBLE
                            progressBar.isIndeterminate = false
                            if (downloadObject.getEta() == -2L || PrefsUtil.downloaderType == 0) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    progressBar.setProgress(downloadObject.progress, true)
                                else
                                    progressBar.progress = downloadObject.progress
                                if (downloadObject.getEta() == -2L && PrefsUtil.downloaderType != 0)
                                    progressBar.secondaryProgress = 100
                            } else {
                                progressBar.progress = 0
                                progressBar.secondaryProgress = downloadObject.progress
                            }
                        }
                        DownloadObject.PAUSED -> {
                            progressBarRoot.visibility = View.VISIBLE
                            progressBar.isIndeterminate = false
                        }
                        else -> progressBarRoot.visibility = View.GONE
                    }
                else
                    progressBarRoot.visibility = View.GONE
            }
        }
    }
}
