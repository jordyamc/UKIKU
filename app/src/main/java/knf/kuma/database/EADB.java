package knf.kuma.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import knf.kuma.database.dao.EaDAO;
import knf.kuma.pojos.EAObject;

@Database(entities = {EAObject.class}, version = 1)
public abstract class EADB extends RoomDatabase {
    public static EADB INSTANCE;

    public static void init(Context context) {
        INSTANCE = Room.databaseBuilder(context, EADB.class, "ee-db")
                .allowMainThreadQueries()
                .build();
    }

    public abstract EaDAO eaDAO();
}
