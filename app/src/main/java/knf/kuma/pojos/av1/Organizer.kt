package knf.kuma.pojos.av1

import androidx.annotation.Keep
import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.recyclerview.widget.DiffUtil
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Update
import com.google.gson.annotations.SerializedName
import knf.kuma.database.BaseConverter
import kotlinx.coroutines.flow.Flow

@Keep
@Entity
data class Organizer(
    @PrimaryKey
    @SerializedName("aid")
    val aid: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("last_watched")
    val lastWatched: Int,
    @SerializedName("state")
    var state: Int
) {

    constructor(): this(0, "", "", 0, 0)

    fun animeUrl() = "https://animeav1.com/media/$slug"
    fun imageUrl() = "https://cdn.animeav1.com/covers/$aid.jpg"

    fun isValid(): Boolean = aid != 0
}

data class OrganizerWRecord(
    @Embedded val organizer: Organizer,
    @Embedded(prefix = "record_") val lastChapter: Record?
) {
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<OrganizerWRecord>() {
            override fun areItemsTheSame(
                oldItem: OrganizerWRecord,
                newItem: OrganizerWRecord
            ): Boolean {
                return oldItem.organizer.aid == newItem.organizer.aid
            }

            override fun areContentsTheSame(
                oldItem: OrganizerWRecord,
                newItem: OrganizerWRecord
            ): Boolean {
                return oldItem.lastChapter?.eid == newItem.lastChapter?.eid
            }
        }
    }
}

@Dao
@TypeConverters(BaseConverter::class)
abstract class OrganizerDao {

    @get:Query(QUERY_ALL)
    abstract val all: Flow<List<OrganizerWRecord>>

    @get:Query("$QUERY_ALL$ORDER_NAME")
    abstract val allPaging: PagingSource<Int, OrganizerWRecord>

    @get:Query("SELECT * FROM Organizer ORDER BY name")
    abstract val allSimplePaging: PagingSource<Int, Organizer>

    @get:Query("SELECT * FROM Organizer")
    abstract val allRaw: List<Organizer>

    @get:Query("SELECT aid FROM Organizer")
    abstract val allAids: List<Int>

    @Query("$QUERY_ALL WHERE o.state=:state$ORDER_NAME")
    abstract fun getFlowByState(state: Int): Flow<List<OrganizerWRecord>>

    @Query("SELECT count(*) FROM Organizer WHERE state=:state")
    abstract suspend fun countByState(state: Int): Int

    @Query("$QUERY_ALL WHERE o.state=:state$ORDER_NAME")
    abstract fun getByStatePaging(state: Int): PagingSource<Int, OrganizerWRecord>

    @Query("SELECT * FROM Organizer WHERE state=:state ORDER BY name")
    abstract fun getSimpleByStatePaging(state: Int): PagingSource<Int, Organizer>

    @get:Query("SELECT count(*) FROM Organizer")
    abstract val countFlow: Flow<Int>

    @get:Query("SELECT count(*) FROM Organizer")
    abstract val countLive: LiveData<Int>

    @get:Query("SELECT count(*) FROM Organizer")
    abstract val countAll: Int

    @get:Query("SELECT count(*) FROM Organizer WHERE state=1")
    abstract val countWatchingFlow: Flow<Int>

    @get:Query("SELECT count(*) FROM Organizer WHERE state=3")
    abstract val countCompletedFlow: Flow<Int>

    @get:Query("SELECT count(*) FROM Organizer WHERE state=1")
    abstract val countWatchingLive: LiveData<Int>

    @get:Query("SELECT count(*) FROM Organizer WHERE state=3")
    abstract val countCompletedLive: LiveData<Int>

    @get:Query("SELECT count(*) FROM Organizer WHERE state=4")
    abstract val countDroppedLive: LiveData<Int>

    @Query("$QUERY_ALL WHERE o.aid = :aid$ORDER_NAME")
    abstract fun getByAid(aid: Int): OrganizerWRecord?

    @Query("$QUERY_ALL WHERE o.state IN (:states) ORDER BY RANDOM() LIMIT 10")
    abstract fun getAllWState(vararg states: Int): Flow<List<OrganizerWRecord>>

    @Query("SELECT count(*) FROM Organizer WHERE aid = :aid AND state>0 AND state <3")
    abstract fun isSeeing(aid: Int): Boolean

    @Query("SELECT count(*) FROM Organizer WHERE aid = :aid")
    abstract fun isSeeingAll(aid: Int): Boolean

    @Query("SELECT count(*) FROM seeingobject WHERE aid IN (:list) AND state=3")
    abstract fun isAnimeCompleted(list: List<Int>): Flow<Int>

    @Query("SELECT count(*) FROM seeingobject WHERE aid IN (:list) AND state=3")
    abstract fun isAnimeCompletedLive(list: List<Int>): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun add(organizer: Organizer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addAll(list: List<Organizer>)

    @Update
    abstract fun update(organizer: Organizer)

    @Delete
    abstract fun remove(organizer: Organizer)

    @Query("DELETE FROM ORGANIZER")
    abstract fun clear()

    companion object {
        private const val QUERY_ALL = """
            SELECT 
                o.*, 
                r.eid AS record_eid, 
                r.name AS record_name, 
                r.number AS record_number, 
                r.slug AS record_slug, 
                r.aid AS record_aid, 
                r.date AS record_date
            FROM Organizer o
            LEFT JOIN Record r ON o.aid = r.aid AND r.number = (
                SELECT MAX(number) 
                FROM Record 
                WHERE aid = o.aid
            )
        """

        private const val ORDER_NAME = " ORDER BY o.name ASC"
    }
}
