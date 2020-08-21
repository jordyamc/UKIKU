package knf.kuma.explorer

import knf.kuma.database.CacheDB
import knf.kuma.pojos.ExplorerObject
import java.util.*

class ExplorerObjectWrap(val obj: ExplorerObject){
    val fileList = obj.chapters.map { FileDownWrap(it) }.toMutableList()
}

class FileDownWrap(val obj: ExplorerObject.FileDownObj) {
    var isSeen = CacheDB.INSTANCE.seenDAO().chapterIsSeen(obj.aid, String.format(Locale.getDefault(), "Episodio %s", obj.chapter))
}