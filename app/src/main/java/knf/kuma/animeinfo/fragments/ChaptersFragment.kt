package knf.kuma.animeinfo.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import knf.kuma.App
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.animeinfo.AnimeViewModel
import knf.kuma.animeinfo.ktx.fileName
import knf.kuma.animeinfo.viewholders.AnimeChaptersHolder
import knf.kuma.commons.*
import knf.kuma.custom.snackbar.SnackProgressBar
import knf.kuma.custom.snackbar.SnackProgressBarManager
import knf.kuma.download.FileAccessHelper
import knf.kuma.download.MultipleDownloadManager
import knf.kuma.jobscheduler.DirUpdateWork
import knf.kuma.pojos.AnimeObject
import xdroid.toaster.Toaster
import java.util.*
import java.util.regex.Pattern

class ChaptersFragment : BottomFragment(), AnimeChaptersHolder.ChapHolderCallback {
    private var holder: AnimeChaptersHolder? = null
    private var moveFile: String? = null
    private var chapters: MutableList<AnimeObject.WebInfo.AnimeChapter> = ArrayList()
    private lateinit var snackManager: SnackProgressBarManager

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.let {
            ViewModelProvider(it).get(AnimeViewModel::class.java).liveData.observe(viewLifecycleOwner, Observer { animeObject ->
                if (animeObject != null) {
                    val chapters = animeObject.chapters
                    chapters?.let {
                        if (checkIntegrity(chapters)) {
                            if (PrefsUtil.isChapsAsc)
                                chapters.reverse()
                            holder?.setAdapter(this@ChaptersFragment, chapters)
                            holder?.goToChapter()
                        } else if (Network.isConnected) {
                            DirUpdateWork.runNow()
                            "Integridad de directorio comprometida, actualizando directorio...".toast()
                        }
                        null
                    }
                }
            })
        }
    }

    private fun checkIntegrity(list: List<AnimeObject.WebInfo.AnimeChapter>): Boolean {
        return try {
            list.isEmpty() || (list[0].aid != null && list[0].eid != null)
        } catch (e: Exception) {
            false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.recycler_chapters, container, false)
        holder = AnimeChaptersHolder(view, childFragmentManager, this).also {
            snackManager = SnackProgressBarManager(it.recyclerView)
                    .setProgressBarColor(EAHelper.getThemeColor())
                    .setOverlayLayoutAlpha(0.4f)
                    .setOverlayLayoutColor(android.R.color.background_dark)
        }
        return view
    }

    override fun onReselect() {
        holder?.smoothGoToChapter()
    }

    fun onMove(to: String) {
        this.moveFile = to
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .setType("video/mp4"), 55698)
    }

    override fun onImportMultiple(chapters: MutableList<AnimeObject.WebInfo.AnimeChapter>) {
        when (chapters.size) {
            0 -> Toaster.toast("No se puede importar ningun episodio")
            1 -> {
                this.moveFile = chapters[0].fileName
                onMove(chapters[0].fileName)
            }
            else -> {
                this.chapters = chapters
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        .setType("video/mp4"), 55698)
            }
        }
    }

    override fun onDownloadMultiple(addQueue: Boolean, chapters: List<AnimeObject.WebInfo.AnimeChapter>) {
        activity?.let {
            holder?.let { holder ->
                MultipleDownloadManager.startDownload(it, holder.recyclerView, chapters.sortedBy { noCrashLet(9999) { "(\\d+)".toRegex().findAll(it.number).last().destructured.component1().toInt() } }, addQueue)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK)
            try {
                holder?.adapter?.isImporting = true
                if (data?.clipData == null || data.clipData?.itemCount ?: 0 == 0) {
                    if (moveFile == null && chapters.size > 0) {
                        val uri = data?.data
                        val file = DocumentFile.fromSingleUri(App.context, uri ?: Uri.EMPTY)
                        val last = getLastNumber(file?.name)
                        moveFile = findChapter(last)?.fileName
                    }
                    val snackbar = SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, "Importando...")
                            .setIsIndeterminate(false)
                            .setProgressMax(100)
                            .setShowProgressPercentage(true)
                    snackManager.show(snackbar, SnackProgressBarManager.LENGTH_INDEFINITE)
                    FileUtil.moveFile(App.context.contentResolver, data?.data, FileAccessHelper.getOutputStream(moveFile)).observe(this, Observer { pair ->
                        try {
                            if (pair != null) {
                                if (pair.second) {
                                    if (pair.first == -1) {
                                        Toaster.toast("Error al importar")
                                        FileAccessHelper.delete(moveFile)
                                    } else
                                        Toaster.toast("Importado exitosamente")
                                    holder?.adapter?.notifyDataSetChanged()
                                    moveFile = null
                                    snackManager.dismiss()
                                    holder?.adapter?.isImporting = false
                                } else
                                    snackManager.setProgress(pair.first)
                            }
                        } catch (e: Exception) {
                            Toaster.toast("Error al importar")
                        }
                    })
                } else {
                    val snackbar = SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, "Importando...")
                            .setIsIndeterminate(false)
                            .setProgressMax(100)
                            .setShowProgressPercentage(true)
                    snackManager.show(snackbar, SnackProgressBarManager.LENGTH_INDEFINITE)
                    val moveRequests = ArrayList<Pair<Uri, String>>()
                    val count = data.clipData?.itemCount ?: 0
                    for (i in 0 until count) {
                        try {
                            val uri = data.clipData?.getItemAt(i)?.uri ?: Uri.EMPTY
                            val file = DocumentFile.fromSingleUri(App.context, uri)
                            val last = getLastNumber(file?.name)
                            moveRequests.add(Pair(uri, findChapter(last)?.fileName ?: ""))
                        } catch (e: Exception) {
                            //
                        }
                    }
                    if (moveRequests.size == 0) {
                        Toaster.toast("No se pudo inferir el numero de los episodios")
                        snackManager.dismiss()
                    } else {
                        FileUtil.moveFiles(App.context.contentResolver, moveRequests).observe(this@ChaptersFragment, Observer { pairBooleanPair ->
                            try {
                                if (pairBooleanPair != null) {
                                    if (pairBooleanPair.second) {
                                        Toaster.toast("Importados ${pairBooleanPair.first.second} archivos exitosamente")
                                        holder?.adapter?.notifyDataSetChanged()
                                        chapters = ArrayList()
                                        snackManager.dismiss()
                                        holder?.adapter?.isImporting = false
                                    } else {
                                        snackbar.setMessage(pairBooleanPair.first.first)
                                        snackManager.updateTo(snackbar)
                                        snackManager.setProgress(pairBooleanPair.first.second)
                                    }
                                }
                            } catch (e: Exception) {
                                Toaster.toast("Error al importar")
                            }
                        })
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toaster.toast("Error al importar")
            }

    }

    private fun findChapter(num: String?): AnimeObject.WebInfo.AnimeChapter? {
        for (c in ArrayList<AnimeObject.WebInfo.AnimeChapter>(chapters)) {
            if (c.number == "Episodio $num") {
                chapters.remove(c)
                return c
            }
        }
        return null
    }

    private fun getLastNumber(name: String?): String? {
        if (name.isNullOrEmpty()) return null
        val matcher = Pattern.compile(".*[_ ]0?(\\d+)[_ ].*$|0?(\\d+)$").matcher(name.replace(".mp4", ""))
        var last: String? = null
        while (matcher.find()) {
            try {
                last = matcher.group(1)
                if (last == null)
                    last = matcher.group(2)
            } catch (e: Exception) {
                try {
                    last = matcher.group(2)
                } catch (e1: Exception) {
                    e1.printStackTrace()
                }
            }
        }
        return last
    }

    companion object {

        fun get(): ChaptersFragment {
            return ChaptersFragment()
        }
    }
}
