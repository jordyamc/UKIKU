package knf.kuma.animeinfo

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import com.squareup.picasso.Callback
import knf.kuma.App
import knf.kuma.R
import knf.kuma.backup.firestore.syncData
import knf.kuma.cast.CastMedia
import knf.kuma.commons.CastUtil
import knf.kuma.commons.EAHelper
import knf.kuma.commons.FileWrapper
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.bind
import knf.kuma.commons.distinct
import knf.kuma.commons.doOnUI
import knf.kuma.commons.getSurfaceColor
import knf.kuma.commons.isFullMode
import knf.kuma.commons.load
import knf.kuma.commons.noCrash
import knf.kuma.commons.roundedString
import knf.kuma.commons.safeShow
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadManagerCentral
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.av1.Chapter
import knf.kuma.pojos.av1.DirectoryAV1
import knf.kuma.pojos.av1.OrganizerWRecord
import knf.kuma.queue.QueueManager
import knf.kuma.videoservers.FileActions
import knf.kuma.videoservers.ServersFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

class AnimeChaptersAdapterMaterial(private val fragment: Fragment, private val recyclerView: RecyclerView, val anime: DirectoryAV1, val chapters: List<Chapter>, private val touchListener: DragSelectTouchListener, val isMaterial: Boolean) : RecyclerView.Adapter<AnimeChaptersAdapterMaterial.ChapterImgHolder>(), FastScrollRecyclerView.SectionedAdapter {

    private val context: Context? = fragment.context
    private val recordsDAO = CacheDB.INSTANCE.recordAV1DAO()
    private val seeingDAO = CacheDB.INSTANCE.organizerDAO()
    private val downloadsDAO = CacheDB.INSTANCE.downloadsDAO()
    private val isNetworkAvailable = Network.isConnected
    val selection = HashSet<Int>()
    private var seeingObject: OrganizerWRecord? = null
    var isImporting = false
    private var processingPosition = -1

