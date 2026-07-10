package knf.kuma.animeinfo.ktx

import knf.kuma.commons.PatternUtil
import knf.kuma.pojos.AnimeObject

val AnimeObject.WebInfo.AnimeChapter.fileName: String
    get() = eid + "$" + PatternUtil.getFileName(link)

val AnimeObject.WebInfo.AnimeChapter.filePath: String
    get() = eid +"$" + PatternUtil.getFileName(link)