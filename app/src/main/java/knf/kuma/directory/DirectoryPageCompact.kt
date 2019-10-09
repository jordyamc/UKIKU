package knf.kuma.directory

import androidx.annotation.Keep
import org.jsoup.nodes.Element
import pl.droidsonroids.jspoon.ElementConverter
import pl.droidsonroids.jspoon.annotation.Selector

class DirectoryPageCompact {
    @Selector("article.Anime")
    var list: List<DirObjectCompact> = emptyList()
    @Selector(value = "ul.pagination", converter = NextConverter::class)
    var hasNext: Boolean = false

    class NextConverter @Keep constructor() : ElementConverter<Boolean> {
        override fun convert(node: Element, selector: Selector): Boolean {
            val last = node.select("li").last()
            val child = last.child(0)
            return !last.hasClass("disabled") && child.hasAttr("rel") && child.attr("href") != "#"
        }
    }
}