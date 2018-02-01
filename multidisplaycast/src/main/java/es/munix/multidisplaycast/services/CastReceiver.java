package es.munix.multidisplaycast.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import es.munix.multidisplaycast.CastManager;

/**
 * Created by munix on 2/11/16.
 */

public class CastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive( Context context, Intent intent ) {
        Bundle extras = intent.getExtras();
        if ( extras != null ) {
            String action = extras.getString( "action" );

            Log.v( "CastReceiver", "action " + action );

            if ( action.equals( "disconnect" ) ) {
                CastManager.getInstance().stop();
            } else if ( action.equals( "pause" ) ) {
                CastManager.getInstance().togglePause();
            }
        }
    }
}
