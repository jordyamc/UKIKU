package knf.kuma.explorer.creator

import android.net.Uri
import knf.kuma.database.CacheDB
import knf.kuma.pojos.ExplorerObject
import java.io.File
import java.io.FileFilter

class SimpleFileCreator(val base: File) : Creator {

    override fun exist(): Boolean = base.exists()

    override fun createDirectoryList(progressCallback: (Int, Int) -> Unit): List<ExplorerObject> {
        return if (base.exists()) {
            val list = mutableListOf<ExplorerObject>()
            val files = base.listFiles(FileFilter { it.isDirectory })
            if (files != null) {
                var progress = 0
                for (animeObject in CacheDB.INSTANCE.animeDAO().getAllByFile(files.map { it.name }.toMutableList()))
                    try {
                        progress++
                        progressCallback(progress, files.size)
                        list.add(ExplorerObject(animeObject, createSubFilesList(animeObject.getFinalName())))
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                    }
            }
            list
        } else
            emptyList()
    }

    override fun createSubFilesList(fileName: String): List<SubFile> =
            File(base, fileName).listFiles()?.filter { it.name.endsWith(".mp4") }?.map { SubFile(it.name, Uri.fromFile(it).toString()) }
                    ?: emptyList()
}