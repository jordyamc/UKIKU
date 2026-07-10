package knf.kuma.explorer.creator

import android.util.Log
import knf.kuma.database.CacheDB
import knf.kuma.explorer.ExplorerCreator
import knf.kuma.pojos.ExplorerObject
import knf.kuma.retrofit.Repository
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
            val files = base.listFiles(FileFilter { it.isDirectory && !it.name.startsWith("$")})
            if (files != null) {
                var progress = 0
                return files.mapNotNull {
                    val name = it.name ?: return@mapNotNull null
                    if (it.listFiles()!!.isEmpty()) {
                        it.delete()
                        return@mapNotNull null
                    }
                    val dir = CacheDB.INSTANCE.directoryDAO().findBySlug(name) ?: Repository.getDirectory(name)
                    progress++
                    progressCallback(progress, files.size)
                    try {
                        ExplorerObject(dir!!).also {
                            Log.e("ExplorerCreator", "Found dir: ${it.fileName}")
                            it.file_list.forEach {
                                Log.e("ExplorerCreator", "Found file: ${it.name}")
                            }
                        }
                    } catch (e: ExplorerCreator.DirectoryEmptyException) {
                        e.printStackTrace()
                        it.delete()
                        null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }
            emptyList()
        } else
            emptyList()
    }
}