package knf.kuma.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import knf.kuma.App
import knf.kuma.database.dao.AchievementsDAO
import knf.kuma.database.dao.AnimeDAO
import knf.kuma.database.dao.DownloadsDAO
import knf.kuma.database.dao.ExplorerDAO
import knf.kuma.database.dao.FavsDAO
import knf.kuma.database.dao.NotificationDAO
import knf.kuma.database.dao.PlayerStateDAO
import knf.kuma.database.dao.QueueDAO
import knf.kuma.database.dao.RecordsDAO
import knf.kuma.database.dao.SeeingDAO
import knf.kuma.database.dao.SeenDAO
import knf.kuma.player.PlayerState
import knf.kuma.pojos.Achievement
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.ExplorerObject
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.GenreStatusObject
import knf.kuma.pojos.NotificationObj
import knf.kuma.pojos.QueueObject
import knf.kuma.pojos.RecentObject
import knf.kuma.pojos.RecordObject
import knf.kuma.pojos.SeeingObject
import knf.kuma.pojos.SeenObject
import knf.kuma.pojos.av1.CalendarDao
import knf.kuma.pojos.av1.DirectoryAV1
import knf.kuma.pojos.av1.DirectoryAV1Calendar
import knf.kuma.pojos.av1.DirectoryDao
import knf.kuma.pojos.av1.FavoriteAV1
import knf.kuma.pojos.av1.FavoriteAV1Dao
import knf.kuma.pojos.av1.GenreRecord
import knf.kuma.pojos.av1.GenreRecordDao
import knf.kuma.pojos.av1.Organizer
import knf.kuma.pojos.av1.OrganizerDao
import knf.kuma.pojos.av1.RecentAV1
import knf.kuma.pojos.av1.RecentAV1Dao
import knf.kuma.pojos.av1.Record
import knf.kuma.pojos.av1.RecordDao
import knf.kuma.recents.RecentModel

@Database(
    entities = [
        RecentObject::class,
        RecentModel::class,
        PlayerState::class,
        AnimeObject::class,
        FavoriteObject::class,
        AnimeObject.WebInfo.AnimeChapter::class,
        SeenObject::class,
        NotificationObj::class,
        DownloadObject::class,
        RecordObject::class,
        SeeingObject::class,
        ExplorerObject::class,
        GenreStatusObject::class,
        QueueObject::class,
        Achievement::class,
        DirectoryAV1::class,
        FavoriteAV1::class,
        Organizer::class,
        RecentAV1::class,
        Record::class,
        GenreRecord::class,
        DirectoryAV1Calendar::class
    ],
    version = 21
)
abstract class CacheDB : RoomDatabase() {

    abstract fun animeDAO(): AnimeDAO

    abstract fun favsDAO(): FavsDAO

    abstract fun seenDAO(): SeenDAO

    abstract fun notificationDAO(): NotificationDAO

    abstract fun downloadsDAO(): DownloadsDAO

    abstract fun recordsDAO(): RecordsDAO

    abstract fun seeingDAO(): SeeingDAO

    abstract fun explorerDAO(): ExplorerDAO

    abstract fun queueDAO(): QueueDAO

    abstract fun achievementsDAO(): AchievementsDAO

    abstract fun playerStateDAO(): PlayerStateDAO

    abstract fun recentAV1DAO(): RecentAV1Dao

    abstract fun directoryDAO(): DirectoryDao

    abstract fun favoriteAV1DAO(): FavoriteAV1Dao

    abstract fun organizerDAO(): OrganizerDao

    abstract fun recordAV1DAO(): RecordDao

    abstract fun genreRecordDAO(): GenreRecordDao

    abstract fun calendarBlacklistDAO(): CalendarDao

