package knf.kuma.backup.objects

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import knf.kuma.pojos.AnimeObject

class AnimeChapters {
    var aid = "0"
    var chapters = "[]"

    fun chaptersList(): List<AnimeObject.WebInfo.AnimeChapter> {
        val type = object : TypeToken<List<AnimeObject.WebInfo.AnimeChapter>>() {

        }.type
        return Gson().fromJson<List<AnimeObject.WebInfo.AnimeChapter>>(chapters, type) ?: listOf()
    }
}