    init {
        setHasStableIds(true)
        if (chapters.isNotEmpty()) {
            noCrash {
                doAsync {
                    seeingObject = seeingDAO.getByAid(anime.aid)
                    if (anime.state == 0)
                        DownloadedObserver.observe(fragment.lifecycleScope, chapters.size, chapters[0].fileWrapper(anime))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterImgHolder {
        return ChapterImgHolder(LayoutInflater.from(parent.context).inflate(if (isMaterial) R.layout.item_chapter_preview_material else R.layout.item_chapter_preview, parent, false))
    }

    override fun onBindViewHolder(holder: ChapterImgHolder, position: Int, payloads: MutableList<Any>) {
        if (context != null)
            if (selection.contains(position))
                holder.cardView.setBackgroundColor(ContextCompat.getColor(context, EAHelper.getThemeColorLight()))
            else
                holder.cardView.setBackgroundColor(fragment.getSurfaceColor())
        if (payloads.isEmpty())
            super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: ChapterImgHolder, position: Int) {
        if (context == null) return
        val chapter = chapters[position]
        if (selection.contains(position))
            holder.cardView.setBackgroundColor(ContextCompat.getColor(context, EAHelper.getThemeColorLight()))
        else
            holder.cardView.setBackgroundColor(fragment.getSurfaceColor())
        if (processingPosition == holder.bindingAdapterPosition) {
            holder.progressBar.isIndeterminate = true
            holder.progressBarRoot.visibility = View.VISIBLE
        } else
            holder.progressBarRoot.visibility = View.GONE
        if (!Network.isConnected)
            holder.imageView.visibility = View.GONE
        else {
            holder.imageView.load(chapter.thumbnail(anime), object : Callback {
                override fun onSuccess() {
                    holder.imageView.visibility = View.VISIBLE
                }
                override fun onError(e: Exception?) {

                }
            })
        }
        val downloadObject = AtomicReference<DownloadObject>()
        holder.apply {
            fileWrapperJob?.cancel()
            fileWrapperJob = fragment.lifecycleScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    chapter.fileWrapper(anime)
                    downloadObject.set(downloadsDAO.getByEid(chapter.eid.toString()))
                }

                if (!isActive)
                    return@launch
                setQueueObserver(CacheDB.INSTANCE.queueDAO().isInQueueLive(chapter.eid.toString()), fragment, Observer {
                    setQueue(it, isPlayAvailable(chapter.fileWrapper(anime), downloadObject.get()))
                })
                setDownloadObserver(downloadsDAO.getLiveByEid(chapter.eid.toString()).distinct, fragment, Observer { downloadObject1 ->
                    setDownloadState(downloadObject1)
                    val casting = CastUtil.get().casting.value
                    val isCasting = casting != null && casting == chapter.eid.toString()
                    if (!isCasting)
                        fragment.lifecycleScope.launch(Dispatchers.IO){
                            setQueue(QueueManager.isInQueue(chapter.eid.toString()), isPlayAvailable(chapter.fileWrapper(anime), downloadObject1))
                        }
                    else
                        setDownloaded(isPlayAvailable(chapter.fileWrapper(anime), downloadObject1), true)
                    downloadObject.set(downloadObject1)
                })
                setCastingObserver(fragment, Observer { s ->
                    if (chapter.eid.toString() != s)
                        fragment.lifecycleScope.launch(Dispatchers.IO){
                            setQueue(QueueManager.isInQueue(chapter.eid.toString()), isPlayAvailable(chapter.fileWrapper(anime), downloadObject.get()))
                        }
                    else
                        setDownloaded(isPlayAvailable(chapter.fileWrapper(anime), downloadObject.get()), chapter.eid.toString() == s)
                })
            }
        }
        holder.chapter.setTextColor(ContextCompat.getColor(context, if (chapter.isSeen) EAHelper.getThemeColor() else R.color.textPrimary))
        holder.separator.visibility = if (position == 0) View.GONE else View.VISIBLE
        holder.chapter.text = chapter.name
        if (!isFullMode)
            holder.actions.visibility = View.GONE
        else
            holder.actions.setOnClickListener { view ->
                fragment.lifecycleScope.launch(Dispatchers.Main) {
                    val menu = PopupMenu(context, view)
                    if (CastUtil.get().casting.value == chapter.eid.toString()) {
                        menu.inflate(R.menu.chapter_casting_menu)
                        if (canPlay(chapter.fileWrapper(anime)))
                            menu.menu.findItem(R.id.download).isVisible = false
                    } else if (isPlayAvailable(
                            chapter.fileWrapper(anime),
                            downloadObject.get()
                        )
                    ) {
                        menu.inflate(R.menu.chapter_downloaded_menu)
                        if (!CastUtil.get().connected())
                            menu.menu.findItem(R.id.cast).isVisible = false
                    } else if (isNetworkAvailable)
                        menu.inflate(R.menu.chapter_menu)
                    else
                        menu.inflate(R.menu.chapter_menu_offline)
                    if (QueueManager.isInQueue(chapter.eid.toString()) && menu.menu.findItem(R.id.queue) != null)
                        menu.menu.findItem(R.id.queue).isVisible = false
                    if (!PrefsUtil.showImport() || isImporting)
                        menu.menu.findItem(R.id.import_file).isVisible = false
                    menu.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.play -> if (canPlay(chapter.fileWrapper(anime))) {
                                fragment.lifecycleScope.launch(Dispatchers.IO){
                                    recordsDAO.addChapter(chapter.asRecord(anime))
                                }
                                chapter.isSeen = true
                                holder.setSeen(true)
                                ServersFactory.startPlay(context, chapter.episodeName(anime), chapter.fileWrapper(anime).name())
                                syncData {
                                    history()
                                }
                            } else {
                                Toaster.toast("Aun no se está descargando")
                            }
                            R.id.cast -> if (canPlay(chapter.fileWrapper(anime))) {
                                //CastUtil.get().play(fragment.activity as Activity, recyclerView, chapter.eid, SelfServer.start(chapter.fileName, true), chapter.name, chapter.number, if (chapter.img == null) chapter.aid else chapter.img, chapter.img == null)
                                CastUtil.get().play(recyclerView, CastMedia.create(anime, chapter))
                                fragment.lifecycleScope.launch(Dispatchers.IO){
                                    recordsDAO.addChapter(chapter.asRecord(anime))
                                }
                                chapter.isSeen = true
                                syncData {
                                    history()
                                }
                                holder.setSeen(true)
                            }
                            R.id.casting -> CastUtil.get().openControls()
                            R.id.delete -> MaterialDialog(context).safeShow {
                                message(
                                    text = "¿Eliminar el ${
                                        chapter.name.lowercase(
                                            Locale.getDefault()
                                        )
                                    }?"
                                )
                                positiveButton(text = "CONFIRMAR") {
                                    fragment.lifecycleScope.launch(Dispatchers.Main) {
                                        withContext(Dispatchers.IO) {
                                            FileAccessHelper.deletePath(
                                                chapter.filePath(anime),
                                                false
                                            )
                                        }
                                        downloadObject.get()?.state = -8
                                        chapter.fileWrapper(anime).exist = false
                                        holder.setDownloaded(false, false)
                                    }
                                    DownloadManagerCentral.cancel(chapter.eid.toString())
                                    QueueManager.remove(chapter.eid.toString())
                                }
                                negativeButton(text = "CANCELAR")
                            }
                            R.id.download -> {
                                setOrientation(true)
                                FileActions.download(fragment, anime, chapter) { state, _ ->
                                    when (state) {
                                        FileActions.CallbackState.START_DOWNLOAD -> {
                                            fragment.lifecycleScope.launch(Dispatchers.Main) {
                                                holder.progressBar.isIndeterminate = true
                                                holder.progressBarRoot.visibility = View.VISIBLE
                                                holder.setQueue(withContext(Dispatchers.IO){ CacheDB.INSTANCE.queueDAO().isInQueue(chapter.eid.toString()) }, true)
                                                chapter.fileWrapper(anime).exist = true
                                            }
                                        }

                                        else -> {
                                            fragment.doOnUI {
                                                holder.progressBarRoot.visibility = View.GONE
                                            }
                                        }
                                    }
                                    setOrientation(false)
                                }
                            }
                            R.id.streaming -> {
                                setOrientation(true)
                                FileActions.stream(fragment, anime, chapter) { state, extra ->
                                    when (state) {
                                        FileActions.CallbackState.START_STREAM, FileActions.CallbackState.START_CAST -> {
                                            if (state == FileActions.CallbackState.START_CAST) {
                                                CastUtil.get().play(recyclerView, CastMedia.create(anime, chapter, extra as? String))
                                            }
                                            fragment.lifecycleScope.launch(Dispatchers.IO){
                                                recordsDAO.addChapter(chapter.asRecord(anime))
                                            }
                                            chapter.isSeen = true
                                            syncData {
                                                history()
                                            }
                                            holder.setSeen(true)
                                        }

                                        else -> {
                                            //
                                        }
                                    }
                                    setOrientation(false)
                                }
                            }
                            R.id.queue -> if (isPlayAvailable(chapter.fileWrapper(anime), downloadObject.get())) {
                                QueueManager.add(chapter.fileWrapper(anime), downloadObject.get(), true, anime, chapter)
                                holder.setQueue(true, true)
                            } else {
                                setOrientation(true)
                                ServersFactory.start(context, chapter.link(anime), anime, chapter, true, true, object : ServersFactory.ServersInterface {
                                    override fun onFinish(started: Boolean, success: Boolean) {
                                        if (success) {
                                            holder.setQueue(true, false)
                                        }
                                        setOrientation(false)
                                    }

                                    override fun onCast(url: String?) {}

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
                                        return recyclerView
                                    }
                                })
                            }
                            R.id.share -> fragment.activity?.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, chapter.episodeName(anime) + "\n" + chapter.link(anime)), "Compartir"))
                            R.id.import_file -> (fragment as MoveCallback).onMove(chapter.filePath(anime))
                        }
                        true
                    }
                    menu.show()
                }
            }
        holder.cardView.setOnClickListener {
            if (chapter.isSeen) {
                fragment.lifecycleScope.launch(Dispatchers.IO){
                    recordsDAO.deleteChapter(anime.aid, chapter.number)
                }
                chapter.isSeen = false
                holder.chapter.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
            } else {
                fragment.lifecycleScope.launch(Dispatchers.IO){
                    recordsDAO.addChapter(chapter.asRecord(anime))
                }
                chapter.isSeen = true
                holder.chapter.setTextColor(ContextCompat.getColor(context, EAHelper.getThemeColor()))
            }
            syncData { history() }
        }
        holder.cardView.setOnLongClickListener {
            touchListener.startDragSelection(holder.adapterPosition)
            true
        }
    }

    override fun getSectionName(position: Int): String {
        return chapters[position].number.roundedString()
    }

    private fun setOrientation(block: Boolean) {
        noCrash {
            if (block)
                (fragment.activity as? AppCompatActivity)?.requestedOrientation = when {
                    fragment.context?.resources?.getBoolean(R.bool.isLandscape) == true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            else (fragment.activity as? AppCompatActivity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun isPlayAvailable(fileWrapper: FileWrapper<*>, downloadObject: DownloadObject?): Boolean {
        return fileWrapper.exist || downloadObject != null && downloadObject.isDownloading
    }

    private fun canPlay(fileWrapper: FileWrapper<*>): Boolean {
        return fileWrapper.exist
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun getItemCount(): Int {
        return chapters.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun select(pos: Int, sel: Boolean) {
        if (sel) {
            selection.add(pos)
        } else {
            selection.remove(pos)
        }
        notifyItemChanged(pos, 0)
    }

    fun selectRange(start: Int, end: Int, sel: Boolean) {
        for (i in start..end) {
            if (sel)
                selection.add(i)
            else
                selection.remove(i)
        }
        notifyItemRangeChanged(start, end - start + 1, 0)
    }

    fun deselectAll() {
        selection.clear()
        notifyDataSetChanged()
    }

    override fun onViewRecycled(holder: ChapterImgHolder) {
        holder.unsetCastingObserver()
        holder.unsetDownloadObserver()
        holder.unsetQueueObserver()
        super.onViewRecycled(holder)
    }

    inner class ChapterImgHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View by itemView.bind(R.id.card)
        val separator: View by itemView.bind(R.id.separator)
        val imageView: ImageView by itemView.bind(R.id.img)
        val chapter: TextView by itemView.bind(R.id.chapter)
        private val inDown: ImageView by itemView.bind(R.id.in_down)
        val actions: ImageButton by itemView.bind(R.id.actions)
        val progressBar: ProgressBar by itemView.bind(R.id.progress)
        val progressBarRoot: View by itemView.bind(R.id.progress_root)

        private var downloadLiveData: LiveData<DownloadObject> = MutableLiveData()
        private var queueLiveData: LiveData<Boolean> = MutableLiveData()

        private var downloadObserver: Observer<DownloadObject>? = null
        private var castingObserver: Observer<String>? = null
        private var queueObserver: Observer<Boolean>? = null
        var fileWrapperJob: Job? = null

        fun setDownloadObserver(downloadLiveData: LiveData<DownloadObject>, owner: LifecycleOwner?, observer: Observer<DownloadObject>) {
            if (owner == null) return
            this.downloadLiveData = downloadLiveData
            this.downloadObserver = observer
            this.downloadLiveData.observe(owner, observer)
        }

        fun unsetDownloadObserver() {
            downloadObserver?.let {
                downloadLiveData.removeObserver(it)
                downloadObserver = null
            }
        }

        fun setCastingObserver(owner: LifecycleOwner?, observer: Observer<String>) {
            if (owner == null) return
            this.castingObserver = observer
            CastUtil.get().casting.observe(owner, observer)
        }

        fun unsetCastingObserver() {
            castingObserver?.let {
                CastUtil.get().casting.removeObserver(it)
                castingObserver = null
            }
        }

        fun setQueueObserver(queueLivedata: LiveData<Boolean>, owner: LifecycleOwner?, observer: Observer<Boolean>) {
            if (owner == null) return
            this.queueLiveData = queueLivedata
            this.queueObserver = observer
            this.queueLiveData.observe(owner, observer)
        }

        fun unsetQueueObserver() {
            queueObserver?.let {
                queueLiveData.removeObserver(it)
                queueObserver = null
            }
        }

        fun setDownloaded(downloaded: Boolean, isCasting: Boolean) {
            noCrash {
                inDown.post {
                    if (downloaded)
                        inDown.setImageResource(R.drawable.ic_chap_down)
                    if (isCasting)
                        inDown.setImageResource(R.drawable.ic_casting)
                    inDown.visibility = if (downloaded || isCasting) View.VISIBLE else View.GONE
                }
            }
        }

        fun setQueue(isInQueue: Boolean, isDownloaded: Boolean) {
            noCrash {
                inDown.post {
                    if (!isInQueue)
                        setDownloaded(isDownloaded, false)
                    else {
                        inDown.setImageResource(if (isDownloaded) R.drawable.ic_queue_file else R.drawable.ic_queue_normal)
                        inDown.visibility = View.VISIBLE
                    }
                }
            }
        }

        fun setSeen(seen: Boolean) {
            chapter.post { chapter.setTextColor(ContextCompat.getColor(App.context, if (seen) EAHelper.getThemeColor() else R.color.textPrimary)) }
        }

        fun setDownloadState(downloadObject: DownloadObject?) {
            progressBar.post {
                if (downloadObject != null && PrefsUtil.showProgress())
                    when (downloadObject.state) {
                        DownloadObject.PENDING -> {
                            progressBarRoot.visibility = View.VISIBLE
                            progressBar.isIndeterminate = true
                        }
                        DownloadObject.PAUSED, DownloadObject.DOWNLOADING -> {
                            progressBarRoot.visibility = View.VISIBLE
                            progressBar.isIndeterminate = false
                            if (downloadObject.getEta() == -2L || PrefsUtil.downloaderType == 0)
                                progressBar.setProgress(downloadObject.progress, true)
                            else {
                                progressBar.progress = 0
                                progressBar.secondaryProgress = downloadObject.progress
                            }
                        }
                        else -> progressBarRoot.visibility = View.GONE
                    }
                else
                    progressBarRoot.visibility = View.GONE
            }
        }
    }

}
