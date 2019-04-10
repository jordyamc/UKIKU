package knf.kuma.explorer

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.provider.MediaStore.Video.Thumbnails.MINI_KIND
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.card.MaterialCardView
import knf.kuma.R
import knf.kuma.cast.CastMedia
import knf.kuma.commons.*
import knf.kuma.custom.SeenAnimeOverlay
import knf.kuma.database.CacheDB
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.ExplorerObject
import knf.kuma.pojos.RecordObject
import knf.kuma.queue.QueueManager
import knf.kuma.videoservers.ServersFactory
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.FileOutputStream
import java.util.*


class ExplorerChapsAdapter internal constructor(fragment: Fragment, private val recyclerView: RecyclerView, private val explorerObject: ExplorerObject, private var clearInterface: FragmentChapters.ClearInterface?) : RecyclerView.Adapter<ExplorerChapsAdapter.ChapItem>() {
    private val context: Context? = fragment.context

    private val downloadsDAO = CacheDB.INSTANCE.downloadsDAO()
    private val chaptersDAO = CacheDB.INSTANCE.chaptersDAO()
    private val recordsDAO = CacheDB.INSTANCE.recordsDAO()
    private val explorerDAO = CacheDB.INSTANCE.explorerDAO()

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            R.layout.item_chap
        } else {
            R.layout.item_chap_grid
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapItem {
        return ChapItem(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: ChapItem, position: Int) {
        val chapObject = explorerObject.chapters[position]
        loadThumb(chapObject, holder.imageView)
        holder.seenOverlay.setSeen(chaptersDAO.chapterIsSeen(chapObject.eid), false)
        holder.chapter.text = String.format(Locale.getDefault(), "Episodio %s", chapObject.chapter)
        holder.time.text = chapObject.time
        holder.cardView.setOnClickListener {
            chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromDownloaded(chapObject))
            recordsDAO.add(RecordObject.fromDownloaded(chapObject))
            holder.seenOverlay.setSeen(true, true)
            if (CastUtil.get().connected()) {
                CastUtil.get().play(recyclerView, CastMedia.create(chapObject))
            } else {
                ServersFactory.startPlay(context, chapObject.chapTitle, chapObject.fileName)
            }
        }
        holder.cardView.setOnLongClickListener {
            if (!chaptersDAO.chapterIsSeen(chapObject.eid)) {
                chaptersDAO.addChapter(AnimeObject.WebInfo.AnimeChapter.fromDownloaded(chapObject))
                holder.seenOverlay.setSeen(true, true)
            } else {
                chaptersDAO.deleteChapter(AnimeObject.WebInfo.AnimeChapter.fromDownloaded(chapObject))
                holder.seenOverlay.setSeen(false, true)
            }
            true
        }
        holder.action.setOnClickListener {
            context?.let {
                MaterialDialog(context).safeShow {
                    message(text = "¿Eliminar el episodio ${chapObject.chapter} de ${chapObject.title}?")
                    positiveButton(text = "CONFIRMAR") {
                        delete(chapObject, holder.adapterPosition)
                    }
                    negativeButton(text = "CANCELAR")
                }
            }
        }
    }

    fun setInterface(clearInterface: FragmentChapters.ClearInterface) {
        this.clearInterface = clearInterface
    }

    private fun delete(obj: ExplorerObject.FileDownObj, position: Int) {
        if (position < 0) return
        FileAccessHelper.INSTANCE.delete(obj.fileName, true)
        downloadsDAO.deleteByEid(obj.eid)
        QueueManager.remove(obj.eid)
        explorerObject.chapters.removeAt(position)
        doOnUI { notifyItemRemoved(position) }
        if (explorerObject.chapters.size == 0) {
            explorerDAO.delete(explorerObject)
            clearInterface?.onClear()
        } else {
            explorerObject.count = explorerObject.chapters.size
            explorerDAO.update(explorerObject)
        }
    }

    internal fun deleteAll() {
        doAsync {
            for ((i, obj) in explorerObject.chapters.withIndex()) {
                FileAccessHelper.INSTANCE.delete(obj.fileName, true)
                downloadsDAO.deleteByEid(obj.eid)
                QueueManager.remove(obj.eid)
                doOnUI {
                    notifyItemRemoved(i)
                }
            }
            explorerDAO.delete(explorerObject)
            clearInterface?.onClear()
        }
    }

    private fun loadThumb(fileDownObj: ExplorerObject.FileDownObj, imageView: ImageView?) {
        val file = File(context?.cacheDir, explorerObject.fileName + "_" + fileDownObj.chapter.toLowerCase() + ".png")
        if (file.exists()) {
            fileDownObj.thumb = file
            PicassoSingle.get().load(file).into(imageView)
        } else {
            doAsync {
                try {
                    val bitmap = ThumbnailUtils.createVideoThumbnail(fileDownObj.path, MINI_KIND)
                    if (bitmap == null) {
                        throw IllegalStateException("Null bitmap")
                    } else {
                        file.createNewFile()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
                        fileDownObj.thumb = file
                        doOnUI { PicassoSingle.get().load(file).into(imageView) }
                    }
                } catch (e: Exception) {
                    doOnUI { PicassoSingle.get().load(R.drawable.ic_no_thumb).fit().into(imageView) }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return try {
            explorerObject.chapters.size
        } catch (e: Exception) {
            0
        }

    }

    inner class ChapItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val seenOverlay: SeenAnimeOverlay by itemView.bind(R.id.seen)
        val chapter: TextView by itemView.bind(R.id.chapter)
        val time: TextView by itemView.bind(R.id.time)
        val action: ImageButton by itemView.bind(R.id.action)
    }
}
