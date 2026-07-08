package knf.kuma.pojos.av1

import androidx.core.net.toUri
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.recyclerview.widget.DiffUtil
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import knf.kuma.commons.FileWrapper
import knf.kuma.commons.JsExtractor
import knf.kuma.commons.PrefsUtil.saveWithName
import knf.kuma.commons.roundedString
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.DownloadObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class Recommended(
    @SerializedName("aid")
    val aid: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("type")
    val type: String
) {
    val animeUrl: String get() = "https://animeav1.com/media/$slug"
    val imageUrl: String get() = "https://cdn.animeav1.com/covers/$aid.jpg"
    val typeText: String get() = when (type) {
        "tv-anime" -> "Anime"
        "pelicula" -> "Película"
        "ova" -> "OVA"
        else -> "Especial"
    }
}

data class DirectoryAV1Min(
    val aid: Int,
    val name: String,
    val slug: String,
    val category: Int
) {
    val animeUrl: String get() = "https://animeav1.com/media/$slug"
    val imageUrl: String get() = "https://cdn.animeav1.com/covers/$aid.jpg"
    val type: String get() {
        return when (category) {
            1 -> "Anime"
            2 -> "Película"
            3 -> "OVA"
            else -> "Especial"
        }
    }

    companion object {
        fun fromJson(item: JSONObject) : DirectoryAV1Min {
            return DirectoryAV1Min(
                item.getInt("id"),
                item.getString("title"),
                item.getString("slug"),
                item.getInt("categoryId")
            )
        }

        val DIFF = object : DiffUtil.ItemCallback<DirectoryAV1Min>() {
            override fun areItemsTheSame(
                oldItem: DirectoryAV1Min,
                newItem: DirectoryAV1Min
            ): Boolean {
                return oldItem.aid == newItem.aid
            }

            override fun areContentsTheSame(
                oldItem: DirectoryAV1Min,
                newItem: DirectoryAV1Min
            ): Boolean {
                return true
            }
        }
    }
}

@Entity(tableName = "CalendarBlacklist")
data class DirectoryAV1Calendar(
    @PrimaryKey
    val aid: Int,
    val name: String,
    val slug: String,
    val day: Int,
    val category: Int
) {
    val animeUrl: String get() = "https://animeav1.com/media/$slug"
    val imageUrl: String get() = "https://cdn.animeav1.com/covers/$aid.jpg"

    @Ignore
    var isHidden: Boolean = false
    @Ignore
    var isFavorite: Boolean = false

    fun asMin(): DirectoryAV1Min {
        return DirectoryAV1Min(aid, name, slug, category)
    }

    companion object {
        fun fromJson(item: JSONObject) : DirectoryAV1Calendar {
            return DirectoryAV1Calendar(
                item.getInt("id"),
                item.getString("title"),
                item.getString("slug"),
                item.let {
                    if (item.has("latestEpisode")) {
                        OffsetDateTime.parse(
                            item.getJSONObject("latestEpisode").getString("createdAt")
                        ).dayOfWeek.value
                    } else {
                        -1
                    }
                },
                item.getJSONObject("category").getInt("id")
            )
        }
    }
}

