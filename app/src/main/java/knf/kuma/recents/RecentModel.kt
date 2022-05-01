package knf.kuma.recents

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.text.isDigitsOnly
import androidx.recyclerview.widget.DiffUtil
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import knf.kuma.R
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.*
import knf.kuma.commons.PatternUtil.getAnimeUrl
import knf.kuma.commons.PatternUtil.getFileName
import knf.kuma.commons.PrefsUtil.saveWithName
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
import kotlin.math.abs

@Entity
open class RecentModel {

    @JvmField
    @PrimaryKey
    var key: Int = -1

    @JvmField
    @Selector(value = "img[src]", attr = "src", format = "/(\\d+)\\.\\w+")
    var aid: String = "0"

    @JvmField
    @Selector(value = "img", attr = "alt")
    var name: String = ""

    @JvmField
    @Selector(".Capi")
    var chapter: String = ""

    @JvmField
    @Selector(value = "a", converter = AFixer::class)
    var chapterUrl: String = ""

    @JvmField
    @Selector(value = "img[src]", converter = ImageFixer::class)
    var img: String = ""

    @Ignore
    lateinit var extras: RecentExtras

    @Ignore
    lateinit var state: RecentState

    fun prepare() {
        if (!aid.isDigitsOnly())
            aid = "0"
        if (!::extras.isInitialized)
            extras = RecentExtras(this)
        if (!::state.isInitialized)
            state = RecentState(this)
    }

    override fun equals(other: Any?): Boolean = other is RecentModel && other.chapter == chapter && other.name == name && other.aid == aid && other.key == key
    override fun hashCode(): Int = name.hashCode() + chapter.hashCode()

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<RecentModel>() {
            override fun areItemsTheSame(oldItem: RecentModel, newItem: RecentModel): Boolean =
                    oldItem::class == newItem::class &&
                            if (oldItem is RecentModelAd)
                                oldItem.id == (newItem as RecentModelAd).id
                            else
                                oldItem.extras.eid == newItem.extras.eid

            override fun areContentsTheSame(oldItem: RecentModel, newItem: RecentModel): Boolean =
                    if (oldItem !is RecentModelAd) oldItem == newItem else true
        }
    }
}

class RecentExtras(model: RecentModel) {
    val eid: String by lazy { abs("${model.aid}${model.chapter}".hashCode()).toString() }
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
    var isDeleting = false
    val downloadLive = CacheDB.INSTANCE.downloadsDAO().getLiveByEid(model.extras.eid)
    var isDownloaded: Boolean
        get() = model.extras.fileWrapper.exist
        set(value) {
            model.extras.fileWrapper.exist = value
        }
    val checkIsDownloaded: Boolean get() = model.extras.fileWrapper.existForced()
    val canPlay: Boolean get() = downloadObject?.isDownloadingOrPaused == false && checkIsDownloaded
}

class RecentsPage {
    @Selector("ul.ListEpisodios li:not(article), ul.List-Episodes li:not(article)")
    var list: List<RecentModel> = emptyList()
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

val RecentModel.menuHideList: List<Int>
    get() = mutableListOf<Int>().apply {
        if (PrefsUtil.recentActionType == "0")
            add(R.id.streaming)
        if (PrefsUtil.recentActionType == "1" || state.downloadObject?.isDownloadingOrPaused == true || state.canPlay)
            add(R.id.download)
        if (state.isDeleting || !state.canPlay)
            add(R.id.delete)
    }

fun RecentModel.openInfo(context: Context) {
    context.startActivity(Intent(context, DesignUtils.infoClass).apply {
        data = Uri.parse(this@openInfo.extras.animeUrl)
        putExtra("title", name)
        putExtra("img", PatternUtil.getCover(aid))
    })
}