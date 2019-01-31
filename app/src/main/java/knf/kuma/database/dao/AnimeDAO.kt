package knf.kuma.database.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import knf.kuma.database.BaseConverter
import knf.kuma.pojos.AnimeObject

@Dao
@TypeConverters(BaseConverter::class)
interface AnimeDAO {

    @get:Query("SELECT * FROM AnimeObject")
    val allList: LiveData<MutableList<AnimeObject>>

    @get:Query("SELECT count(*) FROM AnimeObject")
    val allListCount: LiveData<Int>

    @get:Query("SELECT * FROM AnimeObject ORDER BY name")
    val all: DataSource.Factory<Int, AnimeObject>

    @get:Query("SELECT * FROM AnimeObject WHERE state LIKE 'En emisión'")
    val allInEmission: List<AnimeObject>

    @get:Query("SELECT * FROM AnimeObject WHERE type LIKE 'Anime' ORDER BY name")
    val animeDir: DataSource.Factory<Int, AnimeObject>

    @get:Query("SELECT * FROM AnimeObject WHERE type LIKE 'Anime' ORDER BY rate_stars DESC")
    val animeDirVotes: DataSource.Factory<Int, AnimeObject>

    @get:Query("SELECT * FROM AnimeObject WHERE type LIKE 'Anime' ORDER BY `key` ASC")
    val animeDirID: DataSource.Factory<Int, AnimeObject>

    @get:Query("SELECT * FROM AnimeObject WHERE type LIKE 'Anime' ORDER BY `key` DESC")
    val animeDirAdded: DataSource.Factory<Int, AnimeObject>

    @get:Query("SELECT * FROM AnimeObject WHERE type LIKE 'OVA' OR type LIKE '%special' ORDER BY name")
    val ovaDir: DataSource.Factory<Int, AnimeObject>

    @get:Query("SELECT * FROM AnimeObject WHERE type LIKE 'OVA' OR type LIKE '%special' ORDER BY rate_stars DESC")
    val ovaDirVotes: DataSource.Factory<Int, AnimeObject>

    @get:Query("SELECT * FROM AnimeObject WHERE type LIKE 'OVA' OR type LIKE '%special' ORDER BY `key` ASC")
    val ovaDirID: DataSource.Factory<Int, AnimeObject>

    @get:Query("SELECT * FROM AnimeObject WHERE type LIKE 'OVA' OR type LIKE '%special' ORDER BY `key` DESC")
    val ovaDirAdded: DataSource.Factory<Int, AnimeObject>

    @get:Query("SELECT * FROM AnimeObject WHERE type LIKE 'Película' ORDER BY name")
    val movieDir: DataSource.Factory<Int, AnimeObject>

    @get:Query("SELECT * FROM AnimeObject WHERE type LIKE 'Película' ORDER BY rate_stars DESC")
    val movieDirVotes: DataSource.Factory<Int, AnimeObject>

    @get:Query("SELECT * FROM AnimeObject WHERE type LIKE 'Película' ORDER BY `key` ASC")
    val movieDirID: DataSource.Factory<Int, AnimeObject>

    @get:Query("SELECT * FROM AnimeObject WHERE type LIKE 'Película' ORDER BY `key` DESC")
    val movieDirAdded: DataSource.Factory<Int, AnimeObject>

    @get:Query("SELECT count(*) FROM AnimeObject")
    val count: Int

    @get:Query("SELECT count(*) FROM AnimeObject")
    val countLive: LiveData<Int>

    @Query("SELECT count(*) FROM AnimeObject")
    fun init(): Int

    @Query("SELECT * FROM AnimeObject WHERE link = :link")
    fun getAnime(link: String): LiveData<AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE aid = :aid")
    fun getAnimeByAid(aid: String): LiveData<AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE link = :link")
    fun getAnimeRaw(link: String): AnimeObject?

    @Query("SELECT * FROM AnimeObject ORDER BY RANDOM() LIMIT :limit")
    fun getRandom(limit: Int): LiveData<MutableList<AnimeObject>>

    @Query("SELECT * FROM AnimeObject WHERE state LIKE 'En emisión' AND day = :day AND aid NOT IN (:list) ORDER BY name")
    fun getByDay(day: Int, list: Set<String>): LiveData<MutableList<AnimeObject>>