@Entity
@TypeConverters(DirectoryConverter::class)
data class DirectoryAV1(
    @PrimaryKey
    @SerializedName("aid")
    val aid: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("rate_stars")
    val rateStars: Float,
    @SerializedName("rate_count")
    val rateCount: Int,
    @SerializedName("type")
    val type: String,
    @SerializedName("state")
    val state: Int,
    @SerializedName("day")
    val day: Int?,
    @SerializedName("genres")
    val genres: List<Genre>,
    @SerializedName("relations")
    val relations: List<Relation>,
    @SerializedName("chapters")
    val chapters: List<Chapter>
) {
    val animeUrl: String get() = "https://animeav1.com/media/$slug"
    val imageUrl: String get() = "https://cdn.animeav1.com/covers/$aid.jpg"

    val typeText: String get() = when (type) {
        "tv-anime" -> "Anime"
        "pelicula" -> "Película"
        "ova" -> "OVA"
        else -> "Especial"
    }

    fun asFavorite(category: String = FavoriteAV1.CATEGORY_NONE): FavoriteAV1 {
        return FavoriteAV1(
            aid,
            name,
            slug,
            type,
            category
        )
    }

    fun asOrganizer(state: Int): Organizer {
        return Organizer(
            aid,
            name,
            slug,
            -1,
            state
        )
    }

    companion object {
        suspend fun fromJson(item: JSONObject) : DirectoryAV1 {
            return DirectoryAV1(
                item.getInt("id"),
                item.getString("title"),
                item.getString("slug"),
                item.getString("synopsis"),
                item.getDouble("score").toFloat() / 2,
                item.getInt("votes"),
                item.getJSONObject("category").getString("slug"),
                item.getInt("status"),
                item.let {
                    if (item.getInt("status") == 2 && item.getString("startDate") != null) {
                        LocalDate.parse(
                            item.getString("startDate"),
                            DateTimeFormatter.ISO_LOCAL_DATE
                        ).dayOfWeek.value
                    } else {
                        null
                    }
                },
                item.getJSONArray("genres").let {
                    val list = mutableListOf<Genre>()
                    for (j in 0 until it.length()) {
                        val genre = it.getJSONObject(j)
                        list.add(Genre(genre.getString("name"), genre.getString("slug")))
                    }
                    list
                },
                item.getJSONArray("relations").let {
                    val list = mutableListOf<Relation>()
                    for (j in 0 until it.length()) {
                        val relation = it.getJSONObject(j)
                        list.add(
                            Relation(
                                relation.getJSONObject("destination").getInt("id"),
                                relation.getJSONObject("destination").getString("title"),
                                relation.getInt("type"),
                                relation.getJSONObject("destination").getString("slug")
                            )
                        )
                    }
                    list
                },
                item.getJSONArray("episodes").let {
                    val list = mutableListOf<Chapter>()
                    withContext(Dispatchers.IO) {
                        for (j in 0 until it.length()) {
                            val episode = it.getJSONObject(j)
                            list.add(Chapter(episode.getInt("id"), episode.getDouble("number")))
                        }
                    }
                    list
                }
            )
        }
    }
}

data class ChapterWID(
    val eid: Int,
    val number: Double,
    val aid: Int,
    val slug: String,
    val name: String
) {
    val thumbnail: String get() = "https://cdn.animeav1.com/screenshots/$aid/${number.roundedString()}.jpg"
    val link: String get() = "https://animeav1.com/media/$slug/${number.roundedString()}"
    val episodeName: String get() = "Episodio ${number.roundedString()}"
    val fileWrapper: FileWrapper<*> by lazy { FileWrapper.create(filePath()) }
    fun filePath(): String {
        return if (saveWithName) "$eid$$slug-${number.roundedString()}.mp4"
        else "$eid$$aid-${number.roundedString()}.mp4"
    }

    fun asRecord() : Record {
        return Record(
            eid,
            name,
            number,
            aid,
            slug,
            System.currentTimeMillis()
        )
    }

    fun asDownload(addQueue: Boolean = false): DownloadObject = DownloadObject(
        eid.toString(),
        filePath(),
        name,
        episodeName,
        addQueue
    )

    fun asCompatChapter() = AnimeObject.WebInfo.AnimeChapter().apply {
        this.key = this@ChapterWID.eid
        this.number = this@ChapterWID.episodeName
        this.eid = this@ChapterWID.eid.toString()
        this.link = this@ChapterWID.link
        this.aid = this@ChapterWID.aid.toString()
        this.name = this@ChapterWID.name
        this.img = this@ChapterWID.thumbnail
        this.fileWrapper = this@ChapterWID.fileWrapper
    }
}

data class Chapter(
    @SerializedName("eid")
    val eid: Int,
    @SerializedName("number")
    val number: Double
) {
    @Ignore
    var isSeen: Boolean = CacheDB.INSTANCE.recordAV1DAO().chapterIsSeen(eid)
    @Ignore
    val name: String = "Episodio ${number.roundedString()}"
    @Ignore
    private var file: FileWrapper<*>? = null
    fun thumbnail(anime: DirectoryAV1): String = "https://cdn.animeav1.com/screenshots/${anime.aid}/${number.roundedString()}.jpg"
    fun asRecord(anime: DirectoryAV1, date: Long = System.currentTimeMillis()) : Record {
        return Record(
            eid,
            anime.name,
            number,
            anime.aid,
            anime.slug,
            date
        )
    }

    fun asDownload(anime: DirectoryAV1, addQueue: Boolean = false): DownloadObject = DownloadObject(
        eid.toString(),
        filePath(anime),
        anime.name,
        name,
        addQueue
    )

    fun withID(anime: DirectoryAV1): ChapterWID = ChapterWID(eid, number, anime.aid, anime.slug, anime.name)

    fun link(anime: DirectoryAV1): String {
        return "https://animeav1.com/media/${anime.slug}/${number.roundedString()}"
    }

    fun episodeName(anime: DirectoryAV1): String {
        return "${anime.name} - Episodio ${number.roundedString()}"
    }

    fun filePath(anime: DirectoryAV1): String {
        return if (saveWithName) "$${anime.slug}-${number.roundedString()}.mp4"
        else "$${anime.aid}-${number.roundedString()}.mp4"
    }

    fun fileWrapper(anime: DirectoryAV1): FileWrapper<*> {
        return file ?: FileWrapper.create(filePath(anime)).also {
            file = it
        }
    }
}

