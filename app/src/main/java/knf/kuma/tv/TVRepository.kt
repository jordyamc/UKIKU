package knf.kuma.tv

import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import knf.kuma.commons.JsExtractor
import knf.kuma.commons.Network
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.DirectoryAV1
import knf.kuma.pojos.av1.DirectoryAV1Calendar
import knf.kuma.pojos.av1.DirectoryAV1Min
import knf.kuma.pojos.av1.SearchDataSource
import knf.kuma.pojos.av1.SearchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

object TVRepository {
    const val TYPE_BEST = "&status=emision"
    const val TYPE_BEST_GLOBAL = "&status=finalizado"

    suspend fun searchDir(type: String): List<DirectoryAV1Min> {
        val response = JsExtractor.processLink("https://animeav1.com/catalogo?order=popular$type") ?: return emptyList()
        val list = mutableListOf<DirectoryAV1Min>()
        for (i in 0 until response.length()) {
            list.add(DirectoryAV1Min.fromJson(response.getJSONObject(i)))
        }
        return list
    }

    suspend fun getAnime(url: String): DirectoryAV1? {
        return withContext(Dispatchers.IO) {
            val cached = CacheDB.INSTANCE.directoryDAO().findBySlug(url.removeSurrounding("/").substringAfterLast("/"))
            if (Network.isConnected) {
                val response = JsExtractor.processLink(url) ?: return@withContext cached
                DirectoryAV1.fromJson(response.getJSONObject(0)).also {
                    CacheDB.INSTANCE.directoryDAO().add(it)
                }
            } else {
                cached
            }
        }
    }

    fun searchQuery(query: String, slug : String? = null): Flow<PagingData<DirectoryAV1Min>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                SearchDataSource(SearchState(query, slug?.let { listOf(it) }?:emptyList()))
            }
        ).flow
    }

    suspend fun calendarMap(): Map<Int, List<DirectoryAV1Calendar>> {
        val result = JsExtractor.processLink("https://animeav1.com/horario") ?: return emptyMap()
        val list = mutableListOf<DirectoryAV1Calendar>()
        for (i in 0 until result.length()) {
            list.add(DirectoryAV1Calendar.fromJson(result.getJSONObject(i)))
        }
        val grouped = list.sortedBy { it.name }.groupBy { it.day }
        return (1..7).associateWith { grouped[it] ?: emptyList() }
    }
}