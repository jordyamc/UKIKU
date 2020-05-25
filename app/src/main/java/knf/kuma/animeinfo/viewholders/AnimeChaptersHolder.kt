package knf.kuma.animeinfo.viewholders

import android.net.Uri
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor
import knf.kuma.animeinfo.AnimeChaptersAdapter
import knf.kuma.animeinfo.BottomActionsDialog
import knf.kuma.animeinfo.ktx.filePath
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.*
import knf.kuma.custom.CenterLayoutManager
import knf.kuma.database.CacheDB
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.SeenObject
import knf.kuma.queue.QueueManager
import kotlinx.android.synthetic.main.recycler_chapters.view.*
import org.jetbrains.anko.doAsync
import java.util.*

class AnimeChaptersHolder(view: View, private val fragmentManager: FragmentManager, private val callback: ChapHolderCallback) {
    val recyclerView: RecyclerView = view.recycler
    private val manager: LinearLayoutManager = CenterLayoutManager(view.context)
    private var chapters: MutableList<AnimeObject.WebInfo.AnimeChapter> = ArrayList()
    var adapter: AnimeChaptersAdapter? = null
        private set
    private val touchListener: DragSelectTouchListener

    init {
        manager.isSmoothScrollbarEnabled = true
        recyclerView.layoutManager = manager
        touchListener = DragSelectTouchListener()
                .withSelectListener(DragSelectionProcessor(object : DragSelectionProcessor.ISelectionHandler {
                    override fun getSelection(): Set<Int> {
                        return adapter?.selection ?: setOf()
                    }

                    override fun isSelected(i: Int): Boolean {
                        return adapter?.selection?.contains(i) ?: false
                    }

                    override fun updateSelection(i: Int, i1: Int, b: Boolean, b1: Boolean) {
                        adapter?.selectRange(i, i1, b)
                    }

                }).withStartFinishedListener(object : DragSelectionProcessor.ISelectionStartFinishedListener {
                    override fun onSelectionStarted(i: Int, b: Boolean) {

                    }

                    override fun onSelectionFinished(i: Int) {
                        BottomActionsDialog.newInstance(object : BottomActionsDialog.ActionsCallback {
                            override fun onSelect(state: Int) {
                                try {
                                    val snackbar = recyclerView.showSnackbar("Procesando...", duration = Snackbar.LENGTH_INDEFINITE)
                                    when (state) {
                                        BottomActionsDialog.STATE_SEEN -> doAsync {
                                            val dao = CacheDB.INSTANCE.seenDAO()
                                            for (i13 in ArrayList(adapter?.selection
                                                    ?: arrayListOf())) {
                                                dao.addChapter(SeenObject.fromChapter(chapters[i13]))
                                            }
                                            syncData { seen() }
                                            val seeingDAO = CacheDB.INSTANCE.seeingDAO()
                                            val seeingObject = seeingDAO.getByAid(chapters[0].aid)
                                            if (seeingObject != null) {
                                                seeingObject.chapter = chapters[0].number
                                                seeingDAO.update(seeingObject)
                                                syncData { seeing() }
                                            }
                                            recyclerView.post { adapter?.deselectAll() }
                                            snackbar.safeDismiss()
                                        }
                                        BottomActionsDialog.STATE_UNSEEN -> doAsync {
                                            try {
                                                val dao = CacheDB.INSTANCE.seenDAO()
                                                for (i12 in ArrayList(adapter?.selection
                                                        ?: arrayListOf())) {
                                                    dao.deleteChapter(chapters[i12].aid, chapters[i12].number)
                                                }
                                                syncData { seen() }
                                                val seeingDAO = CacheDB.INSTANCE.seeingDAO()
                                                val seeingObject = seeingDAO.getByAid(chapters[0].aid)
                                                if (seeingObject != null) {
                                                    seeingObject.chapter = chapters[0].number
                                                    seeingDAO.update(seeingObject)
                                                    syncData { seeing() }
                                                }
                                                recyclerView.post { adapter?.deselectAll() }
                                                snackbar.safeDismiss()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                snackbar.safeDismiss()
                                            }
                                        }
                                        BottomActionsDialog.STATE_IMPORT_MULTIPLE -> doAsync {
                                            try {
                                                val cChapters = ArrayList<AnimeObject.WebInfo.AnimeChapter>()
                                                val downloadsDAO = CacheDB.INSTANCE.downloadsDAO()
                                                for (i13 in ArrayList(adapter?.selection
                                                        ?: arrayListOf())) {
                                                    val chapter = chapters[i13]
                                                    val file = FileAccessHelper.findFile(chapter.filePath)
                                                    val downloadObject = downloadsDAO.getByEid(chapter.eid)
                                                    if (!file.exists() && (downloadObject == null || !downloadObject.isDownloading))
                                                        cChapters.add(chapter)
                                                }
                                                callback.onImportMultiple(cChapters)
                                                recyclerView.post { adapter?.deselectAll() }
                                                snackbar.safeDismiss()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                snackbar.safeDismiss()
                                            }
                                        }
                                        BottomActionsDialog.STATE_DOWNLOAD_MULTIPLE -> doAsync {
                                            try {
                                                val cChapters = mutableListOf<AnimeObject.WebInfo.AnimeChapter>()
                                                val downloadsDAO = CacheDB.INSTANCE.downloadsDAO()
                                                for (i13 in ArrayList(adapter?.selection
                                                        ?: arrayListOf())) {
                                                    val chapter = chapters[i13]
                                                    val file = FileAccessHelper.findFile(chapter.filePath)
                                                    val downloadObject = downloadsDAO.getByEid(chapter.eid)
                                                    if (!file.exists() && (downloadObject == null || !downloadObject.isDownloading))
                                                        cChapters.add(chapter)
                                                }
                                                recyclerView.post { adapter?.deselectAll() }
                                                snackbar.safeDismiss()
                                                callback.onDownloadMultiple(false, cChapters)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                snackbar.safeDismiss()
                                            }
                                        }
                                        BottomActionsDialog.STATE_QUEUE_MULTIPLE -> doAsync {
                                            try {
                                                val cChapters = mutableListOf<AnimeObject.WebInfo.AnimeChapter>()
                                                val downloadsDAO = CacheDB.INSTANCE.downloadsDAO()
                                                for (i13 in ArrayList(adapter?.selection
                                                        ?: arrayListOf())) {
                                                    val chapter = chapters[i13]
                                                    val file = FileAccessHelper.findFile(chapter.filePath)
                                                    val downloadObject = downloadsDAO.getByEid(chapter.eid)
                                                    if (!file.exists() && (downloadObject == null || !downloadObject.isDownloading))
                                                        cChapters.add(chapter)
                                                    else if (file.exists() || downloadObject?.isDownloading == true)
                                                        QueueManager.add(Uri.fromFile(file), true, chapter)
                                                }
                                                recyclerView.post { adapter?.deselectAll() }
                                                snackbar.safeDismiss()
                                                callback.onDownloadMultiple(true, cChapters)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                snackbar.safeDismiss()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    //
                                }

                            }

                            override fun onDismiss() {
                                adapter?.deselectAll()
                            }
                        }).safeShow(fragmentManager, "actions_dialog")
                    }
                }).withMode(DragSelectionProcessor.Mode.Simple))
                .withMaxScrollDistance(32)
    }

    fun setAdapter(fragment: Fragment, chapters: MutableList<AnimeObject.WebInfo.AnimeChapter>?) {
        if (chapters == null) return
        this.chapters = chapters
        this.adapter = AnimeChaptersAdapter(fragment, recyclerView, chapters, touchListener)
        recyclerView.post {
            recyclerView.adapter = adapter
            recyclerView.addOnItemTouchListener(touchListener)
        }
    }

    fun refresh() {
        if (adapter != null)
            recyclerView.post { adapter?.notifyDataSetChanged() }
    }

    fun goToChapter() {
        if (chapters.isNotEmpty()) {
            val chapter = CacheDB.INSTANCE.seenDAO().getLast(PatternUtil.getEids(chapters))
            if (chapter != null) {
                val position = chapters.indexOf(chapters.find { it.eid == chapter.eid })
                if (position >= 0)
                    manager.scrollToPositionWithOffset(position, 150)
            }
        }
    }

    fun smoothGoToChapter() {
        if (chapters.isNotEmpty()) {
            doOnUI {
                val chapter = CacheDB.INSTANCE.seenDAO().getAllFrom(PatternUtil.getEids(chapters)).maxBy { noCrashLet(-1.0) { "(\\d+\\.?\\d?)".toRegex().findAll(it.number).last().destructured.component1().toDouble() } }
                if (chapter != null) {
                    val position = chapters.indexOfFirst { it.eid == chapter.eid }
                    if (position >= 0)
                        recyclerView.post { manager.smoothScrollToPosition(recyclerView, null, position) }
                }
            }
        }
    }

    interface ChapHolderCallback {
        fun onImportMultiple(chapters: MutableList<AnimeObject.WebInfo.AnimeChapter>)

        fun onDownloadMultiple(addQueue: Boolean, chapters: List<AnimeObject.WebInfo.AnimeChapter>)
    }
}
