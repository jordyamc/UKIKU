package knf.kuma.emision

import knf.kuma.search.SearchObject

class AnimeSubObject : SearchObject() {
    var fileName = ""

    fun getFinalName(): String {
        return fileName
    }
}