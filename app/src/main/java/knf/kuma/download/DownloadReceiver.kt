package knf.kuma.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val did = intent.getIntExtra("did", 0)
        when (intent.getIntExtra("action", -1)) {
            DownloadManager.ACTION_PAUSE -> DownloadManager.pause(did)
            DownloadManager.ACTION_RESUME -> DownloadManager.resume(did)
            DownloadManager.ACTION_CANCEL -> DownloadManager.cancel(intent.getStringExtra("eid"))
        }
    }
}
