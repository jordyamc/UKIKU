package knf.kuma.recents

import android.app.Activity
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
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.*
import knf.kuma.custom.SeenAnimeOverlay
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirectoryService
import knf.kuma.download.DownloadManager
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.RecentObject
import knf.kuma.pojos.RecordObject
import knf.kuma.queue.QueueManager
import knf.kuma.videoservers.ServersFactory
import kotlinx.android.synthetic.main.item_recents.view.*
import xdroid.toaster.Toaster
import java.util.*

class RecentsAdapter internal constructor(private val fragment: Fragment, private val view: View) : RecyclerView.Adapter<RecentsAdapter.ItemHolder>() {

    private val context: Context = fragment.context!!
    private var list: MutableList<RecentObject> = ArrayList()
    private val dao = CacheDB.INSTANCE.favsDAO()
    private val animeDAO = CacheDB.INSTANCE.animeDAO()
    private val chaptersDAO = CacheDB.INSTANCE.chaptersDAO()
    private val recordsDAO = CacheDB.INSTANCE.recordsDAO()
    private val downloadsDAO = CacheDB.INSTANCE.downloadsDAO()
    private var isNetworkAvailable: Boolean = Network.isConnected

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_recents, parent, false))
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.unsetObservers()
        val recentObject = list[position]
        holder.setState(isNetworkAvailable, recentObject.isChapterDownloaded || recentObject.isDownloading)
        PicassoSingle[context].load(PatternUtil.getCover(recentObject.aid!!)).into(holder.imageView)
        holder.setNew(recentObject.isNew)
        holder.setFav(dao.isFav(Integer.parseInt(recentObject.aid!!)))
        holder.setSeen(chaptersDAO.chapterIsSeen(recentObject.eid!!))
        dao.favObserver(Integer.parseInt(recentObject.aid!!)).observe(fragment, Observer { object1 -> holder.setFav(object1 != null) })
        holder.setChapterObserver(chaptersDAO.chapterSeen(recentObject.eid!!), fragment, Observer { chapter -> holder.setSeen(chapter != null) })
        holder.setDownloadObserver(downloadsDAO.getLiveByEid(recentObject.eid!!), fragment, Observer { downloadObject ->
            holder.setDownloadState(downloadObject)
            if (downloadObject == null) {
                recentObject.downloadState = -8
                recentObject.isDownloading = false
            } else {
                recentObject.isDownloading = downloadObject.state == DownloadObject.DOWNLOADING || downloadObject.state == DownloadObject.PENDING || downloadObject.state == DownloadObject.PAUSED
                recentObject.downloadState = downloadObject.state
                val file = FileAccessHelper.INSTANCE.getFile(recentObject.fileName)
                recentObject.isChapterDownloaded = file.exists()
                if (downloadObject.state == DownloadObject.DOWNLOADING || downloadObject.state == DownloadObject.PENDING)
                    holder.downIcon.setImageResource(R.drawable.ic_download)
                else if (downloadObject.state == DownloadObject.PAUSED)
                    holder.downIcon.setImageResource(R.drawable.ic_pause_normal)
            }
            holder.setState(isNetworkAvailable, recentObject.isChapterDownloaded || recentObject.isDownloading)
        })
        holder.setCastingObserver(fragment, Observer { s ->
            if (recentObject.eid == s) {
                holder.setCasting(true, recentObject.fileName)
                holder.streaming.setOnClickListener { CastUtil.get().openControls() }
            } else {
                holder.setCasting(false, recentObject.fileName)
                holder.streaming.setOnClickListener {
                    if (recentObject.isChapterDownloaded || recentObject.isDownloading) {
                        MaterialDialog(context).safeShow {
                            message(text = "¿Eliminar el ${recentObject.chapter!!.toLowerCase()} de ${recentObject.name}?")
                            positiveButton(text = "CONFIRMAR") {
                                FileAccessHelper.INSTANCE.delete(recentObject.fileName, true)
                                DownloadManager.cancel(recentObject.eid!!)
                                QueueManager.remove(recentObject.eid!!)
                                recentObject.isChapterDownloaded = false
                                holder.setState(isNetworkAvailable, false)
                            }
                            negativeButton(text = "CANCELAR")
                        }
                    } else {
                        holder.setLocked(true)
                        ServersFactory.start(context, recentObject.url!!, DownloadObject.fromRecent(recentObject), true, object : ServersFactory.ServersInterface {
                            override fun onFinish(started: Boolean, success: Boolean) {
                                if (!started && success) {
                                    chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(recentObject))
                                    recordsDAO.add(RecordObject.fromRecent(recentObject))
                                }
                                holder.setLocked(false)
                            }

                            override fun onCast(url: String?) {
                                CastUtil.get().play(fragment.activity as? Activity, view, recentObject.eid!!, url, recentObject.name!!, recentObject.chapter!!, recentObject.aid!!, true)
                                chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(recentObject))
                                recordsDAO.add(RecordObject.fromRecent(recentObject))
                                holder.setSeen(true)
                                holder.setLocked(false)
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
                                return view
                            }
                        })
                    }
                }
            }
        })
        holder.title.text = recentObject.name
        holder.chapter.text = recentObject.chapter
        holder.cardView.setOnClickListener {
            if (recentObject.animeObject != null) {
                ActivityAnime.open(fragment, recentObject.animeObject!!, holder.imageView)
            } else {
                val animeObject = animeDAO.getByAid(recentObject.aid!!)
                if (animeObject != null) {
                    ActivityAnime.open(fragment, animeObject, holder.imageView)
                } else {
                    Toaster.toast("Aún no esta en directorio!")
                    DirectoryService.run(context)
                }
            }
        }
        holder.cardView.setOnLongClickListener {
            if (!chaptersDAO.chapterIsSeen(recentObject.eid!!)) {
                chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(recentObject))
                holder.animeOverlay.setSeen(true, true)
            } else {
                chaptersDAO.deleteChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(recentObject))
                holder.animeOverlay.setSeen(false, true)
            }
            true
        }
        holder.download.setOnClickListener {
            val obj = downloadsDAO.getByEid(recentObject.eid!!)
            if (FileAccessHelper.INSTANCE.canDownload(fragment) &&
                    !recentObject.isChapterDownloaded &&
                    !recentObject.isDownloading &&
                    recentObject.downloadState != DownloadObject.PENDING) {
                holder.setLocked(true)
                ServersFactory.start(context, recentObject.url!!, AnimeObject.WebInfo.AnimeChapter.fromRecent(recentObject), false, false, object : ServersFactory.ServersInterface {
                    override fun onFinish(started: Boolean, success: Boolean) {
                        if (started) {
                            recentObject.isChapterDownloaded = true
                            holder.setState(isNetworkAvailable, true)
                        }
                        holder.setLocked(false)
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
                        return view
                    }
                })
            } else if (recentObject.isChapterDownloaded && (obj == null || obj.state == DownloadObject.DOWNLOADING || obj.state == DownloadObject.COMPLETED)) {
                chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(recentObject))
                recordsDAO.add(RecordObject.fromRecent(recentObject))
                holder.setSeen(true)
                ServersFactory.startPlay(context, recentObject.epTitle, recentObject.fileName)
            } else {
                Toaster.toast("Aun no se está descargando")
            }
        }
        holder.download.setOnLongClickListener {
            val obj = downloadsDAO.getByEid(recentObject.eid!!)
            if (CastUtil.get().connected() &&
                    recentObject.isChapterDownloaded && (obj == null || obj.state == DownloadObject.COMPLETED)) {
                chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromRecent(recentObject))
                CastUtil.get().play(fragment.activity as? Activity, view, recentObject.eid!!, SelfServer.start(recentObject.fileName, true), recentObject.name!!, recentObject.chapter!!, recentObject.aid!!, true)
            }
            true
        }
    }

    private fun setOrientation(block: Boolean) {
        noCrash {
            if (block)
                (fragment.activity as? AppCompatActivity)?.requestedOrientation = when {
                    context.resources.getBoolean(R.bool.isLandscape) -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            else (fragment.activity as? AppCompatActivity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }

    override fun onViewRecycled(holder: ItemHolder) {
        holder.unsetObservers()
        super.onViewRecycled(holder)
    }

    internal fun updateList(list: MutableList<RecentObject>, updateListener: UpdateListener) {
        this.isNetworkAvailable = Network.isConnected
        val wasEmpty = this.list.isEmpty()
        this.list = list.distinctBy { it.eid } as MutableList<RecentObject>
        view.post {
            notifyDataSetChanged()
            if (wasEmpty)
                updateListener.invoke()
        }
    }

    override fun getItemId(position: Int): Long {
        return list[position].key.toLong()
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.card
        val imageView: ImageView = itemView.img
        val title: TextView = itemView.title
        val chapter: TextView = itemView.chapter
        val streaming: Button = itemView.streaming
        val download: Button = itemView.download
        val animeOverlay: SeenAnimeOverlay = itemView.seenOverlay
        val downIcon: ImageView = itemView.down_icon
        private val newIcon: ImageView = itemView.new_icon
        private val favIcon: ImageView = itemView.fav_icon
        val progressBar: ProgressBar = itemView.progress
        val progressBarRoot: View = itemView.progress_root

        private var chapterLiveData: LiveData<AnimeObject.WebInfo.AnimeChapter> = MutableLiveData()
        private var downloadLiveData: LiveData<DownloadObject> = MutableLiveData()

        private var chapterObserver: Observer<AnimeObject.WebInfo.AnimeChapter>? = null
        private var downloadObserver: Observer<DownloadObject>? = null
        private var castingObserver: Observer<String>? = null

        fun setChapterObserver(chapterLiveData: LiveData<AnimeObject.WebInfo.AnimeChapter>, owner: LifecycleOwner, observer: Observer<AnimeObject.WebInfo.AnimeChapter>) {
            this.chapterLiveData = chapterLiveData
            this.chapterObserver = observer
            this.chapterLiveData.observe(owner, chapterObserver!!)
        }

        fun unsetChapterObserver() {
            if (chapterObserver != null) {
                chapterLiveData.removeObserver(chapterObserver!!)
                chapterObserver = null
            }
        }

        fun setDownloadObserver(downloadLiveData: LiveData<DownloadObject>, owner: LifecycleOwner, observer: Observer<DownloadObject>) {
            this.downloadLiveData = downloadLiveData
            this.downloadObserver = observer
            this.downloadLiveData.observe(owner, downloadObserver!!)
        }

        fun unsetDownloadObserver() {
            if (downloadObserver != null) {
                downloadLiveData.removeObserver(downloadObserver!!)
                downloadObserver = null
            }
        }

        fun setCastingObserver(owner: LifecycleOwner, observer: Observer<String>) {
            this.castingObserver = observer
            CastUtil.get().casting.observe(owner, castingObserver!!)
        }

        fun unsetCastingObserver() {
            if (castingObserver != null) {
                CastUtil.get().casting.removeObserver(castingObserver!!)
                castingObserver = null
            }
        }

        fun unsetObservers() {
            unsetChapterObserver()
            unsetDownloadObserver()
            unsetCastingObserver()
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

        fun setCasting(casting: Boolean, file_name: String) {
            streaming.post { streaming.text = if (casting) "CAST" else if (FileAccessHelper.INSTANCE.getFile(file_name).exists()) "ELIMINAR" else "STREAMING" }
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

typealias UpdateListener = () -> Unit
