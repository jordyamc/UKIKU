package knf.kuma.download

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import knf.kuma.App
import knf.kuma.commons.*
import knf.kuma.explorer.creator.Creator
import knf.kuma.explorer.creator.DocumentFileCreator
import knf.kuma.explorer.creator.SimpleFileCreator
import knf.kuma.explorer.creator.SubFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.io.*

object FileAccessHelper {

    const val SD_REQUEST = 51247
    var NOMEDIA_CREATING = false

    val downloadsDirectory: File
        get() {
            return try {
                if (PrefsUtil.downloadType == "0") {
                    File(Environment.getExternalStorageDirectory(), "UKIKU/downloads")
                } else {
                    File(FileUtil.getFullPathFromTreeUri(treeUri, App.context), "UKIKU/downloads")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Environment.getDataDirectory()
            }

        }

    val downloadExplorerCreator: Creator
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                DocumentFileCreator(treeUri?.let { find(DocumentFile.fromTreeUri(App.context, it), "UKIKU/downloads", false) })
            } else {
                SimpleFileCreator(
                        if (PrefsUtil.downloadType == "0") {
                            File(Environment.getExternalStorageDirectory(), "UKIKU/downloads")
                        } else {
                            File(FileUtil.getFullPathFromTreeUri(treeUri, App.context), "UKIKU/downloads")
                        }
                )
            }
        }

    val internalRoot: File get() = Environment.getExternalStorageDirectory()

    val externalRoot: File? get() = FileUtil.getFullPathFromTreeUri(treeUri, App.context)?.let { File(it) }

    val treeUri: Uri?
        get() {
            return try {
                Uri.parse(PreferenceManager.getDefaultSharedPreferences(App.context).getString("tree_uri", null))
            } catch (e: Exception) {
                null
            }

        }

    fun isStoragePermissionEnabled(): Boolean {
        return when {
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || PrefsUtil.downloadType == "1") -> treeUri != null && DocumentFile.fromTreeUri(App.context, treeUri!!)?.let { it.exists() && it.canWrite() } == true
            ContextCompat.checkSelfPermission(App.context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> true
            else -> false
        }
    }

    suspend fun isStoragePermissionEnabledAsync(): Boolean {
        return withContext(Dispatchers.IO) {
            when {
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || PrefsUtil.downloadType == "1") -> treeUri != null && DocumentFile.fromTreeUri(App.context, treeUri!!)?.let { it.exists() && it.canWrite() } == true
                ContextCompat.checkSelfPermission(App.context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> true
                else -> false
            }
        }
    }

    fun getFile(file_name: String?): File {
        return try {
            if (file_name.isNullOrEmpty()) throw IllegalStateException("Name can't be null!")
            if (PrefsUtil.downloadType == "0") {
                File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
            } else {
                File(FileUtil.getFullPathFromTreeUri(treeUri, App.context), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            File(Environment.getDataDirectory(), "test.txt")
        }

    }

    fun findFile(file_name: String?): File {
        return try {
            if (file_name.isNullOrEmpty()) throw IllegalStateException("Name can't be null!")
            if (PrefsUtil.downloadType == "0") {
                File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name)).listFiles { file -> file.name.contains(file_name) }!![0]
            } else
                File(FileUtil.getFullPathFromTreeUri(treeUri, App.context), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name)).listFiles { file -> file.name.contains(file_name) }!![0]
        } catch (e: Exception) {
            e.printStackTrace()
            File(Environment.getDataDirectory(), "test.txt")
        }

    }

    fun fileFindExist(file_name: String?): Boolean {
        return try {
            if (file_name.isNullOrEmpty()) throw IllegalStateException("Name can't be null!")
            if (PrefsUtil.downloadType == "0") {
                !File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name)).listFiles { file -> file.name.contains(file_name) }.isNullOrEmpty()
            } else {
                !find(DocumentFile.fromTreeUri(App.context, treeUri!!), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name), false)?.listFiles()?.mapNotNull { it.name?.contains(file_name) }.isNullOrEmpty()

            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getFileUri(file_name: String?): Uri? {
        if (file_name.isNullOrEmpty()) throw IllegalStateException("Name can't be null!")
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            Uri.fromFile(
                    try {
                        if (file_name.startsWith("$")) {
                            findFile(file_name)
                        } else {
                            if (PrefsUtil.downloadType == "0") {
                                File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
                            } else {
                                File(FileUtil.getFullPathFromTreeUri(treeUri, App.context), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        File(Environment.getDataDirectory(), "test.txt")
                    }
            )
        else
            getDataUri(file_name)
    }

    val rootFile: File
        get() {
            return try {
                if (PrefsUtil.downloadType == "0") {
                    Environment.getExternalStorageDirectory()
                } else {
                    File(FileUtil.getFullPathFromTreeUri(treeUri, App.context))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Environment.getExternalStorageDirectory()
            }

        }

    fun getTmpFile(file_name: String): File {
        return File(getDownloadsCacheDir(), PatternUtil.getNameFromFile(file_name) + file_name)
    }

    val toneFile: File
        get() {
            return File(App.context.getExternalFilesDir(null), "custom_tone")
        }

    fun setToneFile(enable: Boolean) {
        if (!enable) toneFile.delete()
        PreferenceManager.getDefaultSharedPreferences(App.context).edit().putBoolean("is_custom_tone", enable).apply()
    }

    fun getFileCreate(file_name: String): File? {
        return try {
            if (PrefsUtil.downloadType == "0") {
                val file = File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
                file.parentFile?.mkdirs()
                if (!file.exists())
                    file.createNewFile()
                file
            } else {
                createTmpIfNotExist()
                val file = File(getDownloadsCacheDir(), PatternUtil.getNameFromFile(file_name) + file_name)
                file.parentFile?.mkdirs()
                if (!file.exists())
                    file.createNewFile()
                file
            }
        } catch (e: Exception) {
            Log.e("File create", "Error")
            e.printStackTrace()
            null
        }

    }

    private fun getDownloadsCacheDir(): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            App.context.getExternalFilesDirs("downloads").last()
        else
            File(FileUtil.getFullPathFromTreeUri(treeUri, App.context), "Android/data/${getPackage()}/files/downloads")
    }

    internal fun isTempFile(file: String): Boolean {
        return try {
            val path = FileUtil.getFullPathFromTreeUri(treeUri, App.context) ?: return false
            file.contains(path)
        } catch (e: Exception) {
            false
        }
    }

    fun checkNoMedia(noMediaNeeded: Boolean) {
        NOMEDIA_CREATING = true
        doAsync {
            try {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
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
                }
                treeUri?.let {
                    val documentRoot = find(DocumentFile.fromTreeUri(App.context, it), "UKIKU/downloads")
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
            if (PrefsUtil.downloadType == "0") {
                File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/$file_name")
            } else {
                File(FileUtil.getFullPathFromTreeUri(treeUri, App.context), "UKIKU/downloads/$file_name")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Environment.getDataDirectory()
        }

    }

    fun getDownloadsDirectoryFiles(file_name: String): List<SubFile> {
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    treeUri?.let { find(DocumentFile.fromTreeUri(App.context, it), "UKIKU/downloads/$file_name", false) }?.listFiles()?.map {
                        SubFile(it.name ?: "", it.uri.toString())
                    }?.filter { it.name.endsWith(".mp4") } ?: emptyList()
                PrefsUtil.downloadType == "0" -> {
                    File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/$file_name").listFiles()?.map { SubFile(it.name, Uri.fromFile(it).toString()) }?.filter { it.name.endsWith(".mp4") }
                            ?: emptyList()
                }
                else -> {
                    File(FileUtil.getFullPathFromTreeUri(treeUri, App.context), "UKIKU/downloads/$file_name").listFiles()?.map { SubFile(it.name, Uri.fromFile(it).toString()) }?.filter { it.name.endsWith(".mp4") }
                            ?: emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }

    }

    fun getDownloadsDirectoryFromFile(file_name: String): File {
        return try {
            if (PrefsUtil.downloadType == "0") {
                File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/${PatternUtil.getNameFromFile(file_name)}")
            } else {
                File(FileUtil.getFullPathFromTreeUri(treeUri, App.context), "UKIKU/downloads/${PatternUtil.getNameFromFile(file_name)}")
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

    fun deletePath(file_name: String?, async: Boolean = true) {
        if (async)
            doAsync { deletePath(file_name) }
        else
            deletePath(file_name)
    }

    private fun delete(file_name: String?) {
        if (file_name.isNull())
            return
        try {
            if (PrefsUtil.downloadType == "0") {
                val file = File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
                file.delete()
                val dir = file.parentFile
                if (dir?.listFiles() == null || dir.listFiles()?.isEmpty() == true)
                    dir?.delete()
            } else {
                treeUri?.let {
                    val documentFile = DocumentFile.fromTreeUri(App.context, it)
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

    private fun deletePath(file_name: String?) {
        if (file_name == null)
            return
        try {
            if (PrefsUtil.downloadType == "0") {
                val file = File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name)).listFiles { file -> file.name.contains(file_name) }!![0]
                file.delete()
                val dir = file.parentFile
                if (dir?.listFiles() == null || dir.listFiles()?.isEmpty() == true)
                    dir?.delete()
            } else {
                treeUri?.let {
                    val documentFile = DocumentFile.fromTreeUri(App.context, it)
                    if (documentFile != null && documentFile.exists()) {
                        val file = find(documentFile, "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name))?.listFiles()?.let {
                            var tFile: DocumentFile? = null
                            it.forEach {
                                if (it.name?.contains(file_name) == true)
                                    tFile = it
                            }
                            tFile
                        }
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
        if (!getDownloadsCacheDir().exists()) {
            treeUri?.let {
                getDownloadsCacheDir().mkdirs()
                /*val documentFile = DocumentFile.fromTreeUri(App.context, it)
                if (documentFile != null && documentFile.exists()) {
                    val file = find(documentFile, "Android/data/${getPackage()}/files/downloads/tmp.file")
                    file?.delete()
                }*/
            }
        }
    }

    fun getOutputStream(file_name: String?): OutputStream? {
        if (file_name == null) return null
        try {
            return if (PrefsUtil.downloadType == "0" && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                var file = File(
                    Environment.getExternalStorageDirectory(),
                    "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name)
                )
                if (!file.exists())
                    file.mkdirs()
                file = File(file, file_name)
                if (!file.exists())
                    file.createNewFile()
                FileOutputStream(
                    File(
                        Environment.getExternalStorageDirectory(),
                        "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name
                    )
                )
            } else {
                treeUri?.let {
                    App.context.contentResolver.openOutputStream(
                        find(
                            DocumentFile.fromTreeUri(App.context, it),
                            "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name
                        )?.uri
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
            return if (PrefsUtil.downloadType == "0") {
                var file = File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name))
                if (!file.exists())
                    file.mkdirs()
                file = File(file, file_name)
                if (!file.exists())
                    file.createNewFile()
                FileOutputStream(File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name))
            } else {
                treeUri?.let {
                    FileOutputStream(App.context.contentResolver.openFileDescriptor(find(DocumentFile.fromTreeUri(App.context, it), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)?.uri
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
            return if (PrefsUtil.downloadType == "0") {
                var file = File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name))
                if (!file.exists())
                    file.mkdirs()
                file = File(file, file_name)
                if (!file.exists())
                    file.createNewFile()
                FileInputStream(File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name))
            } else {
                treeUri?.let {
                    App.context.contentResolver.openInputStream(find(DocumentFile.fromTreeUri(App.context, it), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)?.uri
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
            val file = File(getDownloadsCacheDir(), PatternUtil.getNameFromFile(file_name) + file_name)
            if (file.parentFile?.exists() == false)
                file.parentFile?.mkdirs()
            FileInputStream(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    }

    fun existFile(file_name: String): Boolean {
        return try {
            if (PrefsUtil.downloadType == "0") {
                File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name).exists()
            } else {
                treeUri?.let {
                    val documentFile = DocumentFile.fromTreeUri(App.context, it)
                    if (documentFile != null && documentFile.exists()) {
                        find(documentFile, "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
                    }
                    File(FileUtil.getFullPathFromTreeUri(treeUri, App.context), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name).exists()
                } ?: false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

    }

    fun canDownload(fragment: Fragment): Boolean {
        return if (PrefsUtil.downloadType == "0") {
            true
        } else {
            try {
                val uri = treeUri
                if (uri != null) {
                    val documentFile = DocumentFile.fromTreeUri(App.context, uri)
                    if (documentFile != null && documentFile.exists()) {
                        true
                    } else {
                        openTreeChooser(fragment)
                        false
                    }
                } else {
                    openTreeChooser(fragment)
                    false
                }
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
                val uri = treeUri
                if (uri != null) {
                    val documentFile = DocumentFile.fromTreeUri(App.context, uri)
                    if (documentFile != null && documentFile.exists()) {
                        true
                    } else {
                        openTreeChooser(fragment)
                        false
                    }
                } else {
                    openTreeChooser(fragment)
                    false
                }
            } catch (e: IllegalArgumentException) {
                openTreeChooser(fragment)
                false
            }

        }
    }

    fun getDataUri(file_name: String): Uri? {
        try {
            return if (PrefsUtil.downloadType == "0") {
                FileProvider.getUriForFile(App.context, "${getPackage()}.fileprovider", if (file_name.startsWith("$")) findFile(file_name) else File(Environment.getExternalStorageDirectory(), "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name))
            } else {
                treeUri?.let {
                    val documentFile = DocumentFile.fromTreeUri(App.context, it)
                    if (documentFile != null && documentFile.exists()) {
                        val root = find(documentFile, "UKIKU/downloads/" + PatternUtil.getNameFromFile(file_name) + file_name)
                        return@let root?.uri
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
    fun find(root: DocumentFile?, path: String, create: Boolean = true): DocumentFile? {
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

    fun isUriValid(uri: Uri?): UriValidation {
        val uriValidation = UriValidation()
        uri ?: return uriValidation.also { it.errorMessage = "Uri es nulo" }
        if (isSDCardRoot(uri, uriValidation)) {
            if (isInternalStorage(uri))
                PrefsUtil.storageType = "Memoria Interna"
            else
                PrefsUtil.storageType = "Memoria SD"
            App.context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            PreferenceManager.getDefaultSharedPreferences(App.context).edit().putString("tree_uri", uri.toString()).apply()
            uriValidation.isValid = true
        }
        return uriValidation
    }

    fun openTreeChooser(fragment: Fragment) {
        try {
            fragment.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), SD_REQUEST)
            Log.e("FileAccess", "On open drocument tree")
        } catch (e: Exception) {
            Toaster.toast("Error al buscar SD")
        }

    }

    fun openTreeChooser(context: Context) {
        try {
            context.findActivity()?.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), SD_REQUEST)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                Toaster.toastLong("Por favor selecciona un directorio para las descargas")
            else
                Toaster.toastLong("Por favor selecciona la raiz del almacenamiento")
        } catch (e: Exception) {
            Toaster.toast("Error al buscar SD")
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun isSDCardRoot(uri: Uri, uriValidation: UriValidation): Boolean {
        return isRootUri(uri, uriValidation) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || isExternalStorageDocument(uri, uriValidation))
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun isRootUri(uri: Uri, uriValidation: UriValidation): Boolean {
        return DocumentsContract.getTreeDocumentId(uri).endsWith(":")
                .also {
                    if (!it) {
                        Log.e("Storage", "$uri is not root")
                        uriValidation.errorMessage = "No es la raiz!"
                    }
                } || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun isInternalStorage(uri: Uri, uriValidation: UriValidation): Boolean {
        return isExternalStorageDocument(uri, uriValidation) && DocumentsContract.getTreeDocumentId(uri).contains("primary")
                .also {
                    if (it) {
                        Log.e("Storage", "$uri is internal storage")
                        uriValidation.errorMessage = "Memoria interna"
                    }
                }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun isInternalStorage(uri: Uri): Boolean {
        return isExternalStorageDocument(uri) && DocumentsContract.getTreeDocumentId(uri).contains("primary")
    }

    private fun isExternalStorageDocument(uri: Uri, uriValidation: UriValidation): Boolean {
        return ("com.android.externalstorage.documents" == uri.authority).also {
            if (!it) {
                Log.e("Storage", "$uri is not external storage document")
                uriValidation.errorMessage = "No es almacenamiento externo"
            }
        }
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return ("com.android.externalstorage.documents" == uri.authority)
    }
}
