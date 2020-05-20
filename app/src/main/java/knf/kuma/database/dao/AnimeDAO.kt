package knf.kuma.database.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import knf.kuma.backup.objects.AnimeChapters
import knf.kuma.database.BaseConverter
import knf.kuma.directory.DirObject
import knf.kuma.emision.AnimeSubObject
import knf.kuma.pojos.AnimeObject
import knf.kuma.random.RandomObject
import knf.kuma.recommended.AnimeShortObject
import knf.kuma.search.SearchAdvObject
import knf.kuma.search.SearchObject
import knf.kuma.slices.AnimeSliceObject
import knf.kuma.tv.search.BasicAnimeObject

@Dao
@TypeConverters(BaseConverter::class, AnimeObject.Converter::class)
interface AnimeDAO {

    @get:Query("SELECT `key`,aid,name,link FROM AnimeObject ORDER BY name")
    val allSearch: DataSource.Factory<Int, SearchObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject ORDER BY name")
    val allLive: LiveData<List<DirObject>>

    @get:Query("SELECT link FROM AnimeObject WHERE state LIKE 'En emisión'")
    val allLinksInEmission: List<String>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'Anime' ORDER BY name")
    val animeDir: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'Anime' ORDER BY rate_stars DESC")
    val animeDirVotes: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'Anime' AND state LIKE 'En emisión' ORDER BY rate_stars DESC, rate_count+0 DESC LIMIT 10")
    val emissionVotesLimited: LiveData<List<DirObject>>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'Anime' ORDER BY rate_stars DESC, rate_count+0 DESC LIMIT 20")
    val allVotesLimited: LiveData<List<DirObject>>

    @Query("SELECT `key`,name,link,aid,img,type FROM AnimeObject WHERE aid IN (:aids) ORDER BY RANDOM() LIMIT 10")
    fun animesWithIDRandom(aids: List<String>): List<SearchAdvObject>

    @Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE aid IN (:aids) ORDER BY RANDOM() LIMIT 15")
    fun animesDirWithIDRandom(aids: List<String>): List<DirObject>

    @Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE aid IN (:aids) ORDER BY RANDOM()")
    fun animesDirWithIDRandomNL(aids: List<String>): LiveData<List<DirObject>>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'Anime' ORDER BY `key` ASC")
    val animeDirID: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'Anime' ORDER BY `key` DESC")
    val animeDirAdded: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'Anime' ORDER BY followers+0 DESC")
    val animeDirFollowers: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'OVA' OR type LIKE '%special' ORDER BY name")
    val ovaDir: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'OVA' OR type LIKE '%special' ORDER BY rate_stars DESC")
    val ovaDirVotes: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'OVA' OR type LIKE '%special' ORDER BY `key` ASC")
    val ovaDirID: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'OVA' OR type LIKE '%special' ORDER BY `key` DESC")
    val ovaDirAdded: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'OVA' OR type LIKE '%special' ORDER BY followers+0 DESC")
    val ovaDirFollowers: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'Película' ORDER BY name")
    val movieDir: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'Película' ORDER BY rate_stars DESC")
    val movieDirVotes: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'Película' ORDER BY `key` ASC")
    val movieDirID: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'Película' ORDER BY `key` DESC")
    val movieDirAdded: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE type LIKE 'Película' ORDER BY followers+0 DESC")
    val movieDirFollowers: DataSource.Factory<Int, DirObject>

    @get:Query("SELECT count(*) FROM AnimeObject")
    val count: Int

    @get:Query("SELECT count(*) FROM AnimeObject")
    val countLive: LiveData<Int>

    @Query("SELECT count(*) FROM AnimeObject")
    fun init(): Int

    @Query("SELECT * FROM AnimeObject WHERE aid = :aid")
    fun getAnimeByAid(aid: String): AnimeObject?

    @Query("SELECT `key`,aid,img,link,name,type FROM AnimeObject WHERE aid IN (:aids) ORDER BY name")
    fun getAnimesByAids(aids: List<String>): List<AnimeShortObject>

    @Query("SELECT * FROM AnimeObject WHERE link = :link")
    fun getAnimeRaw(link: String): AnimeObject?

    @Query("SELECT `key`,name,link,aid,type FROM AnimeObject ORDER BY RANDOM() LIMIT :limit")
    fun getRandom(limit: Int): List<RandomObject>

    @Query("SELECT `key`,name,link,aid FROM AnimeObject WHERE state LIKE 'En emisión' AND day = :day AND aid NOT IN (:list) ORDER BY name")
    fun getByDay(day: Int, list: Set<String>): LiveData<MutableList<SearchObject>>

    @Query("SELECT `key`,name,link,aid FROM AnimeObject WHERE state LIKE 'En emisión' AND day = :day ORDER BY name")
    fun getByDay(day: Int): LiveData<MutableList<SearchObject>>

    @Query("SELECT `key`,aid,name,link,rate_stars,type FROM AnimeObject WHERE state LIKE 'En emisión' AND day = :day ORDER BY name")
    fun getByDayDir(day: Int): LiveData<MutableList<DirObject>>

    @Query("SELECT count(*) FROM AnimeObject WHERE state = 'En emisión' AND NOT day = 0 AND aid NOT IN (:list)")
    fun getInEmission(list: Set<String>): LiveData<Int>

    @Query("SELECT `key`,link,name,aid FROM AnimeObject WHERE state LIKE 'En emisión' AND day = :day AND aid NOT IN (:list) ORDER BY name")
    fun getByDayDirect(day: Int, list: Set<String>): MutableList<SearchObject>