    @Query("SELECT * FROM AnimeObject WHERE state LIKE 'En emisión' AND day = :day AND aid NOT IN (:list) ORDER BY name")
    fun getByDayInline(day: Int, list: Set<String>): MutableList<AnimeObject>

    @Query("SELECT count(*) FROM AnimeObject WHERE state = 'En emisión' AND NOT day = 0 AND aid NOT IN (:list)")
    fun getInEmission(list: Set<String>): LiveData<Int>

    @Query("SELECT * FROM AnimeObject WHERE state LIKE 'En emisión' AND day = :day AND aid NOT IN (:list) ORDER BY name")
    fun getByDayDirect(day: Int, list: Set<String>): MutableList<AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query ORDER BY name")
    fun getSearch(query: String): DataSource.Factory<Int, AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query ORDER BY name")
    fun getSearchList(query: String): LiveData<MutableList<AnimeObject>>

    @Query("SELECT * FROM AnimeObject WHERE aid LIKE :query ORDER BY name")
    fun getSearchID(query: String): DataSource.Factory<Int, AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE genres LIKE :genres ORDER BY name")
    fun getSearchG(genres: String): DataSource.Factory<Int, AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE genres LIKE :genre ORDER BY name")
    fun getAllGenre(genre: String): DataSource.Factory<Int, AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE genres LIKE :genre ORDER BY name")
    fun getAllGenreLive(genre: String): LiveData<List<AnimeObject>>

    @Query("SELECT * FROM AnimeObject WHERE genres LIKE :genres ORDER BY name")
    fun getByGenres(genres: String): MutableList<AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query AND genres LIKE :genres ORDER BY name")
    fun getSearchTG(query: String, genres: String): DataSource.Factory<Int, AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query AND state LIKE :state ORDER BY name")
    fun getSearchS(query: String, state: String): DataSource.Factory<Int, AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query AND state LIKE :state AND genres LIKE :genres ORDER BY name")
    fun getSearchSG(query: String, state: String, genres: String): DataSource.Factory<Int, AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query AND type LIKE :type ORDER BY name")
    fun getSearchTY(query: String, type: String): DataSource.Factory<Int, AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :query AND type LIKE :type AND genres LIKE :genres ORDER BY name")
    fun getSearchTYG(query: String, type: String, genres: String): DataSource.Factory<Int, AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE fileName LIKE :file")
    fun getByFile(file: String): AnimeObject?

    @Query("SELECT * FROM AnimeObject WHERE fileName IN (:names) OR aid IN (:names)")
    fun getAllByFile(names: MutableList<String>): MutableList<AnimeObject>

    @Query("SELECT * FROM AnimeObject WHERE name LIKE :name ORDER BY name COLLATE NOCASE LIMIT 5")
    fun getByName(name: String): MutableList<AnimeObject>

    @Query("SELECT count(*) FROM AnimeObject WHERE sid LIKE :sid")
    fun existSid(sid: String): Boolean

    @Query("SELECT count(*) FROM AnimeObject WHERE link LIKE :link")
    fun existLink(link: String): Boolean

    @Query("SELECT count(*) FROM animeobject WHERE aid = :aid AND genres LIKE :genre")
    fun hasGenre(aid: String, genre: String): Boolean

    @Query("SELECT count(*) FROM animeobject WHERE aid = :aid AND state = 'Finalizado'")
    fun isCompleted(aid: String): Boolean

    @Query("SELECT * FROM AnimeObject WHERE link LIKE :link")
    fun getByLink(link: String): AnimeObject?

    @Query("SELECT * FROM AnimeObject WHERE sid = :sid")
    fun getBySid(sid: String): AnimeObject?

    @Query("SELECT * FROM AnimeObject WHERE aid LIKE :aid")
    fun getByAid(aid: String): AnimeObject?

    @Query("SELECT count(*) FROM AnimeObject WHERE `key` LIKE :aid")
    fun getCount(aid: Int): Int

    @Update
    fun updateAnime(animeObject: AnimeObject)

    @Update
    fun updateAnimes(animeObjects: List<AnimeObject>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(animeObject: AnimeObject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(objects: MutableList<AnimeObject>)

    @Query("DELETE FROM animeobject")
    fun nuke()

}
