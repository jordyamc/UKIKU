package knf.kuma.download

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import knf.kuma.commons.FileUtil
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.getPackage
import knf.kuma.commons.isNull
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.io.*

typealias DeleteListener = () -> Unit


class FileAccessHelper private constructor(private val context: Context) {

    val downloadsDirectory: File
        get() {
            return try {
                if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0") == "0") {
                    File(Environment.getExternalStorageDirectory(), "UKIKU/downloads")
                } else {
                    File(FileUtil.getFullPathFromTreeUri(treeUri, context), "UKIKU/downloads")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Environment.getDataDirectory()
            }

        }

    private val treeUri: Uri?
        get() {
            return try {
                Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString("tree_uri", ""))
            } catch (e: Exception) {
                null
            }

        }

    fun getFile(file_name: String?): File {
        return try {
            if (file_name.isNullOrEmpty()) throw IllegalStateException("Name can't be null!")
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0") == "0") {
                File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
            } else {
                File(FileUtil.getFullPathFromTreeUri(treeUri, context), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            File(Environment.getDataDirectory(), "test.txt")
        }

    }

    fun getTmpFile(file_name: String): File {
        return File(FileUtil.getFullPathFromTreeUri(treeUri, context), "Android/data/${getPackage()}/files/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
    }

    val toneFile: File
        get() {
            return File(context.getExternalFilesDir(null), "custom_tone")
        }

    fun setToneFile(enable: Boolean) {
        if (!enable) toneFile.delete()
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("is_custom_tone", enable).apply()
    }

    fun getFileCreate(file_name: String): File? {
        return try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0") == "0") {
                val file = File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
                file.parentFile.mkdirs()
                if (!file.exists())
                    file.createNewFile()
                file
            } else {
                createTmpIfNotExist()
                val file = File(FileUtil.getFullPathFromTreeUri(treeUri, context), "Android/data/${getPackage()}/files/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
                file.parentFile.mkdirs()
                if (!file.exists())
                    file.createNewFile()
                file
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    }

    internal fun isTempFile(file: String): Boolean {
        return try {
            val path = FileUtil.getFullPathFromTreeUri(treeUri, context) ?: return false
            file.contains(path)
        } catch (e: Exception) {
            false
        }
    }

    fun checkNoMedia(noMediaNeeded: Boolean) {
        NOMEDIA_CREATING = true
        doAsync {
            try {
                val file = File(Environment.getExternalStorageDirectory(), "UKIKU/downloads")
                if (!file.exists())
                    file.mkdirs()
                val root = File(file, ".nomedia")
                if (noMediaNeeded && !root.exists())
                    root.createNewFile()
                else if (!noMediaNeeded && root.exists())
                    root.delete()
                val list = file.listFiles(FileFilter { it.isDirectory })
                if (list != null && list.isNotEmpty())
                    for (current in list) {
                        val inside = File(current, ".nomedia")
                        if (noMediaNeeded && !inside.exists())
                            inside.createNewFile()
                        else if (!noMediaNeeded && inside.exists())
                            inside.delete()
                    }
                treeUri?.let {
                    val documentRoot = find(DocumentFile.fromTreeUri(context, it), "UKIKU/downloads")
                    val nomediaRoot = documentRoot?.findFile(".nomedia")
                    if (noMediaNeeded && (nomediaRoot == null || !nomediaRoot.exists()))
                        documentRoot?.createFile("application/nomedia", ".nomedia")
                    else if (!noMediaNeeded && nomediaRoot != null && nomediaRoot.exists())
                        nomediaRoot.delete()
                    val documentList = documentRoot?.listFiles()
                    if (!documentList.isNullOrEmpty())
                        for (dFile in documentList) {
                            if (dFile.isDirectory) {
                                val inside = dFile.findFile(".nomedia")
                                if (noMediaNeeded && (inside == null || !inside.exists()))
                                    dFile.createFile("application/nomedia", ".nomedia")
                                else if (!noMediaNeeded && inside != null && inside.exists())
                                    inside.delete()
                            }
                        }
                }
                Toaster.toast("Archivos nomedia " + if (noMediaNeeded) "creados" else "eliminados")
                NOMEDIA_CREATING = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getDownloadsDirectory(file_name: String): File {
        return try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0") == "0") {
                File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/$file_name")
            } else {
                File(FileUtil.getFullPathFromTreeUri(treeUri, context), "UKIKU/downloads/$file_name")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Environment.getDataDirectory()
        }

    }

    fun getDownloadsDirectoryFromFile(file_name: String): File {
        return try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0") == "0") {
                File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/${PatternUtil.getNameFromFile(file_name)}")
            } else {
                File(FileUtil.getFullPathFromTreeUri(treeUri, context), "UKIKU/downloads/${PatternUtil.getNameFromFile(file_name)}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Environment.getDataDirectory()
        }

    }

    fun delete(file_name: String?, async: Boolean = true) {
        if (async)
            doAsync { delete(file_name) }
        else
            delete(file_name)
    }

    private fun delete(file_name: String?) {
        if (file_name.isNull())
            return
        try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0") == "0") {
                val file = File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
                file.delete()
                val dir = file.parentFile
                if (dir.listFiles() == null || dir.listFiles().isEmpty())
                    dir.delete()
            } else {
                treeUri?.let {
                    val documentFile = DocumentFile.fromTreeUri(context, it)
                    if (documentFile != null && documentFile.exists()) {
                        val file = find(documentFile, "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
                        file?.delete()
                        val dir = file?.parentFile
                        if (dir != null && dir.listFiles().isEmpty())
                            dir.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createTmpIfNotExist() {
        if (!File(FileUtil.getFullPathFromTreeUri(treeUri, context), "Android/data/${getPackage()}").exists()) {
            treeUri?.let {
                val documentFile = DocumentFile.fromTreeUri(context, it)
                if (documentFile != null && documentFile.exists()) {
                    val file = find(documentFile, "Android/data/${getPackage()}/files/downloads/tmp.file")
                    file?.delete()
                }
            }
        }
    }

    fun getOutputStream(file_name: String?): OutputStream? {
        if (file_name == null) return null
        try {
            return if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0") == "0") {
                var file = File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name))
                if (!file.exists())
                    file.mkdirs()
                file = File(file, file_name)
                if (!file.exists())
                    file.createNewFile()
                FileOutputStream(File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name))
            } else {
                treeUri?.let {
                    context.contentResolver.openOutputStream(find(DocumentFile.fromTreeUri(context, it), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)?.uri
                            ?: Uri.EMPTY, "rw")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

    fun getFileOutputStream(file_name: String): FileOutputStream? {
        try {
            return if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0") == "0") {
                var file = File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name))
                if (!file.exists())
                    file.mkdirs()
                file = File(file, file_name)
                if (!file.exists())
                    file.createNewFile()
                FileOutputStream(File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name))
            } else {
                treeUri?.let {
                    FileOutputStream(context.contentResolver.openFileDescriptor(find(DocumentFile.fromTreeUri(context, it), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)?.uri
                            ?: Uri.EMPTY, "rw")?.fileDescriptor)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

    fun getInputStream(file_name: String): InputStream? {
        try {
            return if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0") == "0") {
                var file = File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name))
                if (!file.exists())
                    file.mkdirs()
                file = File(file, file_name)
                if (!file.exists())
                    file.createNewFile()
                FileInputStream(File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name))
            } else {
                treeUri?.let {
                    context.contentResolver.openInputStream(find(DocumentFile.fromTreeUri(context, it), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)?.uri
                            ?: Uri.EMPTY)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

    fun getTmpInputStream(file_name: String): InputStream? {
        return try {
            FileInputStream(File(FileUtil.getFullPathFromTreeUri(treeUri, context), "Android/data/knf.kuma/files/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    }

    fun existFile(file_name: String): Boolean {
        return try {
            if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0") == "0") {
                File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name).exists()
            } else {
                treeUri?.let {
                    val documentFile = DocumentFile.fromTreeUri(context, it)
                    if (documentFile != null && documentFile.exists()) {
                        find(documentFile, "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
                    }
                    File(FileUtil.getFullPathFromTreeUri(treeUri, context), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name).exists()
                } ?: false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

    }

    fun canDownload(fragment: Fragment): Boolean {
        return if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0") == "0") {
            true
        } else {
            try {
                treeUri?.let {
                    val documentFile = DocumentFile.fromTreeUri(context, it)
                    if (documentFile != null && documentFile.exists()) {
                        true
                    } else {
                        openTreeChooser(fragment)
                        false
                    }
                } ?: false
            } catch (e: IllegalArgumentException) {
                openTreeChooser(fragment)
                false
            }

        }
    }

    fun canDownload(fragment: Fragment, value: String?): Boolean {
        return if (value == "0") {
            true
        } else {
            try {
                treeUri?.let {
                    val documentFile = DocumentFile.fromTreeUri(context, it)
                    if (documentFile != null && documentFile.exists()) {
                        true
                    } else {
                        openTreeChooser(fragment)
                        false
                    }
                } ?: false
            } catch (e: IllegalArgumentException) {
                openTreeChooser(fragment)
                false
            }

        }
    }

    fun getDataUri(file_name: String): Uri? {
        try {
            return if (PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0") == "0") {
                FileProvider.getUriForFile(context, "${getPackage()}.fileprovider", File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name))
            } else {
                treeUri?.let {
                    val documentFile = DocumentFile.fromTreeUri(context, it)
                    if (documentFile != null && documentFile.exists()) {
                        val root = find(documentFile, "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
                        root?.uri
                    }
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

    @Throws(Exception::class)
    private fun find(root: DocumentFile?, path: String, create: Boolean = true): DocumentFile? {
        var fRoot = root
        for (name in path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val file = fRoot?.findFile(name)
            fRoot = if (file == null || !file.exists()) {
                if (create)
                    when {
                        name.endsWith(".mp4") -> fRoot?.createFile("video/mp4", name)
                        name.endsWith(".nomedia") -> fRoot?.createFile("application/nomedia", name)
                        else -> fRoot?.createDirectory(name)
                    }
                fRoot?.findFile(name)
            } else
                file
        }
        return fRoot
    }

    fun isUriValid(uri: Uri?): Boolean {
        return if (uri != null && isSDCardRoot(uri)) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString("tree_uri", uri.toString()).apply()
            true
        } else {
            false
        }
    }

    companion object {
        const val SD_REQUEST = 51247
        @SuppressLint("StaticFieldLeak")
        lateinit var INSTANCE: FileAccessHelper
        var NOMEDIA_CREATING = false

        fun init(context: Context) {
            FileAccessHelper.INSTANCE = FileAccessHelper(context)
        }

        fun openTreeChooser(fragment: Fragment) {
            try {
                fragment.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), SD_REQUEST)
            } catch (e: Exception) {
                Toaster.toast("Error al buscar SD")
            }

        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private fun isSDCardRoot(uri: Uri): Boolean {
            return isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        private fun isRootUri(uri: Uri): Boolean {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            return docId.endsWith(":")
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        private fun isInternalStorage(uri: Uri): Boolean {
            return isExternalStorageDocument(uri) && DocumentsContract.getTreeDocumentId(uri).contains("primary")
        }

        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }
    }
}
