package knf.kuma.pojos.av1

import androidx.annotation.Keep
import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.recyclerview.widget.DiffUtil
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import com.google.firebase.firestore.Exclude
import com.google.gson.annotations.SerializedName
import knf.kuma.commons.FileWrapper
import knf.kuma.commons.PrefsUtil.saveWithName
import knf.kuma.commons.roundedString
import knf.kuma.database.BaseConverter
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.ExplorerObject
import kotlinx.coroutines.flow.Flow

@Keep
@Entity
data class Record(
    @PrimaryKey
    @SerializedName("eid")
    val eid: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("number")
    val number: Double,
    @SerializedName("aid")
    val aid: Int,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("date")
    val date: Long
) {

    constructor(): this(0, "", 0.0, 0, "", 0L)


    val animeUrl: String @Exclude get() = "https://animeav1.com/media/$slug"
    val imageUrl: String @Exclude get() = "https://cdn.animeav1.com/covers/$aid.jpg"
    val chapterUrl: String @Exclude get() = "https://animeav1.com/media/$slug/${number.roundedString()}"
    val chapter: String @Exclude get() = "Episodio ${number.roundedString()}"
    val fileWrapper: FileWrapper<*> by lazy { FileWrapper.create(getFilePath()) }
    private fun getFilePath(): String {
        return if (saveWithName) "$eid$$slug-${number.roundedString()}.mp4"
        else "$eid$$aid-${number.roundedString()}.mp4"
    }
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Record>() {
            override fun areItemsTheSame(
                oldItem: Record,
                newItem: Record
            ): Boolean {
                return oldItem.eid == newItem.eid
            }

            override fun areContentsTheSame(
                oldItem: Record,
                newItem: Record
            ): Boolean {
                return true
            }
        }

        fun fromDownloaded(obj: ExplorerObject.FileDownObj) = Record(
            obj.eid.toInt(),
            obj.title,
            obj.chapter.toDouble(),
            obj.aid.toInt(),
            obj.slug,
            System.currentTimeMillis()
        )

        fun fromLegacyChapter(obj: AnimeObject.WebInfo.AnimeChapter) = Record(
            obj.key,
            obj.name,
            obj.number.trim().substringAfterLast(" ").toDouble(),
            obj.aid.toInt(),
            obj.fileWrapper?.file()?.name?: CacheDB.INSTANCE.directoryDAO().findByAid(obj.aid.toInt())?.slug ?: "",
            System.currentTimeMillis()
        )
    }
}

@Dao
@TypeConverters(BaseConverter::class)
abstract class RecordDao {
    @get:Query("SELECT * FROM Record")
    abstract val all: MutableList<Record>

    @get:Query("SELECT * FROM Record")
    abstract val allFlow: Flow<List<Record>>

    @get:Query("SELECT * FROM Record ORDER BY date DESC")
    abstract val allPaged: PagingSource<Int, Record>

    @get:Query("SELECT *, MAX(number) FROM Record GROUP BY aid ORDER BY date DESC")
    abstract val allPagedMaxOnly: PagingSource<Int, Record>

    @get:Query("SELECT count(*) FROM Record")
    abstract val countFlow: Flow<Int>

    @get:Query("SELECT count(*) FROM Record")
    abstract val countLive: LiveData<Int>

    @get:Query("SELECT count(*) FROM Record")
    abstract val count: Int

    @get:Query("SELECT MIN(date) FROM Record")
    abstract val lastDate: Long?

    @Query("SELECT * FROM Record WHERE aid = :aid AND number = :number LIMIT 1")
    abstract fun chapterSeen(aid: Int, number: Double): Flow<Record?>

    @Query("SELECT count(*) FROM Record WHERE aid = :aid AND number = :number LIMIT 1")
    abstract fun chapterIsSeenFlow(aid: Int, number: Double): Flow<Boolean>

    @Query("SELECT count(*) FROM Record WHERE aid = :aid AND number = :number")
    abstract fun chapterIsSeen(aid: Int, number: Double): Boolean

    @Query("SELECT count(*) FROM Record WHERE eid = :eid")
    abstract fun chapterIsSeen(eid: Int): Boolean

    @Query("SELECT * FROM Record WHERE eid IN (:eids) ORDER BY number DESC LIMIT 1")
    abstract fun getLast(eids: List<Int>): Record?

    @Query("SELECT * FROM Record WHERE aid = :aid ORDER BY number DESC LIMIT 1")
    abstract suspend fun getLastByAid(aid: Int): Record?

    @Query("SELECT * FROM Record WHERE eid IN (:eids)")
    abstract fun getAllFrom(eids: List<Int>): List<Record>

    @Query("SELECT * FROM Record WHERE aid = :aid")
    abstract fun getAllByAid(aid: Int): List<Record>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addChapter(chapter: Record)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun addChapterIgnore(chapter: Record)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addAll(list: List<Record>)

    @Delete
    abstract fun delete(chapter: Record)

    @Query("DELETE FROM Record WHERE aid = :aid AND number = :number")
    abstract fun deleteChapter(aid: Int, number: Double)

    @Query("DELETE FROM Record")
    abstract fun clear()
}
