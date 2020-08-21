package knf.kuma.pojos

import knf.kuma.database.CacheDB

class RecentWrap(val obj: RecentObject) {
    var isSeen = CacheDB.INSTANCE.seenDAO().chapterIsSeen(obj.aid,obj.chapter)
    var isFav = CacheDB.INSTANCE.favsDAO().isFav(obj.aid.toInt())
}

fun RecentObject.wrap(): RecentWrap = RecentWrap(this)