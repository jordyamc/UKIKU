package knf.kuma.animeinfo.viewholders

import android.net.Uri
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor
import knf.kuma.R
import knf.kuma.animeinfo.AnimeChaptersAdapterMaterial
import knf.kuma.animeinfo.BottomActionsDialog
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.doOnUI
import knf.kuma.commons.safeDismiss
import knf.kuma.commons.showSnackbar
import knf.kuma.custom.CenterLayoutManager
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.Chapter
import knf.kuma.pojos.av1.DirectoryAV1
import knf.kuma.queue.QueueManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find

class AnimeChaptersHolder(view: View, private val fragment: Fragment, private val callback: ChapHolderCallback) {
    val recyclerView: RecyclerView = view.find(R.id.recycler)
    private val manager: LinearLayoutManager = CenterLayoutManager(view.context)
    private var anime: DirectoryAV1? = null
    private var chapters: List<Chapter> = emptyList()
    var adapter: AnimeChaptersAdapterMaterial? = null
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
                        BottomActionsDialog.newInstance(
                            adapter?.selection?.size ?: 0,
                            object : BottomActionsDialog.ActionsCallback {
                                override fun onSelect(state: Int) {
                                    try {
                                        val snackbar = recyclerView.showSnackbar(
                                            "Procesando...",
                                            duration = Snackbar.LENGTH_INDEFINITE
                                        )
                                        when (state) {
                                            BottomActionsDialog.STATE_SEEN -> doAsync {
                                                val dao = CacheDB.INSTANCE.recordAV1DAO()
                                                for (i13 in ArrayList(
                                                    adapter?.selection
                                                        ?: arrayListOf()
                                                )) {
                                                    dao.addChapter(chapters[i13].asRecord(anime!!))
                                                }
                                                fragment.doOnUI {
                                                    adapter?.apply {
                                                        if (selection.isNotEmpty()) {
                                                            selection.forEach {
                                                                chapters[it].isSeen = true
                                                            }
                                                            deselectAll()
                                                        }
                                                    }
                                                }
                                                fragment.doOnUI { snackbar.safeDismiss() }
                                        }
                                        BottomActionsDialog.STATE_UNSEEN -> doAsync {
                                            try {
                                                val dao = CacheDB.INSTANCE.recordAV1DAO()
                                                for (i12 in ArrayList(adapter?.selection
                                                        ?: arrayListOf())) {
                                                    dao.deleteChapter(anime!!.aid, chapters[i12].number)
                                                }
                                                syncData { history() }
                                                fragment.doOnUI {
                                                    adapter?.apply {
                                                        if (selection.isNotEmpty()) {
                                                            selection.forEach {
                                                                this.chapters[it].isSeen = false
                                                            }
                                                            deselectAll()
                                                        }
                                                    }
                                                }
                                                fragment.doOnUI { snackbar.safeDismiss() }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                fragment.doOnUI { snackbar.safeDismiss() }
                                            }
                                        }
                                        BottomActionsDialog.STATE_IMPORT_MULTIPLE -> doAsync {
                                            try {
                                                val cChapters = ArrayList<Chapter>()
                                                val downloadsDAO = CacheDB.INSTANCE.downloadsDAO()
                                                for (i13 in ArrayList(adapter?.selection
                                                        ?: arrayListOf())) {
                                                    val chapter = chapters[i13]
                                                    val downloadObject = downloadsDAO.getByEid(chapter.eid.toString())
                                                    if (!chapter.fileWrapper(anime!!).exist && (downloadObject == null || !downloadObject.isDownloading))
                                                        cChapters.add(chapter)
                                                }
                                                callback.onImportMultiple(cChapters)
                                                recyclerView.post { adapter?.deselectAll() }
                                                fragment.doOnUI { snackbar.safeDismiss() }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                fragment.doOnUI { snackbar.safeDismiss() }
                                            }
                                        }
                                        BottomActionsDialog.STATE_DOWNLOAD_MULTIPLE -> doAsync {
                                            try {
                                                val cChapters = mutableListOf<Chapter>()
                                                val downloadsDAO = CacheDB.INSTANCE.downloadsDAO()
                                                for (i13 in ArrayList(adapter?.selection
                                                        ?: arrayListOf())) {
                                                    val chapter = chapters[i13]
                                                    val downloadObject = downloadsDAO.getByEid(chapter.eid.toString())
                                                    if (!chapter.fileWrapper(anime!!).exist && (downloadObject == null || !downloadObject.isDownloading))
                                                        cChapters.add(chapter)
                                                }
                                                recyclerView.post { adapter?.deselectAll() }
                                                fragment.doOnUI { snackbar.safeDismiss() }
                                                callback.onDownloadMultiple(false, cChapters)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                fragment.doOnUI { snackbar.safeDismiss() }
                                            }
                                        }
                                        BottomActionsDialog.STATE_QUEUE_MULTIPLE -> doAsync {
                                            try {
                                                val cChapters = mutableListOf<Chapter>()
                                                val downloadsDAO = CacheDB.INSTANCE.downloadsDAO()
                                                for (i13 in ArrayList(adapter?.selection
                                                        ?: arrayListOf())) {
                                                    val chapter = chapters[i13]
                                                    val downloadObject = downloadsDAO.getByEid(chapter.eid.toString())
                                                    val fileWrapper = chapter.fileWrapper(anime!!)
                                                    if (!fileWrapper.exist && (downloadObject == null || !downloadObject.isDownloading))
                                                        cChapters.add(chapter)
                                                    else if (fileWrapper.exist || downloadObject?.isDownloading == true)
                                                        QueueManager.add(Uri.fromFile(fileWrapper.file()), true, anime!!, chapter)
                                                }
                                                recyclerView.post { adapter?.deselectAll() }
                                                fragment.doOnUI { snackbar.safeDismiss() }
                                                callback.onDownloadMultiple(true, cChapters)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                fragment.doOnUI { snackbar.safeDismiss() }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    //
                                }

                            }

                            override fun onDismiss() {
                                recyclerView.post { adapter?.deselectAll() }
                            }
                        }).safeShow(fragment.childFragmentManager, "actions_dialog")
                    }
                }).withMode(DragSelectionProcessor.Mode.Simple))
                .withMaxScrollDistance(32)
    }

    fun setAdapter(fragment: Fragment, anime: DirectoryAV1, chapters: List<Chapter>) {
        if (chapters.isEmpty()) return
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            this@AnimeChaptersHolder.anime = anime
            this@AnimeChaptersHolder.chapters = chapters
            this@AnimeChaptersHolder.adapter = AnimeChaptersAdapterMaterial(fragment, recyclerView, anime, chapters, touchListener, false)
            recyclerView.post {
                recyclerView.adapter = adapter
                recyclerView.addOnItemTouchListener(touchListener)
            }
        }
    }

    fun refresh() {
        if (adapter != null)
            recyclerView.post { adapter?.notifyDataSetChanged() }
    }

    fun goToChapter() {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            if (chapters.isNotEmpty()) {
                val eids =
                    chapters.sortedBy { it.number }.map { it.eid }
                eids.chunked(50).forEach { list ->
                    val chapter = CacheDB.INSTANCE.recordAV1DAO().getLast(list)
                    if (chapter != null) {
                        val position = chapters.indexOf(chapters.find { it.eid == chapter.eid })
                        if (position >= 0)
                            launch(Dispatchers.Main) {
                                manager.scrollToPositionWithOffset(position, 150)
                            }
                        return@forEach
                    }
                }
            }
        }
    }

    fun smoothGoToChapter() {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            if (chapters.isNotEmpty()) {
                val eids =
                    chapters.sortedBy { it.number }.map { it.eid }
                eids.chunked(50).forEach { list ->
                    val chapter = CacheDB.INSTANCE.recordAV1DAO().getLast(list)
                    if (chapter != null) {
                        val position = chapters.indexOf(chapters.find { it.eid == chapter.eid })
                        if (position >= 0)
                            recyclerView.post {
                                manager.smoothScrollToPosition(
                                    recyclerView,
                                    null,
                                    position
                                )
                            }
                        return@forEach
                    }
                }
            }
        }
    }

    interface ChapHolderCallback {
        fun onImportMultiple(chapters: MutableList<Chapter>)

        fun onDownloadMultiple(addQueue: Boolean, chapters: List<Chapter>)
    }
}
