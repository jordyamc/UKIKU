package knf.kuma.database;

import android.arch.persistence.room.TypeConverter;

public class BaseConverter {
    @TypeConverter
    public int BooleanToInt(Boolean b){
        return b?1:0;
    }

    @TypeConverter
    public Boolean intToBoolean(int i){
        return i>0;
    }
}
