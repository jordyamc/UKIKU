package es.munix.multidisplaycast.utils;

import java.util.Locale;

/**
 * Created by munix on 3/11/16.
 */

public class Format {

    public static String time( long millisec ) {
        int seconds = (int) ( millisec / 1000 );
        int hours = seconds / ( 60 * 60 );
        seconds %= ( 60 * 60 );
        int minutes = seconds / 60;
        seconds %= 60;

        String time;
        if ( hours > 0 ) {
            time = String.format( Locale.US, "%d:%02d:%02d", hours, minutes, seconds );
        } else {
            time = String.format( Locale.US, "%d:%02d", minutes, seconds );
        }

        return time;
    }
}
