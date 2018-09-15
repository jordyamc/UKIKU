package knf.kuma.changelog.objects

import org.jsoup.nodes.Element
import java.util.*

class Release(element: Element) {
    var version: String = element.attr("version")
    var code: String = element.attr("code")
    var changes: MutableList<Change>

    init {
        val list = ArrayList<Change>()
        for (e in element.select("change")) {
            list.add(Change(e))
        }
        this.changes = list
    }
}
