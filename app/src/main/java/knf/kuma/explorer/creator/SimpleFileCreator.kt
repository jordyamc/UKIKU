package knf.kuma.explorer.creator

import knf.kuma.database.CacheDB
import knf.kuma.pojos.ExplorerObject
import java.io.File
import java.io.FileFilter

class SimpleFileCreator(val base: File) : Creator {

    override fun exist(): Boolean = base.exists()

    override fun createSlugList(): List<String> {
        return if (base.exists())
            base.listFiles(FileFilter { it.isDirectory })?.map { it.name } ?: emptyList()
        else
            emptyList()
    }

    override fun createDirectoryList(progressCallback: (Int, Int) -> Unit): List<ExplorerObject> {
        return if (base.exists()) {
            val list = mutableListOf<ExplorerObject>()
            val files = base.listFiles(FileFilter { it.isDirectory })
            if (files != null) {
                var progress = 0
                CacheDB.INSTANCE.directoryDAO().findAllBySlug(files.mapNotNull { it.name }).forEach {
                    try {
                        progress++
                        progressCallback(progress, files.size)
                        list.add(ExplorerObject(it))
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                    }
                }
            }
            list
        } else
            emptyList()
    }
}