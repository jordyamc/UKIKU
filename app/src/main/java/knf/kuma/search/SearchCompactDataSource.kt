package knf.kuma.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import knf.kuma.App
import knf.kuma.commons.BypassUtil
import knf.kuma.directory.DirObjectCompact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchCompactDataSource(
    val factory: knf.kuma.retrofit.Factory,
    val query: String,
    val onInit: (isEmpty: Boolean) -> Unit
) : PagingSource<Int, DirObjectCompact>() {

    override fun getRefreshKey(state: PagingState<Int, DirObjectCompact>): Int? =
        state.anchorPosition

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DirObjectCompact> {
        val page = params.key ?: 1
        val response = withContext(Dispatchers.IO) {
            factory.getSearch(
                BypassUtil.getStringCookie(App.context),
                BypassUtil.userAgent,
                query,
                page
            ).execute()
        }
        if (response.isSuccessful) {
            response.body()?.let {
                onInit(it.list.isEmpty())
                return LoadResult.Page(it.list, null, if (it.hasNext) page + 1 else null)
            }
        }
        onInit(true)
        return LoadResult.Page(emptyList(), null, null)
    }
}