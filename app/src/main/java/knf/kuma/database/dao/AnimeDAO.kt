package knf.kuma.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.TypeConverters
import knf.kuma.database.BaseConverter
import knf.kuma.pojos.AnimeObject
import knf.kuma.search.SearchObject

@Dao
@TypeConverters(BaseConverter::class, AnimeObject.Converter::class)
interface AnimeDAO {
    @Query("SELECT aid FROM animeobject WHERE name = :name")
    fun findAidByName(name: String): String?

    @Query("SELECT `key`,name,link,aid FROM AnimeObject WHERE aid LIKE :aid")
    fun getByAidSimple(aid: String): SearchObject?

    @Query("SELECT `key`,name,link,aid FROM AnimeObject WHERE aid = :aid")
    fun getSOByAid(aid: String): SearchObject?

    @Query("SELECT count(*) FROM AnimeObject WHERE `key` LIKE :aid")
    fun getCount(aid: Int): Int

}
