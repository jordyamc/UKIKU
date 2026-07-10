package knf.kuma.explorer.creator

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import knf.kuma.database.CacheDB
import knf.kuma.pojos.ExplorerObject
import knf.kuma.retrofit.Repository

class DocumentFileCreator(private val rootDF: DocumentFile?) : Creator {

    override fun exist(): Boolean = rootDF?.exists()
            ?: false

    override fun createSlugList(): List<String> {
        rootDF ?: return emptyList()
        return rootDF.listFiles().filter { it.isDirectory }.mapNotNull { it.name }
    }

    override fun createDirectoryList(progressCallback: (Int, Int) -> Unit): List<ExplorerObject> {
        rootDF ?: return emptyList()
        val directories = rootDF.listFiles().filter { it.isDirectory && it.name?.startsWith("$") == false }
        var progress = 0
        return directories.mapNotNull {
            val name = it.name ?: return@mapNotNull null
            if (it.listFiles().isEmpty()) {
                it.delete()
                return@mapNotNull null
            }
            val dir = CacheDB.INSTANCE.directoryDAO().findBySlug(name) ?: Repository.getDirectory(name)
            progress++
            progressCallback(progress, directories.size)
            try {
                ExplorerObject(dir!!).also {
                    Log.e("ExplorerCreator", "Found dir: ${it.fileName}")
                    it.file_list.forEach {
                        Log.e("ExplorerCreator", "Found file: ${it.name}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}