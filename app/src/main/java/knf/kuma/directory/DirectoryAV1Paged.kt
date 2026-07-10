package knf.kuma.directory

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import knf.kuma.commons.JsExtractor
import knf.kuma.pojos.av1.DirectoryAV1Min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DirectoryAV1DataSource(val type: String, val order: Int, val retryCallback: () -> Unit) :
    PagingSource<String, DirectoryAV1Min>() {

    private val pages = listOf("0","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z")

    override fun getRefreshKey(state: PagingState<String, DirectoryAV1Min>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, DirectoryAV1Min> {
        val code = params.key ?: "0:1"
        val letter = code.split(":")[0]
        val page = code.split(":")[1].toInt()
        try {
            val dir = withContext(Dispatchers.IO) {
                JsExtractor.processLink("https://animeav1.com/catalogo?${getOrder()}${if (order == 0) "&letter=$letter" else ""}$type&page=$page")
            }
            if (dir == null) {
                retryCallback()
                return LoadResult.Error(Exception("Error al cargar directorio"))
            }
            if (dir.length() == 0) {
                val currentLetterIndex = pages.indexOf(letter)
                if (currentLetterIndex < pages.size - 1 && order == 0) {
                    return LoadResult.Page(emptyList(), null, pages[currentLetterIndex + 1] + ":1")
                }
                return LoadResult.Page(emptyList(), null, null)
            }
            val items = mutableListOf<DirectoryAV1Min>()
            for (i in 0 until dir.length()) {
                val item = dir.getJSONObject(i)
                items.add(DirectoryAV1Min.fromJson(item))
            }
            if (items.size == 20) {
                return LoadResult.Page(items, null, "$letter:${page + 1}")
            } else {
                val currentLetterIndex = pages.indexOf(letter)
                if (currentLetterIndex < pages.size - 1) {
                    return LoadResult.Page(items, null, pages[currentLetterIndex + 1] + ":1")
                }
                return LoadResult.Page(items, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            retryCallback()
            return LoadResult.Error(e)
        }
    }

    fun getOrder(): String {
        return when(order) {
            1 -> "order=popular"
            2 -> "order=latest_released"
            else -> "order=title"
        }
    }
}

fun createDirectoryAV1PagedList(type: String, order: Int, retryCallback: () -> Unit) =
    Pager(
        config = PagingConfig(20),
        pagingSourceFactory = { DirectoryAV1DataSource(type, order, retryCallback) }
    ).flow