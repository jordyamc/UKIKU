package knf.kuma.recents

import androidx.recyclerview.widget.DiffUtil
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.FileWrapper
import knf.kuma.commons.PatternUtil.getAnimeUrl
import knf.kuma.commons.PatternUtil.getFileName
import knf.kuma.commons.PrefsUtil.saveWithName
import knf.kuma.commons.distinct
import knf.kuma.database.CacheDB
import knf.kuma.database.CacheDBWrap
import knf.kuma.database.dao.SeenDAO
import knf.kuma.pojos.DownloadObject
import knf.kuma.pojos.SeenObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.nodes.Element
import pl.droidsonroids.jspoon.ElementConverter
import pl.droidsonroids.jspoon.annotation.Selector

@Entity
class RecentModel {
    @PrimaryKey
    var key: Int = -1

    @Selector(value = "img[src]", attr = "src", format = "/(\\d+)\\.\\w+")
    lateinit var aid: String

    @Selector(value = "img", attr = "alt")
    lateinit var name: String

    @Selector(".Capi")
    lateinit var chapter: String

    @Selector(value = "a", converter = AFixer::class)
    lateinit var chapterUrl: String

    @Selector(value = "img[src]", converter = ImageFixer::class)
    lateinit var img: String

    @Ignore
    lateinit var extras: RecentExtras

    @Ignore
    lateinit var state: RecentState

    fun prepare() {
        extras = RecentExtras(this)
        state = RecentState(this)
    }

    override fun equals(other: Any?): Boolean = other is RecentModel && other.chapter == chapter && other.name == name && other.aid == aid && other.key == key
    override fun hashCode(): Int = name.hashCode() + chapter.hashCode()

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<RecentModel>() {
            override fun areItemsTheSame(oldItem: RecentModel, newItem: RecentModel): Boolean =
                    oldItem.extras.eid == newItem.extras.eid

            override fun areContentsTheSame(oldItem: RecentModel, newItem: RecentModel): Boolean =
                    oldItem == newItem
        }
    }
}

class RecentExtras(model: RecentModel) {
    val eid: String by lazy { "${model.aid}${model.chapter}".hashCode().toString() }
    val isNewChapter: Boolean by lazy { model.chapter.matches("^.* [10]$".toRegex()) }
    val animeUrl: String by lazy { getAnimeUrl(model.chapterUrl, model.aid) }
    val filePath: String by lazy {
        if (saveWithName) "$" + getFileName(model.chapterUrl) else "$" + model.aid + "-" + model.chapter.substring(model.chapter.lastIndexOf(" ") + 1) + ".mp4"
    }
    val fileName: String by lazy { eid + filePath }
    val chapterTitle: String by lazy { model.name + model.chapter.substring(model.chapter.lastIndexOf(" ")) }
    val fileWrapper: FileWrapper<*> by lazy { FileWrapper.create(filePath) }
}

class RecentState(val model: RecentModel) {
    var isFavorite = CacheDB.INSTANCE.favsDAO().isFav(model.aid.toInt())
    val favoriteLive = CacheDB.INSTANCE.favsDAO().isFavLive(model.aid.toInt()).distinct
    var isSeen = CacheDB.INSTANCE.seenDAO().chapterIsSeen(model.aid, model.chapter)
    val seenLive = CacheDB.INSTANCE.seenDAO().chapterIsSeenLive(model.aid, model.chapter).distinct
    var downloadObject: DownloadObject? = CacheDBWrap.INSTANCE.downloadsDAO().getByEid(model.extras.eid)
    val downloadLive = CacheDB.INSTANCE.downloadsDAO().getLiveByEid(model.extras.eid)
    val isDownloaded: Boolean get() = model.extras.fileWrapper.exist
}

class RecentsPage {
    @Selector("ul.ListEpisodios li:not(article), ul.List-Episodes li:not(article)")
    lateinit var list: List<RecentModel>
}

class AFixer : ElementConverter<String> {
    override fun convert(node: Element, selector: Selector): String {
        return "https://animeflv.net${node.attr("href")}"
    }
}

class ImageFixer : ElementConverter<String> {
    override fun convert(node: Element, selector: Selector): String {
        return "https://animeflv.net${node.attr("src")}"
    }
}

fun RecentModel.toggleSeen(scope: CoroutineScope, seenDAO: SeenDAO) {
    scope.launch(Dispatchers.IO) {
        if (!state.isSeen)
            seenDAO.addChapter(SeenObject.fromRecentModel(this@toggleSeen))
        else
            seenDAO.deleteChapter(aid, chapter)
        syncData { seen() }
    }
}