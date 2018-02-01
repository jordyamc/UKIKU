package es.munix.multidisplaycast.helpers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

import com.bumptech.glide.Glide;

import java.util.concurrent.ExecutionException;

import es.munix.multidisplaycast.CastControlsActivity;
import es.munix.multidisplaycast.R;
import es.munix.multidisplaycast.services.CastReceiver;

/**
 * Created by munix on 3/11/16.
 */

public class NotificationsHelper {

    public static final int NOTIFICATION_ID = 800;

    public static void cancelNotification( Context context ) {
        if ( context != null ) {
            NotificationManager nMgr = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );
            nMgr.cancel( NOTIFICATION_ID );
        }
    }

    public static void showNotification( final Context context, String title, String subtitle, final String icon, Boolean isPaused ) {

        final NotificationCompat.Builder notification = new NotificationCompat.Builder( context ).setOngoing( true )
                .setAutoCancel( false )
                .setContentTitle( title )
                .setContentText( subtitle )
                .setSmallIcon( R.drawable.cast_on );


        Intent castActivityIntent = new Intent( context, CastControlsActivity.class );
        PendingIntent castActivityPendingIntent = PendingIntent.getActivity( context, NOTIFICATION_ID, castActivityIntent, 0 );
        notification.setContentIntent( castActivityPendingIntent );


        Intent disconnectIntent = new Intent( context, CastReceiver.class );
        disconnectIntent.putExtra( "action", "disconnect" );
        notification.addAction( R.drawable.ic_stop_white_24dp, "Detener", PendingIntent.getBroadcast( context, NOTIFICATION_ID + 1, disconnectIntent, 0 ) );


        Intent pauseIntent = new Intent( context, CastReceiver.class );
        pauseIntent.putExtra( "action", "pause" );
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast( context, NOTIFICATION_ID + 2, pauseIntent, 0 );
        if ( !isPaused ) {
            notification.addAction( R.drawable.ic_pause_white_24dp, "Pausar", pausePendingIntent );
        } else {
            notification.addAction( R.drawable.ic_play_arrow_white_24dp, "Reanudar", pausePendingIntent );
        }

        final Handler mHandler = new Handler() {
            @Override
            public void handleMessage( Message msg ) {
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );
                notificationManager.notify( NOTIFICATION_ID, notification.build() );
            }
        };

        new Thread() {
            @Override
            public void run() {
                try {
                    Bitmap largeIcon = Glide.
                            with( context ).
                            load( icon ).
                            asBitmap().
                            into( 100, 100 ).
                            get();
                    notification.setLargeIcon( largeIcon );
                } catch ( InterruptedException e ) {
                    e.printStackTrace();
                } catch ( ExecutionException e ) {
                    e.printStackTrace();
                } catch ( OutOfMemoryError oom ) {
                    oom.printStackTrace();
                }
                mHandler.sendEmptyMessage( 1 );
            }
        }.start();
    }
}
