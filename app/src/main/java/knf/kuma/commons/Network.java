package knf.kuma.commons;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import static android.content.Context.WIFI_SERVICE;

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

    public static String getIPAddress() {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }
}
