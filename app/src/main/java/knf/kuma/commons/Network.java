package knf.kuma.commons;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Jordy on 03/01/2018.
 */

public class Network {
    private static Context context;

    public static void init(Context context){
        Network.context=context;
    }

    public static boolean isConnected(){
        try {
            ConnectivityManager cm =
                    (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
        }catch (NullPointerException e){
            return false;
        }
    }
}
