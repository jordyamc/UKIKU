package knf.kuma.pojos.av1

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import knf.kuma.R
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.FileWrapper
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.PrefsUtil.saveWithName
import knf.kuma.commons.roundedString
import knf.kuma.database.CacheDB
import knf.kuma.database.CacheDBWrap
import knf.kuma.pojos.DownloadObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.json.JSONObject

open class BaseRecentAV1

data class AdRecentAV1(val adID: String) : BaseRecentAV1()

@Entity
data class RecentAV1(
    @PrimaryKey
    val key: Int,
    val aid: Int,
    val eid: Int,
    val name: String,
    val number: Double,
    val slug: String
) : BaseRecentAV1() {
    val animeUrl: String get() = "https://animeav1.com/media/$slug"
    val animeImageUrl: String get() = "https://cdn.animeav1.com/covers/$aid.jpg"
    val episodeImageUrl: String get() = "https://cdn.animeav1.com/screenshots/$aid/${number.roundedString()}.jpg"
    val chapterUrl: String get() = "https://animeav1.com/media/$slug/${number.roundedString()}"
    val chapter: String get() = "Episodio ${number.roundedString()}"
    val nameChapter: String get() = "$name - ${number.roundedString()}"

    @Ignore
    val state: RecentState = RecentState(this)

    fun getFilePath(): String {
        return if (saveWithName) "$eid$$slug-${number.roundedString()}.mp4"
        else "$eid$$aid-${number.roundedString()}.mp4"
    }

    fun asRecord(): Record = Record(
        eid = eid,
        aid = aid,
        name = name,
        number = number,
        slug = slug,
        date = System.currentTimeMillis()
    )

    fun asChapter() = ChapterWID(
        eid,
        number,
        aid,
        slug,
        name
    )

    fun asDownload(): DownloadObject = DownloadObject(
        eid.toString(),
        getFilePath(),
        name,
        chapter,
        false
    )

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<RecentAV1>() {
            override fun areItemsTheSame(oldItem: RecentAV1, newItem: RecentAV1): Boolean =
                oldItem.eid == newItem.eid

            override fun areContentsTheSame(oldItem: RecentAV1, newItem: RecentAV1): Boolean =
                oldItem == newItem
        }

        fun fromJson(key: Int, json: JSONObject) = RecentAV1(
            key,
            json.getJSONObject("media").getInt("id"),
            json.getInt("id"),
            json.getJSONObject("media").getString("title"),
            json.getDouble("number"),
            json.getJSONObject("media").getString("slug")
        )
    }

    class RecentState(val model: RecentAV1) {
        val isNew = model.number < 2
        val fileWrapper: FileWrapper<*> = FileWrapper.create(model.getFilePath())
        var isFavorite = CacheDB.INSTANCE.favoriteAV1DAO().isFav(model.aid)
        val favoriteFlow =
            CacheDB.INSTANCE.favoriteAV1DAO().isFavFlow(model.aid).distinctUntilChanged()
        var isSeen = CacheDB.INSTANCE.recordAV1DAO().chapterIsSeen(model.aid, model.number)
        val seenFlow = CacheDB.INSTANCE.recordAV1DAO().chapterIsSeenFlow(model.aid, model.number)
            .distinctUntilChanged()
        var downloadObject: DownloadObject? =
            CacheDBWrap.INSTANCE.downloadsDAO().getByEid(model.eid.toString())
        var isDownloading: Boolean =
            downloadObject != null && downloadObject!!.state == DownloadObject.DOWNLOADING
        var downloadState: Int = downloadObject?.state ?: -8
        var isDeleting = false
        val downloadFlow = CacheDB.INSTANCE.downloadsDAO().getFlowByEid(model.eid.toString())
        var isDownloaded: Boolean
            get() = fileWrapper.exist
            set(value) {
                fileWrapper.exist = value
            }
        val checkIsDownloaded: Boolean get() = fileWrapper.existForced()
        val canPlay: Boolean get() = downloadObject?.isDownloadingOrPaused == false && checkIsDownloaded
    }
}

val RecentAV1.menuHideList: List<Int>
    get() = mutableListOf<Int>().apply {
        if (PrefsUtil.recentActionType == "0")
            add(R.id.streaming)
        if (PrefsUtil.recentActionType == "1" || state.downloadObject?.isDownloadingOrPaused == true || state.canPlay)
            add(R.id.download)
        if (state.isDeleting || !state.canPlay)
            add(R.id.delete)
    }

fun RecentAV1.openInfo(context: Context) {
    context.startActivity(Intent(context, DesignUtils.infoClass).apply {
        data = this@openInfo.animeUrl.toUri()
        putExtra("title", name)
        putExtra("img", animeImageUrl)
    })
}

fun RecentAV1.toggleSeen(scope: CoroutineScope, seenDAO: RecordDao) {
    scope.launch(Dispatchers.IO) {
        if (!state.isSeen)
            seenDAO.addChapter(asRecord())
        else
            seenDAO.deleteChapter(aid, number)
        syncData { history() }
    }
}

@Dao
interface RecentAV1Dao {
    @get:Query("SELECT * FROM RecentAV1 ORDER BY `key`")
    val allFlow: Flow<List<RecentAV1>>

    @get:Query("SELECT * FROM RecentAV1 ORDER BY `key`")
    val all: List<RecentAV1>

    @Query("SELECT * FROM RecentAV1 WHERE eid = :eid")
    fun findByEid(eid: Int): RecentAV1?

    @Query("DELETE FROM RecentAV1")
    fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setCache(objects: List<RecentAV1>)
}