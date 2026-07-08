package knf.kuma.recents

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.droidsonroids.jspoon.ElementConverter
import pl.droidsonroids.jspoon.annotation.Selector

@Entity
@Keep
open class RecentModel {

    @JvmField
    @PrimaryKey
    var key: Int = -1

    @JvmField
    @Selector(value = "img[src]", attr = "src", format = "/(\\d+)\\.\\w+")
    var aid: String = "0"

    @JvmField
    @Selector(value = ".Title")
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

    fun generate(element: Element) {
        aid = element.select("img[src]").attr("src").let {
            Regex("/(\\d+)\\.\\w+").find(it)?.groupValues?.get(1) ?: "0"
        }
        name = element.select(".Title").text()
        chapter = element.select(".Capi").text()
        chapterUrl = element.select("a").attr("href")
        img = element.select("img[src]").attr("src")
    }

    override fun equals(other: Any?): Boolean = other is RecentModel && other.chapter == chapter && other.name == name && other.aid == aid && other.key == key
    override fun hashCode(): Int = name.hashCode() + chapter.hashCode()
}

@Keep
class RecentsPage {
    @Selector("ul.ListEpisodios li:not(article), ul.List-Episodes li:not(article)")
    var list: List<RecentModel> = emptyList()

    fun create(html: String): RecentsPage {
        val doc = Jsoup.parse(html, "https://www3.animeflv.net")
        val episodes = doc.select("ul.ListEpisodios li:not(article), ul.List-Episodes li:not(article)")
        list = episodes.map {
            RecentModel().apply {
                generate(it)
            }
        }
        return this
    }
}

class AFixer : ElementConverter<String> {
    override fun convert(node: Element, selector: Selector): String {
        return "https://www3.animeflv.net${node.attr("href")}"
    }
}

class ImageFixer : ElementConverter<String> {
    override fun convert(node: Element, selector: Selector): String {
        return "https://www3.animeflv.net${node.attr("src")}"
    }
}