data class Relation(
    @SerializedName("aid")
    val aid: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("relation")
    val relation: Int,
    @SerializedName("slug")
    val slug: String
) {
    val animeUrl: String get() = "https://animeav1.com/media/$slug"
    val imageUrl: String get() = "https://cdn.animeav1.com/covers/$aid.jpg"
}

data class Genre(
    @SerializedName("name")
    val name: String,
    @SerializedName("slug")
    val slug: String
) {
    fun asRecord(): GenreRecord = GenreRecord(slug, name, 0)
}

data class SearchState(
    var query: String? = null,
    var genres: List<String>? = null
)

class SearchDataSource(val state: SearchState): PagingSource<Int, DirectoryAV1Min>() {
    private val baseUrl = "https://animeav1.com/catalogo?order=title"

    override fun getRefreshKey(state: PagingState<Int, DirectoryAV1Min>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DirectoryAV1Min> {
        return try {
            val page = params.key ?: 1
            val url = baseUrl.toUri().buildUpon()
            if (!state.query.isNullOrEmpty()) {
                url.appendQueryParameter("search", state.query)
            }
            state.genres?.forEach {
                url.appendQueryParameter("genre", it)
            }
            url.appendQueryParameter("page", page.toString())
            val result = JsExtractor.processLink(url.build().toString()) ?: return LoadResult.Error(Exception("Error al obtener"))
            val list = mutableListOf<DirectoryAV1Min>()
            for (i in 0 until result.length()) {
                val item = result.getJSONObject(i)
                list.add(DirectoryAV1Min.fromJson(item))
            }
            LoadResult.Page(list, null, if (list.size < 20) null else page + 1)
        } catch (e: Exception) {
            e.printStackTrace()
            LoadResult.Error(e)
        }
    }
}

@Dao
abstract class DirectoryDao {

    @get:Query("SELECT count(*) FROM DirectoryAV1")
    abstract val count: Int
    @Query("SELECT * FROM DirectoryAV1 WHERE aid = :aid")
    abstract fun findByAid(aid: Int): DirectoryAV1?

    @Query("SELECT * FROM DirectoryAV1 WHERE slug = :slug")
    abstract fun findBySlug(slug: String): DirectoryAV1?

    @Query("SELECT * FROM DirectoryAV1 WHERE slug IN (:slugs)")
    abstract fun findAllBySlug(slugs: List<String>): List<DirectoryAV1>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun add(directoryAV1: DirectoryAV1)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addAll(directoryAV1: List<DirectoryAV1>)

    @Query("DELETE FROM DirectoryAV1")
    abstract fun nuke()
}

@Dao
interface CalendarDao {

    @get:Query("SELECT aid FROM CalendarBlacklist")
    val allAidsFlow: Flow<List<Int>>

    @get:Query("SELECT aid FROM CalendarBlacklist")
    val allAids: List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(directoryAV1: DirectoryAV1Calendar)

    @Delete
    suspend fun remove(directoryAV1: DirectoryAV1Calendar)
}

class DirectoryConverter {
    @TypeConverter
    fun chaptersToString(chapters: List<Chapter>): String {
        return chapters.joinToString(";") { "${it.eid}:${it.number}" }
    }
    @TypeConverter
    fun stringToChapters(string: String): List<Chapter> {
        return string.split(";").map {
            val (eid, number) = it.split(":")
            Chapter(eid.toInt(), number.toDouble())
        }
    }
    @TypeConverter
    fun genresToString(genres: List<Genre>): String {
        return genres.joinToString(";") { "${it.name}:${it.slug}" }
    }
    @TypeConverter
    fun stringToGenres(string: String): List<Genre> {
        return string.split(";").mapNotNull {
            try {
                val (name, slug) = it.split(":")
                Genre(name, slug)
            } catch (e: Exception) {
                null
            }
        }
    }
    @TypeConverter
    fun relationsToString(relations: List<Relation>?): String {
        return Gson().toJson(relations?:emptyList<Relation>())
    }

    @TypeConverter
    fun stringToRelations(string: String): List<Relation> {
        return Gson().fromJson(string, Array<Relation>::class.java).toList()
    }

}
