package knf.kuma.widgets.emision;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class WEmisionService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WEListProvider(getApplicationContext());
    }
}