    @Query("SELECT `key`,aid,name,link FROM AnimeObject WHERE name LIKE :query ORDER BY name")
    fun getSearch(query: String): DataSource.Factory<Int, SearchObject>

    @Query("SELECT `key`,aid,name,link FROM AnimeObject WHERE name LIKE :query ORDER BY name")
    fun getSearchList(query: String): LiveData<MutableList<BasicAnimeObject>>

    @Query("SELECT `key`,aid,name,link FROM AnimeObject WHERE aid LIKE :query ORDER BY name")
    fun getSearchID(query: String): DataSource.Factory<Int, SearchObject>

    @Query("SELECT `key`,aid,name,link FROM AnimeObject WHERE genres LIKE :genres ORDER BY name")
    fun getSearchG(genres: String): DataSource.Factory<Int, SearchObject>

    @Query("SELECT `key`,name,link,aid FROM AnimeObject WHERE genres LIKE :genre ORDER BY name")
    fun getAllGenre(genre: String): DataSource.Factory<Int, SearchObject>

    @Query("SELECT `key`,aid,name,link FROM AnimeObject WHERE genres LIKE :genre ORDER BY name")
    fun getAllGenreLive(genre: String): LiveData<List<BasicAnimeObject>>

    @Query("SELECT aid FROM AnimeObject WHERE genres LIKE :genres")
    fun getAidsByGenres(genres: String): MutableList<String>

    @Query("SELECT aid FROM AnimeObject WHERE genres LIKE :genres ORDER BY RANDOM() LIMIT 15")
    fun getAidsByGenresLimited(genres: String): MutableList<String>

    @Query("SELECT `key`,aid,name,link FROM AnimeObject WHERE name LIKE :query AND genres LIKE :genres ORDER BY name")
    fun getSearchTG(query: String, genres: String): DataSource.Factory<Int, SearchObject>

    @Query("SELECT `key`,aid,name,link FROM AnimeObject WHERE name LIKE :query AND state LIKE :state ORDER BY name")
    fun getSearchS(query: String, state: String): DataSource.Factory<Int, SearchObject>

    @Query("SELECT `key`,aid,name,link FROM AnimeObject WHERE name LIKE :query AND state LIKE :state AND genres LIKE :genres ORDER BY name")
    fun getSearchSG(query: String, state: String, genres: String): DataSource.Factory<Int, SearchObject>

    @Query("SELECT `key`,aid,name,link FROM AnimeObject WHERE name LIKE :query AND type LIKE :type ORDER BY name")
    fun getSearchTY(query: String, type: String): DataSource.Factory<Int, SearchObject>

    @Query("SELECT `key`,aid,name,link FROM AnimeObject WHERE name LIKE :query AND type LIKE :type AND genres LIKE :genres ORDER BY name")
    fun getSearchTYG(query: String, type: String, genres: String): DataSource.Factory<Int, SearchObject>

    @Query("SELECT `key`,link,name,aid,fileName FROM AnimeObject WHERE fileName IN (:names) OR aid IN (:names)")
    fun getAllByFile(names: MutableList<String>): MutableList<AnimeSubObject>

    @Query("SELECT `key`,name,link,aid,genres FROM AnimeObject WHERE name LIKE :name ORDER BY name COLLATE NOCASE LIMIT 5")
    fun getByName(name: String): MutableList<AnimeSliceObject>

    @Query("SELECT count(*) FROM AnimeObject WHERE link LIKE :link")
    fun existLink(link: String): Boolean

    @Query("SELECT count(*) FROM animeobject WHERE aid = :aid AND genres LIKE :genre")
    fun hasGenre(aid: String, genre: String): Boolean

    @Query("SELECT count(*) FROM animeobject WHERE aid = :aid AND state = 'Finalizado'")
    fun isCompleted(aid: String): Boolean

    @Query("SELECT `key`,name,link,aid FROM AnimeObject WHERE sid = :sid")
    fun getBySid(sid: String): SearchObject?

    @Query("SELECT `key`,name,link,aid FROM AnimeObject WHERE link = :link")
    fun getByLink(link: String): SearchObject?

    @Query("SELECT `key`,name,link,aid,img,type FROM AnimeObject WHERE aid LIKE :aid")
    fun getByAid(aid: String): SearchAdvObject?

    @Query("SELECT `key`,name,link,aid FROM AnimeObject WHERE aid LIKE :aid")
    fun getByAidSimple(aid: String): SearchObject?

    @Query("SELECT * FROM AnimeObject WHERE aid LIKE :aid")
    fun getFullByAid(aid: String): AnimeObject?

    @Query("SELECT aid,chapters FROM AnimeObject WHERE aid = :aid")
    fun getChaptersByAid(aid: String): AnimeChapters

    @Query("SELECT `key`,name,link,aid FROM AnimeObject WHERE aid = :aid")
    fun getSOByAid(aid: String): SearchObject?

    @Query("SELECT count(*) FROM AnimeObject WHERE `key` LIKE :aid")
    fun getCount(aid: Int): Int

    @Update
    fun updateAnime(animeObject: AnimeObject)

    @Update
    fun updateAnimes(animeObjects: List<AnimeObject>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(animeObject: AnimeObject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(objects: List<AnimeObject>)

    @Query("DELETE FROM animeobject")
    fun nuke()

    @Query("DELETE FROM animeobject WHERE UPPER(genres) LIKE '%ECCHI%'")
    fun nukeEcchi()

}
