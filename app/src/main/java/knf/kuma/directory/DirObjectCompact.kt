package knf.kuma.directory

import androidx.annotation.Keep
import org.jsoup.nodes.Element
import pl.droidsonroids.jspoon.ElementConverter
import pl.droidsonroids.jspoon.annotation.Selector

class DirObjectCompact {
    @Selector(value = ":root", converter = ImageExtractor::class)
    var aid: String = ""
    @Selector("h3.Title")
    var name: String = ""
    @Selector(value = ":root", converter = LinkExtractor::class)
    var link: String? = null

    override fun hashCode(): Int {
        return aid.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is DirObjectCompact && other.aid == aid
    }

    class LinkExtractor @Keep constructor() : ElementConverter<String> {
        override fun convert(node: Element, selector: Selector): String {
            return "https://animeflv.net${node.select("a").attr("href")}"
        }
    }

    class ImageExtractor @Keep constructor() : ElementConverter<String> {
        override fun convert(node: Element, selector: Selector): String {
            val img = node.select("figure img").first().let {
                when {
                    it.hasAttr("data-cfsrc") -> it.attr("data-cfsrc")
                    it.hasAttr("src") -> it.attr("src")
                    else -> "/0.jpg"
                }
            }
            return "/(\\d+)\\.".toRegex().find(img)?.destructured?.component1() ?: "0"
        }
    }
}