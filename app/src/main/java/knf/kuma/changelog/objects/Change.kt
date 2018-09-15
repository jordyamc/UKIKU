package knf.kuma.changelog.objects

import org.jsoup.nodes.Element

class Change(element: Element) {
    var type: String = element.attr("type")
    var text: String = element.text()
}
