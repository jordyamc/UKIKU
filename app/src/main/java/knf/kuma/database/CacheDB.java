package knf.kuma.database;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.database.dao.ChaptersDAO;
import knf.kuma.database.dao.DownloadsDAO;
import knf.kuma.database.dao.ExplorerDAO;
import knf.kuma.database.dao.FavsDAO;
import knf.kuma.database.dao.GenresDAO;
import knf.kuma.database.dao.NotificationDAO;
import knf.kuma.database.dao.QueueDAO;
import knf.kuma.database.dao.RecentsDAO;
import knf.kuma.database.dao.RecordsDAO;
import knf.kuma.database.dao.SeeingDAO;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.DownloadObject;
import knf.kuma.pojos.ExplorerObject;
import knf.kuma.pojos.FavoriteObject;
import knf.kuma.pojos.GenreStatusObject;
import knf.kuma.pojos.NotificationObj;
import knf.kuma.pojos.QueueObject;
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
        ExplorerObject.class,
        GenreStatusObject.class,
        QueueObject.class
}, version = 4)
public abstract class CacheDB extends RoomDatabase {
    public static CacheDB INSTANCE;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE `genrestatusobject` (`key` INTEGER NOT NULL, "
                    + "`name` TEXT, `count` INTEGER NOT NULL, PRIMARY KEY(`key`))");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE `queueobject` (`key` INTEGER, `id` INTEGER NOT NULL,"
                    + "`number` TEXT, `eid` TEXT,`isFile` INTEGER NOT NULL,`link` TEXT,`name` TEXT,`aid` TEXT,`time` INTEGER NOT NULL, PRIMARY KEY (`id`))");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `queueobject`  ADD COLUMN `uri` TEXT");
        }
    };

    public abstract RecentsDAO recentsDAO();

    public abstract AnimeDAO animeDAO();

    public abstract FavsDAO favsDAO();

    public abstract ChaptersDAO chaptersDAO();

    public abstract NotificationDAO notificationDAO();

    public abstract DownloadsDAO downloadsDAO();

    public abstract RecordsDAO recordsDAO();

    public abstract SeeingDAO seeingDAO();

    public abstract ExplorerDAO explorerDAO();

    public static void init(Context context) {
        INSTANCE = Room.databaseBuilder(context, CacheDB.class, "cache-db")
                .allowMainThreadQueries()
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build();
    }

    public abstract QueueDAO queueDAO();

    public abstract GenresDAO genresDAO();

}
