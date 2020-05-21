package knf.kuma.animeinfo.ktx

import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PrefsUtil
import knf.kuma.pojos.AnimeObject

val AnimeObject.WebInfo.AnimeChapter.epTitle: String get() = name + number.substring(number.lastIndexOf(" "))

val AnimeObject.WebInfo.AnimeChapter.fileName: String
    get() = if (PrefsUtil.saveWithName)
        eid + "$" + PatternUtil.getFileName(link)
    else
        eid + "$" + aid + "-" + number.substring(number.lastIndexOf(" ") + 1) + ".mp4"

val AnimeObject.WebInfo.AnimeChapter.filePath: String
    get() = if (PrefsUtil.saveWithName)
        "$" + PatternUtil.getFileName(link)
    else
        "$" + aid + "-" + number.substring(number.lastIndexOf(" ") + 1) + ".mp4"