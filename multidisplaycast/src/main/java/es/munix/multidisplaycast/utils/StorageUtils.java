package es.munix.multidisplaycast.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by munix on 3/11/16.
 */

public class StorageUtils {

    private static final String SHARED_PREFS = "MultiCast";

    private static SharedPreferences getSharedPreferences( Context context ) {
        return context.getSharedPreferences( SHARED_PREFS, Context.MODE_PRIVATE );
    }

    public static String getRecentDeviceId( Context context ) {
        return getSharedPreferences( context ).getString( "recentDeviceId", "" );
    }

    public static void setRecentDeviceId( Context context, String deviceId ) {
        SharedPreferences.Editor editor = getSharedPreferences( context ).edit();
        editor.putString( "recentDeviceId", deviceId );
        editor.commit();
    }
}
