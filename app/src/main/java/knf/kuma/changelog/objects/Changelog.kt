package knf.kuma.changelog.objects

import org.jsoup.nodes.Document
import java.util.*

class Changelog(document: Document) {
    var releases: MutableList<Release>

    init {
        val list = ArrayList<Release>()
        for (element in document.select("release")) {
            list.add(Release(element))
        }
        this.releases = list
    }
}
