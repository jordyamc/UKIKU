package knf.kuma.emision

import knf.kuma.commons.PrefsUtil
import knf.kuma.search.SearchObject

class AnimeSubObject : SearchObject() {
    var fileName = ""

    fun getFinalName(): String {
        return if (PrefsUtil.saveWithName)
            fileName
        else
            aid
    }
}