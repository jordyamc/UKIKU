package knf.kuma.recommended

import android.util.Log
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.JsExtractor
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.DirectoryAV1Min
import knf.kuma.pojos.av1.Genre
import knf.kuma.pojos.av1.GenreRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by jordy on 26/03/2018.
 */

object RecommendHelper {
    fun registerAll(list: List<Genre>, type: RankType) {
        GlobalScope.launch {
            for (genre in list)
                register(genre, type)
            syncData { genres() }
        }
    }

    private suspend fun register(genre: Genre, type: RankType) {
        val status = CacheDB.INSTANCE.genreRecordDAO().findBySlug(genre.slug)?: genre.asRecord()
        if (!status.isBlocked) {
            when (type) {
                RankType.FAV -> status.add(3)
                RankType.UNFAV -> status.sub(3)
                RankType.FOLLOW -> status.add(2)
                RankType.UNFOLLOW -> status.sub(2)
                RankType.CHECK -> status.add(1)
                RankType.SEARCH -> status.add(1)
            }
            CacheDB.INSTANCE.genreRecordDAO().insert(status)
        }
    }

    suspend fun block(list: List<GenreRecord>) {
        CacheDB.INSTANCE.genreRecordDAO().insertAll(list)
        syncData { genres() }
    }

    fun reset(slug: String) {
        GlobalScope.launch {
            val status = CacheDB.INSTANCE.genreRecordDAO().findBySlug(slug) ?: return@launch
            status.count = 0
            CacheDB.INSTANCE.genreRecordDAO().insert(status)
            syncData { genres() }
        }
    }

    suspend fun createRecommended(max: Int = 10): List<DirectoryAV1Min> {
        return withContext(Dispatchers.IO) {
            try {
                val status = CacheDB.INSTANCE.genreRecordDAO().top
                if (status.size <= 2) {
                    return@withContext emptyList()
                }
                Log.e("Recommended","Create recommended of: ${status.joinToString { it.name }}")
                val genreQuery = status.joinToString("") { "&genre=${it.slug}" }
                val baseUrl = "https://animeav1.com/catalogo?order=popular$genreQuery&page="
                var currentPage = 1
                val excludeList = LinkedHashSet<Int>().apply {
                    addAll(CacheDB.INSTANCE.favoriteAV1DAO().allAids)
                    addAll(CacheDB.INSTANCE.organizerDAO().allAids)
                }.toList()
                val list = mutableListOf<DirectoryAV1Min>()
                while (true) {
                    val response = JsExtractor.processLink(baseUrl + currentPage)
                    if (response == null || response.length() == 0) {
                        return@withContext list
                    }
                    for (i in 0 until response.length()) {
                        val item = DirectoryAV1Min.fromJson(response.getJSONObject(i))
                        if (item.aid !in excludeList) {
                            list.add(item)
                        }
                    }
                    if (list.size >= max || currentPage >= 5) {
                        return@withContext list
                    }
                    currentPage++
                }
                emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
