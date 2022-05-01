package knf.kuma.animeinfo

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
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
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import com.squareup.picasso.Callback
import knf.kuma.App
import knf.kuma.R
import knf.kuma.ads.AdsUtils
import knf.kuma.animeinfo.fragments.ChaptersFragmentMaterial
import knf.kuma.animeinfo.ktx.epTitle
import knf.kuma.animeinfo.ktx.fileName
import knf.kuma.animeinfo.ktx.filePath
import knf.kuma.backup.firestore.syncData
import knf.kuma.cast.CastMedia
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadManager
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.RecordObject
import knf.kuma.pojos.SeeingObject
import knf.kuma.pojos.SeenObject
import knf.kuma.queue.QueueManager
import knf.kuma.videoservers.ServersFactory
import kotlinx.coroutines.*
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.net.URL
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class AnimeChaptersAdapterMaterial(private val fragment: Fragment, private val recyclerView: RecyclerView, val chapters: List<ChapterObjWrap>, private val touchListener: DragSelectTouchListener) : RecyclerView.Adapter<AnimeChaptersAdapterMaterial.ChapterImgHolder>(), FastScrollRecyclerView.SectionedAdapter {

    private val context: Context? = fragment.context
    private val chaptersDAO = CacheDB.INSTANCE.seenDAO()
    private val recordsDAO = CacheDB.INSTANCE.recordsDAO()
    private val seeingDAO = CacheDB.INSTANCE.seeingDAO()
    private val downloadsDAO = CacheDB.INSTANCE.downloadsDAO()
    private val isNetworkAvailable = Network.isConnected
    val selection = HashSet<Int>()
    private var seeingObject: SeeingObject? = null
    var isImporting = false
    private var processingPosition = -1

    init {
        setHasStableIds(true)
        if (chapters.isNotEmpty()) {
            noCrash {
                doAsync {
                    seeingObject = seeingDAO.getByAid(chapters[0].chapter.aid)
                    if (CacheDB.INSTANCE.animeDAO().isCompleted(chapters[0].chapter.aid))
                        DownloadedObserver.observe(fragment.lifecycleScope, chapters.size, chapters[0].chapter.fileWrapper())
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterImgHolder {
        return ChapterImgHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chapter_preview_material, parent, false))
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
        if (processingPosition == holder.adapterPosition) {
            holder.progressBar.isIndeterminate = true
            holder.progressBarRoot.visibility = View.VISIBLE
        } else
            holder.progressBarRoot.visibility = View.GONE
        if (!Network.isConnected || chapter.chapter.img == null)
            holder.imageView.visibility = View.GONE
        if (chapter.chapter.img != null)
            PicassoSingle.get().load(chapter.chapter.img).into(holder.imageView, object : Callback {
                override fun onSuccess() {
                    holder.imageView.visibility = View.VISIBLE
                }

                override fun onError() {

                }
            })
        val downloadObject = AtomicReference<DownloadObject>()
        holder.apply {
            fileWrapperJob?.cancel()
            fileWrapperJob = fragment.lifecycleScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    chapter.chapter.fileWrapper()
                    downloadObject.set(downloadsDAO.getByEid(chapter.chapter.eid))
                }

                if (!isActive)
                    return@launch
                setQueueObserver(CacheDB.INSTANCE.queueDAO().isInQueueLive(chapter.chapter.eid), fragment, Observer {
                    setQueue(it, isPlayAvailable(chapter.chapter.fileWrapper(), downloadObject.get()))
                })
                setDownloadObserver(downloadsDAO.getLiveByEid(chapter.chapter.eid).distinct, fragment, Observer { downloadObject1 ->
                    setDownloadState(downloadObject1)
                    val casting = CastUtil.get().casting.value
                    val isCasting = casting != null && casting == chapter.chapter.eid
                    if (!isCasting)
                        fragment.lifecycleScope.launch(Dispatchers.IO){
                            setQueue(QueueManager.isInQueue(chapter.chapter.eid), isPlayAvailable(chapter.chapter.fileWrapper(), downloadObject1))
                        }
                    else
                        setDownloaded(isPlayAvailable(chapter.chapter.fileWrapper(), downloadObject1), true)
                    downloadObject.set(downloadObject1)
                })
                setCastingObserver(fragment, Observer { s ->
                    if (chapter.chapter.eid != s)
                        fragment.lifecycleScope.launch(Dispatchers.IO){
                            setQueue(QueueManager.isInQueue(chapter.chapter.eid), isPlayAvailable(chapter.chapter.fileWrapper(), downloadObject.get()))
                        }
                    else
                        setDownloaded(isPlayAvailable(chapter.chapter.fileWrapper(), downloadObject.get()), chapter.chapter.eid == s)
                })
            }
        }
        holder.chapter.setTextColor(ContextCompat.getColor(context, if (chapter.isSeen) EAHelper.getThemeColor() else R.color.textPrimary))
        holder.separator.visibility = if (position == 0) View.GONE else View.VISIBLE
        holder.chapter.text = chapter.chapter.number
        if (!isFullMode)
            holder.actions.visibility = View.GONE
        else
            holder.actions.setOnClickListener { view ->
                fragment.lifecycleScope.launch(Dispatchers.Main) {
                    val menu = PopupMenu(context, view)
                    if (CastUtil.get().casting.value == chapter.chapter.eid) {
                        menu.inflate(R.menu.chapter_casting_menu)
                        if (canPlay(chapter.chapter.fileWrapper()))
                            menu.menu.findItem(R.id.download).isVisible = false
                    } else if (isPlayAvailable(
                            chapter.chapter.fileWrapper(),
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
                    if (QueueManager.isInQueue(chapter.chapter.eid) && menu.menu.findItem(R.id.queue) != null)
                        menu.menu.findItem(R.id.queue).isVisible = false
                    if (!PrefsUtil.showImport() || isImporting)
                        menu.menu.findItem(R.id.import_file).isVisible = false
                    menu.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.play -> if (canPlay(chapter.chapter.fileWrapper())) {
                                fragment.lifecycleScope.launch(Dispatchers.IO){
                                    chaptersDAO.addChapter(SeenObject.fromChapter(chapter.chapter))
                                    recordsDAO.add(RecordObject.fromChapter(chapter.chapter))
                                }
                                chapter.isSeen = true
                                updateSeeing(chapter.chapter.number)
                                holder.setSeen(true)
                                ServersFactory.startPlay(context, chapter.chapter.epTitle, chapter.chapter.fileWrapper().name())
                                syncData {
                                    history()
                                    seen()
                                }
                            } else {
                                Toaster.toast("Aun no se está descargando")
                            }
                            R.id.cast -> if (canPlay(chapter.chapter.fileWrapper())) {
                                //CastUtil.get().play(fragment.activity as Activity, recyclerView, chapter.eid, SelfServer.start(chapter.fileName, true), chapter.name, chapter.number, if (chapter.img == null) chapter.aid else chapter.img, chapter.img == null)
                                CastUtil.get().play(recyclerView, CastMedia.create(chapter.chapter))
                                fragment.lifecycleScope.launch(Dispatchers.IO){
                                    chaptersDAO.addChapter(SeenObject.fromChapter(chapter.chapter))
                                    recordsDAO.add(RecordObject.fromChapter(chapter.chapter))
                                }
                                chapter.isSeen = true
                                syncData {
                                    history()
                                    seen()
                                }
                                updateSeeing(chapter.chapter.number)
                                holder.setSeen(true)
                            }
                            R.id.casting -> CastUtil.get().openControls()
                            R.id.delete -> MaterialDialog(context).safeShow {
                                message(
                                    text = "¿Eliminar el ${
                                        chapter.chapter.number.lowercase(
                                            Locale.getDefault()
                                        )
                                    }?"
                                )
                                positiveButton(text = "CONFIRMAR") {
                                    fragment.lifecycleScope.launch(Dispatchers.Main) {
                                        withContext(Dispatchers.IO) {
                                            FileAccessHelper.deletePath(
                                                chapter.chapter.filePath,
                                                false
                                            )
                                        }
                                        downloadObject.get()?.state = -8
                                        chapter.chapter.fileWrapper().exist = false
                                        holder.setDownloaded(false, false)
                                    }
                                    DownloadManager.cancel(chapter.chapter.eid)
                                    QueueManager.remove(chapter.chapter.eid)
                                }
                                negativeButton(text = "CANCELAR")
                            }
                            R.id.download -> {
                                setOrientation(true)
                                ServersFactory.start(context, chapter.chapter.link, chapter.chapter, false, false, object : ServersFactory.ServersInterface {
                                    override fun onFinish(started: Boolean, success: Boolean) {
                                        fragment.lifecycleScope.launch(Dispatchers.Main) {
                                            if (started) {
                                                holder.setQueue(withContext(Dispatchers.IO){ CacheDB.INSTANCE.queueDAO().isInQueue(chapter.chapter.eid) }, true)
                                                chapter.chapter.fileWrapper().exist = true
                                            }
                                            setOrientation(false)
                                        }
                                    }

                                    override fun onCast(url: String?) {

                                    }

                                    override fun onProgressIndicator(boolean: Boolean) {
                                        doOnUI {
                                            if (boolean) {
                                                holder.progressBar.isIndeterminate = true
                                                holder.progressBarRoot.visibility = View.VISIBLE
                                            } else
                                                holder.progressBarRoot.visibility = View.GONE
                                        }
                                    }

                                    override fun getView(): View? {
                                        return recyclerView
                                    }
                                })
                            }
                            R.id.streaming -> {
                                setOrientation(true)
                                ServersFactory.start(context, chapter.chapter.link, chapter.chapter, true, false, object : ServersFactory.ServersInterface {
                                    override fun onFinish(started: Boolean, success: Boolean) {
                                        if (!started && success) {
                                            fragment.lifecycleScope.launch(Dispatchers.IO){
                                                chaptersDAO.addChapter(SeenObject.fromChapter(chapter.chapter))
                                                recordsDAO.add(RecordObject.fromChapter(chapter.chapter))
                                            }
                                            chapter.isSeen = true
                                            syncData {
                                                history()
                                                seen()
                                            }
                                            updateSeeing(chapter.chapter.number)
                                            holder.setSeen(true)
                                        }
                                        setOrientation(false)
                                    }

                                    override fun onCast(url: String?) {
                                        CastUtil.get().play(recyclerView, CastMedia.create(chapter.chapter, url))
                                        fragment.lifecycleScope.launch(Dispatchers.IO){
                                            chaptersDAO.addChapter(SeenObject.fromChapter(chapter.chapter))
                                            recordsDAO.add(RecordObject.fromChapter(chapter.chapter))
                                        }
                                        chapter.isSeen = true
                                        syncData {
                                            history()
                                            seen()
                                        }
                                        updateSeeing(chapter.chapter.number)
                                        holder.setSeen(true)
                                    }

                                    override fun onProgressIndicator(boolean: Boolean) {

                                    }

                                    override fun getView(): View? {
                                        return recyclerView
                                    }
                                })
                            }
                            R.id.queue -> if (isPlayAvailable(chapter.chapter.fileWrapper(), downloadObject.get())) {
                                QueueManager.add(chapter.chapter.fileWrapper(), downloadObject.get(), true, chapter.chapter)
                                holder.setQueue(true, true)
                            } else {
                                setOrientation(true)
                                ServersFactory.start(context, chapter.chapter.link, chapter.chapter, true, true, object : ServersFactory.ServersInterface {
                                    override fun onFinish(started: Boolean, success: Boolean) {
                                        if (success) {
                                            holder.setQueue(true, false)
                                        }
                                        setOrientation(false)
                                    }

                                    override fun onCast(url: String?) {}

                                    override fun onProgressIndicator(boolean: Boolean) {
                                        doOnUI {
                                            if (boolean) {
                                                holder.progressBar.isIndeterminate = true
                                                holder.progressBarRoot.visibility = View.VISIBLE
                                            } else
                                                holder.progressBarRoot.visibility = View.GONE
                                        }
                                    }

                                    override fun getView(): View? {
                                        return recyclerView
                                    }
                                })
                            }
                            R.id.share -> fragment.activity?.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, chapter.chapter.epTitle + "\n" + chapter.chapter.link), "Compartir"))
                            R.id.import_file -> (fragment as ChaptersFragmentMaterial).onMove(chapter.chapter.fileName)
                            R.id.commentaries -> {
                                fragment.lifecycleScope.launch(Dispatchers.Main){
                                    try {
                                        val version = withContext(Dispatchers.IO) {
                                            Regex("load\\.(\\w+)\\.js").find(URL("https://https-animeflv-net.disqus.com/embed.js").readText())?.destructured?.component1()
                                        }
                                        CommentariesDialog.show(
                                            fragment,
                                            withContext(Dispatchers.IO) {
                                                chapter.chapter.commentariesLink(version)
                                            })
                                    } catch (e: Exception) {
                                        noCrashSuspend {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(withContext(Dispatchers.IO) {
                                                        chapter.chapter.commentariesLink(
                                                            AdsUtils.remoteConfigs.getString(
                                                                "disqus_version"
                                                            )
                                                        )
                                                    })
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        true
                    }
                    menu.show()
                }
            }
        holder.cardView.setOnClickListener {
            if (chapter.isSeen) {
                fragment.lifecycleScope.launch(Dispatchers.IO){
                    chaptersDAO.deleteChapter(chapter.chapter.aid, chapter.chapter.number)
                }
                chapter.isSeen = false
                holder.chapter.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
            } else {
                fragment.lifecycleScope.launch(Dispatchers.IO){
                    chaptersDAO.addChapter(SeenObject.fromChapter(chapter.chapter))
                }
                chapter.isSeen = true
                holder.chapter.setTextColor(ContextCompat.getColor(context, EAHelper.getThemeColor()))
            }
            syncData { seen() }
            updateSeeing(chapter.chapter.number)
        }
        holder.cardView.setOnLongClickListener {
            touchListener.startDragSelection(holder.adapterPosition)
            true
        }
    }

    override fun getSectionName(position: Int): String {
        return chapters[position].chapter.number.trim().substring(chapters[position].chapter.number.trim().lastIndexOf(" ") + 1)
    }

    private fun updateSeeing(chapter: String) {
        fragment.lifecycleScope.launch(Dispatchers.IO){
            seeingObject?.let {
                it.chapter = chapter
                seeingDAO.update(it)
                syncData { seeing() }
            }
        }
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
        return chapters[position].chapter.chapterType?.value ?: 0
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
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    progressBar.setProgress(downloadObject.progress, true)
                                else
                                    progressBar.progress = downloadObject.progress
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
