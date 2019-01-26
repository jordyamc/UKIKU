package knf.kuma.commons

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.crashlytics.android.Crashlytics
import knf.kuma.download.FileAccessHelper
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.OutputStream
import java.lang.reflect.Array
import java.util.*

object FileUtil {

    private const val PRIMARY_VOLUME_NAME = "primary"
    internal var TAG = "TAG"

    val externalMounts: HashSet<String>
        get() {

            val out = HashSet<String>()
            val reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*"
            val s = StringBuilder()
            try {
                val process = ProcessBuilder().command("mount").redirectErrorStream(true).start()
                process.waitFor()
                val inputStream = process.inputStream
                val buffer = ByteArray(1024)
                while (inputStream.read(buffer) != -1) {
                    s.append(String(buffer))
                }
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val lines = s.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (line in lines) {
                if (!line.toLowerCase(Locale.US).contains("asec")) {
                    if (line.matches(reg.toRegex())) {
                        val parts = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        for (part in parts) {
                            if (part.startsWith("/")) {
                                if (!part.toLowerCase(Locale.US).contains("vold")) {
                                    out.add(part)
                                }
                            }
                        }
                    }
                }
            }
            return out
        }

    fun getFullPathFromTreeUri(treeUri: Uri?, con: Context): String? {
        if (treeUri == null) {
            return null
        }
        var volumePath: String? = FileUtil.getVolumePath(FileUtil.getVolumeIdFromTreeUri(treeUri), con)
                ?: return File.separator
        if (volumePath?.endsWith(File.separator) == true) {
            volumePath = volumePath.substring(0, volumePath.length - 1)
        }

        var documentPath = FileUtil.getDocumentPathFromTreeUri(treeUri)
        if (documentPath.endsWith(File.separator)) {
            documentPath = documentPath.substring(0, documentPath.length - 1)
        }

        return if (documentPath.isNotEmpty()) {
            if (documentPath.startsWith(File.separator)) {
                volumePath + documentPath
            } else {
                volumePath + File.separator + documentPath
            }
        } else {
            volumePath
        }
    }


    private fun getVolumePath(volumeId: String?, con: Context): String? {
        try {
            val mStorageManager = con.getSystemService(Context.STORAGE_SERVICE) as StorageManager

            val storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")

            val getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")
            val getUuid = storageVolumeClazz.getMethod("getUuid")
            val getPath = storageVolumeClazz.getMethod("getPath")
            val isPrimary = storageVolumeClazz.getMethod("isPrimary")
            val result = getVolumeList.invoke(mStorageManager)

            val length = Array.getLength(result)
            for (i in 0 until length) {
                val storageVolumeElement = Array.get(result, i)
                val uuid = getUuid.invoke(storageVolumeElement) as String?
                val primary = isPrimary.invoke(storageVolumeElement) as Boolean

                // primary volume?
                if (primary && PRIMARY_VOLUME_NAME == volumeId) {
                    return getPath.invoke(storageVolumeElement) as String
                }

                // other volumes?
                if (uuid != null) {
                    if (uuid == volumeId) {
                        return getPath.invoke(storageVolumeElement) as String
                    }
                }
            }

            // not found.
            return null
        } catch (ex: Exception) {
            return null
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getVolumeIdFromTreeUri(treeUri: Uri): String? {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        return if (split.isNotEmpty()) {
            split[0]
        } else {
            null
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getDocumentPathFromTreeUri(treeUri: Uri): String {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return if (split.size >= 2) {
            split[1]
        } else {
            File.separator
        }
    }

    fun moveFile(resolver: ContentResolver, uri: Uri?, outputStream: OutputStream?, delete: Boolean = true): LiveData<Pair<Int, Boolean>> {
        val liveData = MutableLiveData<Pair<Int, Boolean>>()
        if (uri == null || outputStream == null) {
            doOnUI { liveData.setValue(Pair(-1, true)) }
            return liveData
        }
        doAsync {
            try {
                val inputStream = resolver.openInputStream(uri)
                val total = inputStream?.available()?.toLong() ?: 0
                val buffer = ByteArray(128 * 1024)
                var read: Int = inputStream?.read(buffer) ?: 0
                var current: Long = 0
                while (read != -1) {
                    outputStream.write(buffer, 0, read)
                    current += read.toLong()
                    val prog = (current * 100 / total).toInt()
                    doOnUI { liveData.setValue(Pair(prog, false)) }
                    read = inputStream?.read(buffer) ?: 0
                }
                inputStream?.close()
                outputStream.flush()
                outputStream.close()
                try {
                    if (delete)
                        DocumentsContract.deleteDocument(resolver, uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                doOnUI { liveData.setValue(Pair(100, true)) }
            } catch (e: Exception) {
                e.printStackTrace()
                Crashlytics.logException(e)
                doOnUI { liveData.setValue(Pair(-1, true)) }
            }
        }
        return liveData
    }

    fun moveFiles(resolver: ContentResolver, pairs: MutableList<Pair<Uri, String>>): LiveData<Pair<Pair<String, Int>, Boolean>> {
        val liveData = MutableLiveData<Pair<Pair<String, Int>, Boolean>>()
        doAsync {
            val ps = "Importando archivos: %d/%d"
            val gTotal = pairs.size
            var success = 0
            for ((g_count, pair) in pairs.withIndex()) {
                try {
                    val inputStream = resolver.openInputStream(pair.first)
                    val outputStream = FileAccessHelper.INSTANCE.getOutputStream(pair.second)
                    val total = inputStream?.available()?.toLong() ?: 0
                    val buffer = ByteArray(128 * 1024)
                    var read: Int = inputStream?.read(buffer) ?: 0
                    var current: Long = 0
                    while (read != -1) {
                        outputStream?.write(buffer, 0, read)
                        current += read.toLong()
                        val prog = (current * 100 / total).toInt()
                        doOnUI { liveData.setValue(Pair(Pair(String.format(Locale.US, ps, g_count, gTotal), prog), false)) }
                        read = inputStream?.read(buffer) ?: 0
                    }
                    inputStream?.close()
                    outputStream?.flush()
                    outputStream?.close()
                    doOnUI { liveData.setValue(Pair(Pair(String.format(Locale.US, ps, g_count, gTotal), 100), false)) }
                    try {
                        DocumentsContract.deleteDocument(resolver, pair.first)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    success++
                } catch (e: Exception) {
                    e.printStackTrace()
                    FileAccessHelper.INSTANCE.delete(pair.second)
                }

            }
            val finalSuccess = success
            doOnUI { liveData.setValue(Pair(Pair(String.format(Locale.US, ps, gTotal, gTotal), finalSuccess), true)) }
        }
        return liveData
    }

    fun moveFile(file_name: String, callback: MoveCallback) {
        doAsync {
            try {
                val inputStream = FileAccessHelper.INSTANCE.getTmpInputStream(file_name)
                val outputStream = FileAccessHelper.INSTANCE.getOutputStream(file_name)
                val total = inputStream?.available()?.toLong() ?: 0
                val buffer = ByteArray(128 * 1024)
                var read: Int = inputStream?.read(buffer) ?: 0
                var current: Long = 0
                while (read != -1) {
                    outputStream?.write(buffer, 0, read)
                    current += read.toLong()
                    val prog = (current * 100 / total).toInt()
                    callback.onProgress(Pair(prog, false))
                    read = inputStream?.read(buffer) ?: 0
                }
                inputStream?.close()
                outputStream?.flush()
                outputStream?.close()
                try {
                    val file = FileAccessHelper.INSTANCE.getTmpFile(file_name)
                    file.delete()
                    if (file.parentFile.list().isEmpty())
                        file.parentFile.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                callback.onProgress(Pair(100, true))
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onProgress(Pair(-1, true))
            }
        }
    }

    interface MoveCallback {
        fun onProgress(pair: Pair<Int, Boolean>)
    }
}
