package knf.kuma.pojos

import android.util.Log
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import knf.kuma.commons.noCrashLetNullable
import knf.kuma.database.CacheDB
import knf.kuma.recents.RecentModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

@Keep
@Entity
data class SeenObject(@PrimaryKey val eid: String = "", val aid: String = "", val number: String = "") {
    companion object {
        fun fromChapter(chapter: AnimeObject.WebInfo.AnimeChapter): SeenObject = SeenObject(chapter.eid, chapter.aid, chapter.number)
        fun fromRecent(recent: RecentObject): SeenObject = SeenObject(recent.eid, recent.aid, recent.chapter)
        fun fromRecentModel(recent: RecentModel): SeenObject = SeenObject(recent.extras.eid, recent.aid, recent.chapter)
        fun fromDownloaded(download: ExplorerObject.FileDownObj) = SeenObject(download.eid, download.aid, String.format(Locale.getDefault(), "Episodio %s", download.chapter))
    }
}

fun migrateSeen() {
    GlobalScope.launch(Dispatchers.IO) {
        if (CacheDB.INSTANCE.chaptersDAO().count != 0) {
            var total: Int
            CacheDB.INSTANCE.seenDAO().addAll(CacheDB.INSTANCE.chaptersDAO().all.mapNotNull { noCrashLetNullable { SeenObject.fromChapter(it) } }.also { total = it.size })
            CacheDB.INSTANCE.chaptersDAO().clear()
            Log.e("Seen", "Migrated $total")
        }
    }
}