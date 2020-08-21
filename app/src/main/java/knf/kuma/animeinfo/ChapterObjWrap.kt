package knf.kuma.animeinfo

import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject

class ChapterObjWrap(val chapter: AnimeObject.WebInfo.AnimeChapter) {
    var isSeen = CacheDB.INSTANCE.seenDAO().chapterIsSeen(chapter.aid,chapter.number)
}