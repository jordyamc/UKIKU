package knf.kuma.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DownloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int did = intent.getIntExtra("did", 0);
        switch (intent.getIntExtra("action", -1)) {
            case DownloadManager.ACTION_PAUSE:
                DownloadManager.pause(did);
                break;
            case DownloadManager.ACTION_RESUME:
                DownloadManager.resume(did);
                break;
            case DownloadManager.ACTION_CANCEL:
                DownloadManager.cancel(intent.getStringExtra("eid"));
                break;
        }
    }
}