    companion object {
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `genrestatusobject` (`key` INTEGER NOT NULL, " + "`name` TEXT, `count` INTEGER NOT NULL, PRIMARY KEY(`key`))")
            }
        }
        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `queueobject` (`key` INTEGER, `id` INTEGER NOT NULL," + "`number` TEXT, `eid` TEXT,`isFile` INTEGER NOT NULL,`link` TEXT,`name` TEXT,`aid` TEXT,`time` INTEGER NOT NULL, PRIMARY KEY (`id`))")
            }
        }
        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `queueobject`  ADD COLUMN `uri` TEXT")
            }
        }
        private val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `explorerobject`  ADD COLUMN `aid` TEXT")
            }
        }
        private val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `downloadobject`  ADD COLUMN `headers` TEXT")
            }
        }
        private val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `downloadobject`  ADD COLUMN `did` TEXT")
                db.execSQL("ALTER TABLE `downloadobject`  ADD COLUMN `eta` TEXT")
                db.execSQL("ALTER TABLE `downloadobject`  ADD COLUMN `speed` TEXT")
            }
        }
        private val MIGRATION_8_7: Migration = object : Migration(8, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {

            }
        }
        private val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `downloadobject`  ADD COLUMN `time` INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {

            }
        }
        private val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `recentobject_tmp` (`key` INTEGER NOT NULL, `aid` TEXT, `eid` TEXT, `name` TEXT, `chapter` TEXT, `url` TEXT, `img` TEXT, PRIMARY KEY(`key`))")
                db.execSQL("DROP TABLE `recentobject`")
                db.execSQL("ALTER TABLE `recentobject_tmp` RENAME TO `recentobject`")
            }
        }

        private val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `seeingobject`  ADD COLUMN `state` INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE `achivement` (" +
                            "`key` INTEGER NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`description` TEXT NOT NULL, " +
                            "`icon` INTEGER NOT NULL, " +
                            "`points` INTEGER NOT NULL, " +
                            "`isSecret` INTEGER NOT NULL, " +
                            "`group` TEXT, " +
                            "`time` INTEGER NOT NULL, " +
                            "`count` INTEGER NOT NULL, " +
                            "`goal` INTEGER NOT NULL, " +
                            "`isUnlocked` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`key`))"
                )
            }
        }

        private val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE `achivement_tmp` (" +
                            "`key` INTEGER NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`description` TEXT NOT NULL, " +
                            "`points` INTEGER NOT NULL, " +
                            "`isSecret` INTEGER NOT NULL, " +
                            "`group` TEXT, " +
                            "`time` INTEGER NOT NULL, " +
                            "`count` INTEGER NOT NULL, " +
                            "`goal` INTEGER NOT NULL, " +
                            "`isUnlocked` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`key`))"
                )
                db.execSQL("INSERT INTO `achivement_tmp` (`key`,`name`,`description`,`points`,`isSecret`,`group`,`time`,`count`,`goal`,`isUnlocked`) SELECT `key`,`name`,`description`,`points`,`isSecret`,`group`,`time`,`count`,`goal`,`isUnlocked` FROM `achivement`")
                db.execSQL("DROP TABLE `achivement`")
                db.execSQL("ALTER TABLE `achivement_tmp` RENAME TO `achievement`")
            }
        }

        private val MIGRATION_13_14: Migration = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `animeobject`  ADD COLUMN `followers` TEXT")
            }
        }

        private val MIGRATION_14_15: Migration = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `downloadobject` ADD COLUMN `server` TEXT DEFAULT 'desconocido'")
            }
        }

        private val MIGRATION_15_16: Migration = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE `seenobject` (" +
                            "`eid` TEXT NOT NULL, " +
                            "`aid` TEXT NOT NULL, " +
                            "`number` TEXT NOT NULL, " +
                            "PRIMARY KEY(`eid`))"
                )
            }
        }

        private val MIGRATION_16_17: Migration = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `achievement` ADD COLUMN `isRevealed` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_17_18: Migration = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE `recentmodel` (" +
                            "`key` INTEGER NOT NULL, " +
                            "`aid` TEXT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`chapter` TEXT NOT NULL, " +
                            "`chapterUrl` TEXT NOT NULL, " +
                            "`img` TEXT NOT NULL, " +
                            "PRIMARY KEY(`key`))"
                )
            }
        }

        private val MIGRATION_18_19: Migration = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE `playerstate` (" +
                            "`title` TEXT NOT NULL, " +
                            "`position` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`title`))"
                )
            }
        }
        private val MIGRATION_19_20: Migration = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
            }
        }

        private val MIGRATION_20_21: Migration = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `DirectoryAV1` (
                `aid` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `slug` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `rateStars` REAL NOT NULL,
                `rateCount` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `state` INTEGER NOT NULL,
                `day` INTEGER,
                `genres` TEXT NOT NULL,
                `relations` TEXT NOT NULL,
                `chapters` TEXT NOT NULL,
                PRIMARY KEY(`aid`)
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `FavoriteAV1` (
                `aid` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `slug` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                PRIMARY KEY(`aid`)
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `Organizer` (
                `aid` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `slug` TEXT NOT NULL,
                `lastWatched` INTEGER NOT NULL,
                `state` INTEGER NOT NULL,
                PRIMARY KEY(`aid`)
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `RecentAV1` (
                `key` INTEGER NOT NULL,
                `aid` INTEGER NOT NULL,
                `eid` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `number` REAL NOT NULL,
                `slug` TEXT NOT NULL,
                PRIMARY KEY(`key`)
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `Record` (
                `eid` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `number` REAL NOT NULL,
                `aid` INTEGER NOT NULL,
                `slug` TEXT NOT NULL,
                `date` INTEGER NOT NULL,
                PRIMARY KEY(`eid`)
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `GenreRecord` (
                `slug` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `count` INTEGER NOT NULL,
                `isBlocked` INTEGER NOT NULL,
                PRIMARY KEY(`slug`)
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `CalendarBlacklist` (
                `aid` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `slug` TEXT NOT NULL,
                `day` INTEGER NOT NULL,
                `category` INTEGER NOT NULL,
                PRIMARY KEY(`aid`)
            )
            """.trimIndent()
                )
            }
        }

        val INSTANCE: CacheDB by lazy { init(App.context) }

        private fun init(context: Context): CacheDB =
            Room.databaseBuilder(context, CacheDB::class.java, "cache-db")
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_7,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                    MIGRATION_20_21
                ).build()
    }

}
