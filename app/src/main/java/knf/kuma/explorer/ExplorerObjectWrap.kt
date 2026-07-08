package knf.kuma.explorer

import knf.kuma.database.CacheDB
import knf.kuma.pojos.ExplorerObject

class ExplorerObjectWrap(val obj: ExplorerObject){
    val fileList = obj.chapters.map { FileDownWrap(it) }.toMutableList()
}

class FileDownWrap(val obj: ExplorerObject.FileDownObj) {
    var isSeen = CacheDB.INSTANCE.recordAV1DAO().chapterIsSeen(obj.aid.toInt(), obj.chapter.toDouble())
}