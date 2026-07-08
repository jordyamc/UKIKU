package knf.kuma.animeinfo.ktx

import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PrefsUtil
import knf.kuma.pojos.AnimeObject

val AnimeObject.WebInfo.AnimeChapter.fileName: String
    get() = if (PrefsUtil.saveWithName)
        eid + "$" + PatternUtil.getFileName(link)
    else
        eid + "$" + aid + "-" + number.trim().substringAfterLast(" ") + ".mp4"

val AnimeObject.WebInfo.AnimeChapter.filePath: String
    get() = if (PrefsUtil.saveWithName)
        eid +"$" + PatternUtil.getFileName(link)
    else
        eid + "$" + aid + "-" + number.trim().substringAfterLast(" ") + ".mp4"