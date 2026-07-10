package knf.kuma.download

import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import knf.kuma.App
import knf.kuma.commons.FileUtil
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.safeContext
import knf.kuma.download.FileAccessHelper.find
import knf.kuma.download.FileAccessHelper.treeUri
import java.io.File

abstract class UnifiedFile {
    abstract fun name(): String
    abstract fun exist(): Boolean
    abstract fun child(name: String): UnifiedFile
    abstract fun isDirectory(): Boolean
    abstract fun isFile(): Boolean
    abstract fun listFiles(): List<UnifiedFile>
    abstract fun rename(newName: String): Boolean
    abstract fun delete(): Boolean
    abstract fun move(targetPath: String): Boolean
    abstract fun create(isFile: Boolean): Boolean

    companion object {
        fun getRoot(): UnifiedFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            SAFFile(treeUri?.let { find(DocumentFile.fromTreeUri(App.context, it), "UKIKU/downloads", true) }!!)
        } else {
            InternalFile()
        }
    }
}
class InternalFile(file: File? = null): UnifiedFile() {
    val currentFile = file ?: if (PrefsUtil.downloadType == "0") {
        File(Environment.getExternalStorageDirectory(), "UKIKU/downloads")
    } else {
        File(FileUtil.getFullPathFromTreeUri(treeUri, App.context), "UKIKU/downloads")
    }
    override fun name(): String = currentFile.name
    override fun exist(): Boolean = currentFile.exists()
    override fun child(name: String): UnifiedFile = InternalFile(File(currentFile, name))
    override fun isDirectory(): Boolean = currentFile.isDirectory
    override fun isFile(): Boolean = currentFile.isFile
    override fun listFiles(): List<UnifiedFile> = currentFile.listFiles()?.map { InternalFile(it) } ?: emptyList()
    override fun rename(newName: String): Boolean = currentFile.renameTo(File(currentFile.parentFile, newName))
    override fun delete(): Boolean = currentFile.delete()
    override fun move(targetPath: String): Boolean {
        return currentFile.renameTo(File(File(currentFile.parentFile?.parentFile, targetPath), currentFile.name))
    }
    override fun create(isFile: Boolean): Boolean = if (!isFile) {
        currentFile.mkdirs()
    } else {
        currentFile.createNewFile()
    }
}

class SAFFile(parent: DocumentFile, val child: String? = null): UnifiedFile() {
    private var enabled = false
    var currentFile = child?.let {
        parent.findFile(child)?.also {
            enabled = true
        }
    } ?: parent
    override fun name(): String = if (enabled || child == null) currentFile.name ?: "" else child
    override fun exist(): Boolean = if (enabled) { currentFile.exists() } else { child == null }
    override fun child(name: String): UnifiedFile = SAFFile(currentFile, name)
    override fun isDirectory(): Boolean = if (enabled) { currentFile.isDirectory } else { child == null }
    override fun isFile(): Boolean = if (enabled) { currentFile.isFile } else { false }
    override fun listFiles(): List<UnifiedFile> = if (child == null || enabled && isDirectory()) { currentFile.listFiles().map { SAFFile(it.parentFile!!, it.name) } } else emptyList()
    override fun rename(newName: String): Boolean {
        return if (enabled) {
            val result = currentFile.renameTo(newName)
            if (result) {
                currentFile = currentFile.parentFile?.findFile(newName) ?: return false
            }
            result
        } else {
            false
        }
    }
    override fun delete(): Boolean {
        return if (enabled) {
            val result = currentFile.delete()
            if (result) {
                currentFile = currentFile.parentFile ?: return false
                enabled = false
            }
            result
        } else {
            false
        }
    }
    override fun move(targetPath: String): Boolean {
        if (!enabled || child == null || isDirectory()) return false
        val parent = currentFile.parentFile?.parentFile ?: return false
        val targetParent = parent.findFile(targetPath).let {
            it ?: parent.createDirectory(targetPath) ?: return false
        }
        return DocumentsContract.moveDocument(
            safeContext.contentResolver,
            currentFile.uri,
            parent.uri,
            targetParent.uri
        )?.also {
            currentFile = targetParent.findFile(child) ?: return false
        } != null
    }

    override fun create(isFile: Boolean): Boolean {
        if (exist()) return true
        return if (!isFile) {
            val result = currentFile.createDirectory(child!!)?.also {
                enabled = true
                currentFile = it
            }
            result != null
        } else {
            val result = currentFile.createFile("video/mp4", child!!)?.also {
                enabled = true
                currentFile = it
            }
            result != null
        }
    }
}