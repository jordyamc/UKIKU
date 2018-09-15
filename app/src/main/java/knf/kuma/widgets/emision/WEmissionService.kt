package knf.kuma.widgets.emision

import android.content.Intent
import android.widget.RemoteViewsService

class WEmissionService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        return WEListProvider(applicationContext)
    }
}
