package knf.kuma.database;

import android.arch.persistence.room.TypeConverter;
import android.net.Uri;

public class BaseConverter {
    @TypeConverter
    public int BooleanToInt(Boolean b){
        return b?1:0;
    }

    @TypeConverter
    public Boolean intToBoolean(int i){
        return i == 1;
    }

    @TypeConverter
    public String UriToString(Uri uri) {
        return uri.toString();
    }

    @TypeConverter
    public Uri StringToUri(String s) {
        return Uri.parse(s);
    }
}
