package knf.kuma.updater

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.thin.downloadmanager.DownloadRequest
import com.thin.downloadmanager.DownloadStatusListenerV1
import com.thin.downloadmanager.ThinDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xdroid.toaster.Toaster
import java.io.File

class UpdaterViewModel : ViewModel() {
    private val liveData = MutableLiveData<Pair<UpdaterType, Any?>>(UpdaterType.TYPE_IDLE to null)
    private val manager = ThinDownloadManager()
    private var isStarted = false

    fun start(file: File, link: String): LiveData<Pair<UpdaterType, Any?>> {
        if (isStarted) return liveData
        isStarted = true
        manager.add(DownloadRequest(Uri.parse(link))
                .setDestinationURI(Uri.fromFile(file))
                .setDownloadResumable(false)
                .setStatusListener(object : DownloadStatusListenerV1 {
                    override fun onDownloadComplete(downloadRequest: DownloadRequest?) {
                        viewModelScope.launch(Dispatchers.Main) {
                            liveData.value = UpdaterType.TYPE_COMPLETED to null
                        }
                    }

                    override fun onDownloadFailed(downloadRequest: DownloadRequest?, errorCode: Int, errorMessage: String?) {
                        viewModelScope.launch(Dispatchers.Main) {
                            liveData.value = UpdaterType.TYPE_ERROR to errorMessage
                        }
                        Log.e("Update Error", "Code: $errorCode Message: $errorMessage")
                        Toaster.toast("Error al actualizar: $errorMessage")
                        FirebaseCrashlytics.getInstance().recordException(IllegalStateException("Update failed\nCode: $errorCode Message: $errorMessage"))
                    }

                    override fun onProgress(downloadRequest: DownloadRequest?, totalBytes: Long, downloadedBytes: Long, progress: Int) {
                        viewModelScope.launch(Dispatchers.Main) {
                            liveData.value = UpdaterType.TYPE_PROGRESS to progress
                        }
                    }
                }))
        return liveData
    }

    override fun onCleared() {
        super.onCleared()
        manager.cancelAll()
        isStarted = false
        liveData.value = UpdaterType.TYPE_IDLE to null
    }
}

enum class UpdaterType {
    TYPE_ERROR, TYPE_PROGRESS, TYPE_COMPLETED, TYPE_IDLE
}