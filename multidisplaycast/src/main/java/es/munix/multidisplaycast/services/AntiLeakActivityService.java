package es.munix.multidisplaycast.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import es.munix.multidisplaycast.CastManager;

/**
 * Created by munix on 2/11/16.
 */

public class AntiLeakActivityService extends Service {

    @Nullable
    @Override
    public IBinder onBind( Intent intent ) {
        return null;
    }

    @Override
    public void onTaskRemoved( Intent rootIntent ) {
        try {
            CastManager.getInstance().onDestroy();
        } catch ( Exception e ) {

        }
        stopSelf();
        super.onTaskRemoved( rootIntent );
    }
}
