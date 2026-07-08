package knf.kuma.home

import com.google.gson.Gson
import knf.kuma.App
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirObject
import knf.kuma.pojos.av1.DirectoryAV1Min
import knf.kuma.pojos.av1.Recommended
import knf.kuma.search.SearchAdvObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import java.io.File
import java.net.URL

object StaffRecommendations {
    suspend fun createList(count: Int = 10): List<Recommended> {
        val cached = File(App.context.filesDir, "suggestions.json")
        try {
            val json = withContext(Dispatchers.IO) {
                if (cached.exists()) {
                    cached.readText()
                } else {
                    URL("https://cdn.statically.io/gh/jordyamc/UKIKU@master/static_data/suggestions.json").readText().also {
                        cached.writeText(it)
                    }
                }
            }
            val suggestions = Gson().fromJson(json, Array<Recommended>::class.java)
            return suggestions.toList().shuffled().take(count)
        } catch (e: Exception) {
            cached.delete()
            e.printStackTrace()
            return emptyList()
        }
    }
}