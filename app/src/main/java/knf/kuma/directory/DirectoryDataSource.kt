package knf.kuma.directory

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import knf.kuma.App
import knf.kuma.commons.BypassUtil
import knf.kuma.retrofit.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DirectoryDataSource(val factory: knf.kuma.retrofit.Factory, val type: String, val retryCallback: () -> Unit) : PagingSource<Int, DirObjectCompact>() {

    override fun getRefreshKey(state: PagingState<Int, DirObjectCompact>): Int? = state.anchorPosition

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DirObjectCompact> {
        val page = params.key?: 1
        try {
            val response = withContext(Dispatchers.IO) { factory.getDirectory(BypassUtil.getStringCookie(App.context), BypassUtil.userAgent, type, page).execute() }
            if (response.isSuccessful){
                response.body()?.let {
                    return LoadResult.Page(it.list, null, if (it.hasNext) page + 1 else null)
                }
            }
            retryCallback()
            return LoadResult.Error(IllegalStateException(withContext(Dispatchers.IO) { response.errorBody()?.string() }))
        }catch (e:Exception){
            return LoadResult.Error(e)
        }
    }
}

fun createDirectoryPagedList(type: String, retryCallback: () -> Unit) =
        Pager(
            config = PagingConfig(24),
            pagingSourceFactory = { DirectoryDataSource(Repository.getFactory("https://www3.animeflv.net"), type, retryCallback) }
        ).flow