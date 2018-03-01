package knf.kuma.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.database.dao.ChaptersDAO;
import knf.kuma.database.dao.DownloadsDAO;
import knf.kuma.database.dao.ExplorerDAO;
import knf.kuma.database.dao.FavsDAO;
import knf.kuma.database.dao.NotificationDAO;
import knf.kuma.database.dao.RecentsDAO;
import knf.kuma.database.dao.RecordsDAO;
import knf.kuma.database.dao.SeeingDAO;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.DownloadObject;
import knf.kuma.pojos.ExplorerObject;
import knf.kuma.pojos.FavoriteObject;
import knf.kuma.pojos.NotificationObj;
import knf.kuma.pojos.RecentObject;
import knf.kuma.pojos.RecordObject;
import knf.kuma.pojos.SeeingObject;

@Database(entities = {
        RecentObject.class,
        AnimeObject.class,
        FavoriteObject.class,
        AnimeObject.WebInfo.AnimeChapter.class,
        NotificationObj.class,
        DownloadObject.class,
        RecordObject.class,
        SeeingObject.class,
        ExplorerObject.class
}, version = 1, exportSchema = false)
public abstract class CacheDB extends RoomDatabase {
    public static CacheDB INSTANCE;

    public static void init(Context context) {
        INSTANCE = Room.databaseBuilder(context, CacheDB.class, "cache-db").allowMainThreadQueries().build();
    }

    public abstract RecentsDAO recentsDAO();

    public abstract AnimeDAO animeDAO();

    public abstract FavsDAO favsDAO();

    public abstract ChaptersDAO chaptersDAO();

    public abstract NotificationDAO notificationDAO();

    public abstract DownloadsDAO downloadsDAO();

    public abstract RecordsDAO recordsDAO();

    public abstract SeeingDAO seeingDAO();

    public abstract ExplorerDAO explorerDAO();
}
