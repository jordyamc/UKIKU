package knf.kuma.database

import android.net.Uri
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import knf.kuma.videoservers.Headers

class BaseConverter {
    @TypeConverter
    fun booleanToInt(b: Boolean): Int {
        return if (b) 1 else 0
    }

    @TypeConverter
    fun intToBoolean(i: Int): Boolean {
        return i == 1
    }

    @TypeConverter
    fun uriToString(uri: Uri): String {
        return uri.toString()
    }

    @TypeConverter
    fun stringToUri(s: String): Uri {
        return Uri.parse(s)
    }

    @TypeConverter
    fun headersToString(headers: Headers?): String {
        return Gson().toJson(headers, object : TypeToken<Headers>() {
        }.type)
    }

    @TypeConverter
    fun stringToHeader(json: String?): Headers? {
        return Gson().fromJson<Headers>(json, object : TypeToken<Headers>() {
        }.type)
    }
}
