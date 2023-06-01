package knf.kuma.explorer

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import knf.kuma.R
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUI
import knf.kuma.commons.onClickMenu
import knf.kuma.commons.safeShow
import knf.kuma.database.CacheDB
import knf.kuma.download.DownloadManager
import knf.kuma.pojos.DownloadObject
import org.jetbrains.anko.find
import java.util.*

class DownloadingAdapterMaterial internal constructor(private val fragment: Fragment, private val downloadObjects: MutableList<DownloadObject>) : RecyclerView.Adapter<DownloadingAdapterMaterial.DownloadingItem>() {
    private val downloadsDAO = CacheDB.INSTANCE.downloadsDAO()

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): DownloadingItem {
        return DownloadingItem(LayoutInflater.from(viewGroup.context).inflate(R.layout.item_downloading_extra_material, viewGroup, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DownloadingItem, position: Int) {
        val downloadObject = downloadObjects[position]
        holder.server.text = downloadObject.downloadServer
        holder.title.text = downloadObject.name
        holder.chapter.text = downloadObject.chapter
        holder.eta.text = downloadObject.subtext
        holder.progress.max = 100
        if (downloadObject.state == DownloadObject.PENDING) {
            holder.eta.visibility = View.GONE
            holder.progress.isIndeterminate = true
            holder.progress.progress = 0
        } else {
            if (downloadObject.state == DownloadObject.PAUSED)
                holder.eta.visibility = View.GONE
            else
                holder.eta.visibility = View.VISIBLE
            holder.progress.isIndeterminate = false
            holder.progress.progress = downloadObject.progress
        }
        holder.actionMenu.onClickMenu(R.menu.menu_download_options,hideItems = { downloadObject.getDisabledOptions() }){ item ->
            when(item.itemId){
                R.id.pause -> {
                    downloadObject.state = DownloadObject.PAUSED
                    DownloadManager.pause(downloadObject)
                }
                R.id.resume -> {
                    downloadObject.state = DownloadObject.PENDING
                    DownloadManager.resume(downloadObject)
                }
                R.id.cancel -> {
                    fragment.context?.let {
                        MaterialDialog(it).safeShow {
                            message(text = "¿Cancelar descarga del ${downloadObject.chapter.lowercase(Locale.getDefault())} de ${downloadObject.name}?")
                            positiveButton(text = "CONFIRMAR") {
                                try {
                                    downloadObjects.removeAt(holder.adapterPosition)
                                    notifyItemRemoved(holder.adapterPosition)
                                    DownloadManager.cancel(downloadObject.eid)
                                } catch (e: Exception) {
                                    //
                                }
                            }
                            negativeButton(text = "CANCELAR")
                        }
                    }
                }
            }
        }
        downloadsDAO.getLiveByKey(downloadObject.key).observe(fragment, Observer { downloadObject1 ->
            try {
                if (downloadObject1 == null || downloadObject1.state == DownloadObject.COMPLETED) {
                    downloadObjects.removeAt(holder.adapterPosition)
                    notifyItemRemoved(holder.adapterPosition)
                } else {
                    downloadObject.state = downloadObject1.state
                    if (downloadObject1.state == DownloadObject.PENDING) {
                        holder.eta.visibility = View.GONE
                        holder.progress.isIndeterminate = true
                        holder.progress.progress = 0
                    } else {
                        holder.progress.isIndeterminate = false
                        if (downloadObject1.getEta() == -2L || PrefsUtil.downloaderType == 0)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                holder.progress.setProgress(downloadObject1.progress, true)
                            else
                                holder.progress.progress = downloadObject1.progress
                        else {
                            holder.progress.progress = 0
                            holder.progress.secondaryProgress = downloadObject1.progress
                        }
                        holder.eta.visibility = View.VISIBLE
                        holder.eta.text = downloadObject1.subtext
                    }
                }
            } catch (e: Exception) {
                //
            }
        })
    }

    override fun getItemCount(): Int {
        return downloadObjects.size
    }

    fun remove(eid: String) {
        ArrayList(downloadObjects).forEachIndexed { index, downloadObject ->
            if (downloadObject.eid == eid) {
                downloadObjects.removeAt(index)
                fragment.doOnUI { notifyItemRemoved(index) }
                return
            }
        }
    }

    private fun DownloadObject.getDisabledOptions(): List<Int> {
        val list = mutableListOf<Int>()
        if (canResume && (state == DownloadObject.DOWNLOADING || state == DownloadObject.PAUSED)){
            if (state == DownloadObject.DOWNLOADING)
                list.add(R.id.resume)
            if (state == DownloadObject.PAUSED)
                list.add(R.id.pause)
        }else{
            list.add(R.id.resume)
            list.add(R.id.pause)
        }
        return list
    }

    inner class DownloadingItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val server: TextView = itemView.find(R.id.server)
        val title: TextView = itemView.find(R.id.title)
        val chapter: TextView = itemView.find(R.id.chapter)
        val eta: TextView = itemView.find(R.id.eta)
        val actionMenu: View = itemView.find(R.id.actionMenu)
        val progress: ProgressBar = itemView.find(R.id.progress)
    }
}
