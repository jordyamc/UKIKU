package knf.kuma.animeinfo

import android.content.ActivityNotFoundException
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
import com.google.android.material.card.MaterialCardView
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import com.squareup.picasso.Callback
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.animeinfo.fragments.ChaptersFragment
import knf.kuma.animeinfo.ktx.epTitle
import knf.kuma.animeinfo.ktx.fileName
import knf.kuma.animeinfo.ktx.filePath
import knf.kuma.backup.firestore.syncData
import knf.kuma.cast.CastMedia
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadManager
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.*
import knf.kuma.queue.QueueManager
import knf.kuma.videoservers.ServersFactory
import kotlinx.coroutines.*
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class AnimeChaptersAdapter(private val fragment: Fragment, private val recyclerView: RecyclerView, private val chapters: MutableList<AnimeObject.WebInfo.AnimeChapter>, private val touchListener: DragSelectTouchListener) : RecyclerView.Adapter<AnimeChaptersAdapter.ChapterImgHolder>(), FastScrollRecyclerView.SectionedAdapter {

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
                seeingObject = seeingDAO.getByAid(chapters[0].aid)
                doAsync {
                    doAsync {
                        if (CacheDB.INSTANCE.animeDAO().isCompleted(chapters[0].aid))
                            DownloadedObserver.observe(chapters.size, chapters[0].fileName)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterImgHolder {
        return ChapterImgHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chapter_preview, parent, false))
    }

    override fun onBindViewHolder(holder: ChapterImgHolder, position: Int, payloads: MutableList<Any>) {
        if (context != null)
            if (selection.contains(position))
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, EAHelper.getThemeColorLight()))
            else
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.cardview_background))
        if (payloads.isEmpty())
            super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: ChapterImgHolder, position: Int) {
        if (context == null) return
        val chapter = chapters[position]
        val downloadObject = AtomicReference(downloadsDAO.getByEid(chapter.eid))
        if (selection.contains(position))
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, EAHelper.getThemeColorLight()))
        else
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.cardview_background))
        if (processingPosition == holder.adapterPosition) {
            holder.progressBar.isIndeterminate = true
            holder.progressBarRoot.visibility = View.VISIBLE
        } else
            holder.progressBarRoot.visibility = View.GONE
        if (!Network.isConnected || chapter.img == null)
            holder.imageView.visibility = View.GONE
        if (chapter.img != null)
            PicassoSingle.get().load(chapter.img).into(holder.imageView, object : Callback {
                override fun onSuccess() {
                    holder.imageView.visibility = View.VISIBLE
                }

                override fun onError() {

                }
            })
        holder.apply {
            fileWrapperJob?.cancel()
            fileWrapperJob = fragment.lifecycleScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    chapter.fileWrapper()
                }
                if (!isActive)
                    return@launch
                setQueueObserver(CacheDB.INSTANCE.queueDAO().isInQueueLive(chapter.eid), fragment, Observer {
                    setQueue(it, isPlayAvailable(chapter.fileWrapper(), downloadObject.get()))
                })
                setDownloadObserver(downloadsDAO.getLiveByEid(chapter.eid).distinct, fragment, Observer { downloadObject1 ->
                    setDownloadState(downloadObject1)
                    val casting = CastUtil.get().casting.value
                    val isCasting = casting != null && casting == chapter.eid
                    if (!isCasting)
                        setQueue(QueueManager.isInQueue(chapter.eid), isPlayAvailable(chapter.fileWrapper(), downloadObject1))
                    else
                        setDownloaded(isPlayAvailable(chapter.fileWrapper(), downloadObject1), true)
                    downloadObject.set(downloadObject1)
                })
                setCastingObserver(fragment, Observer { s ->
                    if (chapter.eid != s)
                        setQueue(QueueManager.isInQueue(chapter.eid), isPlayAvailable(chapter.fileWrapper(), downloadObject.get()))
                    else
                        setDownloaded(isPlayAvailable(chapter.fileWrapper(), downloadObject.get()), chapter.eid == s)
                })
            }
        }
        holder.chapter.setTextColor(ContextCompat.getColor(context, if (chaptersDAO.chapterIsSeen(chapter.aid, chapter.number)) EAHelper.getThemeColor() else R.color.textPrimary))
        holder.separator.visibility = if (position == 0) View.GONE else View.VISIBLE
        holder.chapter.text = chapter.number
        if (BuildConfig.BUILD_TYPE == "playstore")
            holder.actions.visibility = View.GONE
        else
            holder.actions.setOnClickListener { view ->
                val menu = PopupMenu(context, view)
                if (CastUtil.get().casting.value == chapter.eid) {
                    menu.inflate(R.menu.chapter_casting_menu)
                    if (canPlay(chapter.fileWrapper()))
                        menu.menu.findItem(R.id.download).isVisible = false
                } else if (isPlayAvailable(chapter.fileWrapper(), downloadObject.get())) {
                    menu.inflate(R.menu.chapter_downloaded_menu)
                    if (!CastUtil.get().connected())
                        menu.menu.findItem(R.id.cast).isVisible = false
                } else if (isNetworkAvailable)
                    menu.inflate(R.menu.chapter_menu)
                else
                    menu.inflate(R.menu.chapter_menu_offline)
                if (QueueManager.isInQueue(chapter.eid) && menu.menu.findItem(R.id.queue) != null)
                    menu.menu.findItem(R.id.queue).isVisible = false
                if (!PrefsUtil.showImport() || isImporting)
                    menu.menu.findItem(R.id.import_file).isVisible = false
                menu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.play -> if (canPlay(chapter.fileWrapper())) {
                            chaptersDAO.addChapter(SeenObject.fromChapter(chapter))
                            recordsDAO.add(RecordObject.fromChapter(chapter))
                            updateSeeing(chapter.number)
                            holder.setSeen(true)
                            ServersFactory.startPlay(context, chapter.epTitle, chapter.fileWrapper().name())
                            syncData {
                                history()
                                seen()
                            }
                        } else {
                            Toaster.toast("Aun no se está descargando")
                        }
                        R.id.cast -> if (canPlay(chapter.fileWrapper())) {
                            //CastUtil.get().play(fragment.activity as Activity, recyclerView, chapter.eid, SelfServer.start(chapter.fileName, true), chapter.name, chapter.number, if (chapter.img == null) chapter.aid else chapter.img, chapter.img == null)
                            CastUtil.get().play(recyclerView, CastMedia.create(chapter))
                            chaptersDAO.addChapter(SeenObject.fromChapter(chapter))
                            recordsDAO.add(RecordObject.fromChapter(chapter))
                            syncData {
                                history()
                                seen()
                            }
                            updateSeeing(chapter.number)
                            holder.setSeen(true)
                        }
                        R.id.casting -> CastUtil.get().openControls()
                        R.id.delete -> MaterialDialog(context).safeShow {
                            message(text = "¿Eliminar el ${chapter.number.toLowerCase()}?")
                            positiveButton(text = "CONFIRMAR") {
                                downloadObject.get()?.state = -8
                                chapter.fileWrapper().exist = false
                                holder.setDownloaded(false, false)
                                FileAccessHelper.deletePath(chapter.filePath, false)
                                DownloadManager.cancel(chapter.eid)
                                QueueManager.remove(chapter.eid)
                            }
                            negativeButton(text = "CANCELAR")
                        }
                        R.id.download -> {
                            setOrientation(true)
                            ServersFactory.start(context, chapter.link, chapter, false, false, object : ServersFactory.ServersInterface {
                                override fun onFinish(started: Boolean, success: Boolean) {
                                    if (started) {
                                        holder.setQueue(CacheDB.INSTANCE.queueDAO().isInQueue(chapter.eid), true)
                                        chapter.fileWrapper().exist = true
                                    }
                                    setOrientation(false)
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
                            ServersFactory.start(context, chapter.link, chapter, true, false, object : ServersFactory.ServersInterface {
                                override fun onFinish(started: Boolean, success: Boolean) {
                                    if (!started && success) {
                                        chaptersDAO.addChapter(SeenObject.fromChapter(chapter))
                                        recordsDAO.add(RecordObject.fromChapter(chapter))
                                        syncData {
                                            history()
                                            seen()
                                        }
                                        updateSeeing(chapter.number)
                                        holder.setSeen(true)
                                    }
                                    setOrientation(false)
                                }

                                override fun onCast(url: String?) {
                                    CastUtil.get().play(recyclerView, CastMedia.create(chapter, url))
                                    chaptersDAO.addChapter(SeenObject.fromChapter(chapter))
                                    recordsDAO.add(RecordObject.fromChapter(chapter))
                                    syncData {
                                        history()
                                        seen()
                                    }
                                    updateSeeing(chapter.number)
                                    holder.setSeen(true)
                                }

                                override fun onProgressIndicator(boolean: Boolean) {

                                }

                                override fun getView(): View? {
                                    return recyclerView
                                }
                            })
                        }
                        R.id.queue -> if (isPlayAvailable(chapter.fileWrapper(), downloadObject.get())) {
                            QueueManager.add(Uri.fromFile(chapter.fileWrapper().file()), true, chapter)
                            holder.setQueue(true, true)
                        } else {
                            setOrientation(true)
                            ServersFactory.start(context, chapter.link, chapter, true, true, object : ServersFactory.ServersInterface {
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
                                .putExtra(Intent.EXTRA_TEXT, chapter.epTitle + "\n" + chapter.link), "Compartir"))
                        R.id.import_file -> (fragment as ChaptersFragment).onMove(chapter.fileName)
                        R.id.commentaries -> {
                            try {
                                CommentariesDialog.show(fragment, chapter.commentariesLink())
                            } catch (e: ActivityNotFoundException) {
                                noCrash { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(chapter.commentariesLink()))) }
                            }
                        }
                    }
                    true
                }
                menu.show()
            }
        holder.cardView.setOnClickListener {
            if (chaptersDAO.chapterIsSeen(chapter.aid, chapter.number)) {
                chaptersDAO.deleteChapter(chapter.aid, chapter.number)
                holder.chapter.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
            } else {
                chaptersDAO.addChapter(SeenObject.fromChapter(chapter))
                holder.chapter.setTextColor(ContextCompat.getColor(context, EAHelper.getThemeColor()))
            }
            syncData { seen() }
            updateSeeing(chapter.number)
        }
        holder.cardView.setOnLongClickListener {
            touchListener.startDragSelection(holder.adapterPosition)
            true
        }
    }

    override fun getSectionName(position: Int): String {
        return chapters[position].number.trim().substring(chapters[position].number.trim().lastIndexOf(" ") + 1)
    }

    private fun updateSeeing(chapter: String) {
        seeingObject?.let {
            it.chapter = chapter
            seeingDAO.update(it)
            syncData { seeing() }
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
        return chapters[position].chapterType?.value ?: 0
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
        val cardView: MaterialCardView by itemView.bind(R.id.card)
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
            inDown.post {
                if (downloaded)
                    inDown.setImageResource(R.drawable.ic_chap_down)
                if (isCasting)
                    inDown.setImageResource(R.drawable.ic_casting)
                inDown.visibility = if (downloaded || isCasting) View.VISIBLE else View.GONE
            }
        }

        fun setQueue(isInQueue: Boolean, isDownloaded: Boolean) {
            inDown.post {
                if (!isInQueue)
                    setDownloaded(isDownloaded, false)
                else {
                    inDown.setImageResource(if (isDownloaded) R.drawable.ic_queue_file else R.drawable.ic_queue_normal)
                    inDown.visibility = View.VISIBLE
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
