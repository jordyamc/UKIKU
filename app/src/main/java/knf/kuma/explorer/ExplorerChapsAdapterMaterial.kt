package knf.kuma.explorer

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore.Video.Thumbnails.MINI_KIND
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import knf.kuma.App
import knf.kuma.R
import knf.kuma.backup.firestore.syncData
import knf.kuma.cast.CastMedia
import knf.kuma.commons.*
import knf.kuma.custom.SeenAnimeOverlay
import knf.kuma.database.CacheDB
import knf.kuma.download.FileAccessHelper
import knf.kuma.pojos.ExplorerObject
import knf.kuma.pojos.RecordObject
import knf.kuma.pojos.SeenObject
import knf.kuma.queue.QueueManager
import knf.kuma.videoservers.ServersFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.FileOutputStream
import java.util.*


class ExplorerChapsAdapterMaterial internal constructor(val fragment: Fragment, private val recyclerView: RecyclerView, val explorerObject: ExplorerObjectWrap, private var clearInterface: FragmentChaptersMaterial.ClearInterface?) : RecyclerView.Adapter<ExplorerChapsAdapterMaterial.ChapItem>() {
    private val context: Context? = fragment.context

    private val downloadsDAO = CacheDB.INSTANCE.downloadsDAO()
    private val chaptersDAO = CacheDB.INSTANCE.seenDAO()
    private val recordsDAO = CacheDB.INSTANCE.recordsDAO()
    private val explorerDAO = CacheDB.INSTANCE.explorerDAO()

    private val layout: Int
        @LayoutRes
        get() = if (PrefsUtil.layType == "0") {
            R.layout.item_chap_material
        } else {
            R.layout.item_chap_grid_material
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapItem {
        return ChapItem(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: ChapItem, position: Int) {
        val chapObject = explorerObject.fileList[position]
        loadThumb(chapObject.obj, holder.imageView)
        val chapterNum = String.format(Locale.getDefault(), "Episodio %s", chapObject.obj.chapter)
        holder.seenOverlay.setSeen(chapObject.isSeen, false)
        holder.chapter.text = chapterNum
        holder.time.text = chapObject.obj.time
        holder.cardView.setOnClickListener {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                chaptersDAO.addChapter(SeenObject.fromDownloaded(chapObject.obj))
                recordsDAO.add(RecordObject.fromDownloaded(chapObject.obj))
            }
            chapObject.isSeen = true
            syncData {
                history()
                seen()
            }
            holder.seenOverlay.setSeen(true, true)
            if (CastUtil.get().connected()) {
                CastUtil.get().play(recyclerView, CastMedia.create(chapObject.obj))
            } else {
                ServersFactory.startPlay(context, chapObject.obj.chapTitle, chapObject.obj.fileName)
            }
        }
        holder.cardView.setOnLongClickListener {
            if (!chapObject.isSeen) {
                fragment.lifecycleScope.launch(Dispatchers.IO){
                    chaptersDAO.addChapter(SeenObject.fromDownloaded(chapObject.obj))
                }
                chapObject.isSeen = true
                holder.seenOverlay.setSeen(true, true)
            } else {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    chaptersDAO.deleteChapter(chapObject.obj.aid, chapterNum)
                }
                chapObject.isSeen = false
                holder.seenOverlay.setSeen(false, true)
            }
            syncData { seen() }
            true
        }
        holder.action.setOnClickListener {
            context?.let {
                MaterialDialog(context).safeShow {
                    message(text = "¿Eliminar el episodio ${chapObject.obj.chapter} de ${chapObject.obj.title}?")
                    positiveButton(text = "CONFIRMAR") {
                        delete(chapObject.obj, holder.adapterPosition)
                    }
                    negativeButton(text = "CANCELAR")
                }
            }
        }
    }

    fun setInterface(clearInterface: FragmentChaptersMaterial.ClearInterface) {
        this.clearInterface = clearInterface
    }

    private fun delete(obj: ExplorerObject.FileDownObj, position: Int) {
        if (position < 0) return
        doAsync {
            FileAccessHelper.delete(obj.fileName, true)
            downloadsDAO.deleteByEid(obj.eid)
            QueueManager.remove(obj.eid)
            explorerObject.fileList.removeAt(position)
            doOnUI { notifyItemRemoved(position) }
            if (explorerObject.fileList.size == 0) {
                explorerDAO.delete(explorerObject.obj)
                clearInterface?.onClear()
            } else {
                explorerObject.obj.count = explorerObject.fileList.size
                explorerDAO.update(explorerObject.obj)
            }
        }
    }

    internal fun deleteAll() {
        doAsync {
            for ((i, obj) in explorerObject.fileList.withIndex()) {
                FileAccessHelper.delete(obj.obj.fileName, true)
                downloadsDAO.deleteByEid(obj.obj.eid)
                QueueManager.remove(obj.obj.eid)
                doOnUI {
                    notifyItemRemoved(i)
                }
            }
            explorerDAO.delete(explorerObject.obj)
            clearInterface?.onClear()
        }
    }

    private fun loadThumb(fileDownObj: ExplorerObject.FileDownObj, imageView: ImageView?) {
        val file = File(context?.cacheDir, explorerObject.obj.fileName + "_" + fileDownObj.chapter.toLowerCase() + ".png")
        if (file.exists()) {
            fileDownObj.thumb = file
            PicassoSingle.get().load(file).into(imageView)
        } else {
            doAsync {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        MediaMetadataRetriever().apply {
                            setDataSource(App.context, fileDownObj.file.getFileUri())
                        }.frameAtTime
                    else
                        ThumbnailUtils.createVideoThumbnail(File(fileDownObj.file.getFileUri().path).absolutePath, MINI_KIND)
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
            explorerObject.fileList.size
        } catch (e: Exception) {
            0
        }

    }

    inner class ChapItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val seenOverlay: SeenAnimeOverlay by itemView.bind(R.id.seen)
        val chapter: TextView by itemView.bind(R.id.chapter)
        val time: TextView by itemView.bind(R.id.time)
        val action: ImageButton by itemView.bind(R.id.action)
    }
}
