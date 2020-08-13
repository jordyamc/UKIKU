package knf.kuma.commons

import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import knf.kuma.App
import knf.kuma.download.FileAccessHelper
import java.io.File
import java.io.InputStream

abstract class FileWrapper<T>(val path: String) {

    abstract var exist: Boolean
    abstract fun existForced(): Boolean
    abstract fun file(): File?
    abstract fun name(): String
    abstract fun length(): Long?
    abstract fun lastModified(): Long?
    abstract fun inputStream(): InputStream?
    abstract fun parentSize(): Int
    abstract fun generate(): T
    abstract fun reset()

    companion object {
        fun create(file_name: String?): FileWrapper<*> {
            file_name ?: throw IllegalStateException("Path can't be null!")
            return when {
                PrefsUtil.downloadType == "0" -> NormalFileWrapper(file_name)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> DocumentFileWrapper(file_name)
                else -> NormalPreQFileWrapper(file_name)
            }
        }

        fun fromFileName(file_name: String): File =
                File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
    }
}

class NormalFileWrapper(path: String) : FileWrapper<File?>(path) {
    private var mFile = generate()
    override var exist = mFile?.exists() == true
    override fun existForced(): Boolean {
        reset()
        return exist
    }
    override fun file(): File? = mFile
    override fun name(): String = mFile?.name ?: path
    override fun length(): Long? = mFile?.length()
    override fun lastModified(): Long? = mFile?.lastModified()
    override fun inputStream(): InputStream? = mFile?.inputStream()
    override fun parentSize(): Int = mFile?.parentFile?.list()?.size ?: 0
    override fun generate(): File? = File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(path)).listFiles { file -> file.name.contains(path) }?.let {
        if (it.isNotEmpty())
            it[0]
        else
            null
    }

    override fun reset() {
        mFile = generate()
        exist = mFile?.exists() == true
    }
}

class NormalPreQFileWrapper(path: String) : FileWrapper<File?>(path) {
    private var mFile = generate()
    override var exist = mFile?.exists() == true
    override fun existForced(): Boolean {
        reset()
        return exist
    }
    override fun file(): File? = mFile
    override fun name(): String = mFile?.name ?: path
    override fun length(): Long? = mFile?.length()
    override fun lastModified(): Long? = mFile?.lastModified()
    override fun inputStream(): InputStream? = mFile?.inputStream()
    override fun parentSize(): Int = mFile?.parentFile?.list()?.size ?: 0
    override fun generate(): File? = File(FileUtil.getFullPathFromTreeUri(FileAccessHelper.treeUri, App.context), "UKIKU/downloads/" + PatternUtil.getNameFromFile(path)).listFiles { file -> file.name.contains(path) }?.let {
        if (it.isNotEmpty())
            it[0]
        else
            null
    }

    override fun reset() {
        mFile = generate()
        exist = mFile?.exists() == true
    }
}

class DocumentFileWrapper(path: String) : FileWrapper<DocumentFile?>(path) {
    private var document = generate()
    override var exist = document?.exists() == true
    override fun existForced(): Boolean {
        reset()
        return exist
    }
    override fun file(): File? = FileUtil.getFullPathFromTreeUri(document?.uri, App.context)?.let { File(it) }
    override fun name(): String = document?.name ?: path
    override fun length(): Long? = document?.length()
    override fun lastModified(): Long? = document?.lastModified()
    override fun inputStream(): InputStream? = document?.let { App.context.contentResolver.openInputStream(it.uri) }
    override fun parentSize(): Int = document?.parentFile?.listFiles()?.size ?: 0
    override fun generate(): DocumentFile? = FileAccessHelper.treeUri?.let { uri ->
        FileAccessHelper.find(DocumentFile.fromTreeUri(App.context, uri), "UKIKU/downloads/" + PatternUtil.getNameFromFile(path), false)?.listFiles()?.let { list ->
            var file: DocumentFile? = null
            list.forEach {
                if (it.name?.contains(path) == true) {
                    file = it
                    return@forEach
                }
            }
            file
        }
    }

    override fun reset() {
        document = generate()
        exist = document?.exists() == true
    }
}