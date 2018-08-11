package knf.kuma.database;

import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import androidx.room.TypeConverter;
import knf.kuma.videoservers.Headers;

public class BaseConverter {
    @TypeConverter
    public int BooleanToInt(Boolean b) {
        return b ? 1 : 0;
    }

    @TypeConverter
    public Boolean intToBoolean(int i) {
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

    @TypeConverter
    public String HeadersToString(Headers headers) {
        return new Gson().toJson(headers, new TypeToken<Headers>() {
        }.getType());
    }

    @TypeConverter
    public Headers StringToHeader(String json) {
        return new Gson().fromJson(json, new TypeToken<Headers>() {
        }.getType());
    }
}
