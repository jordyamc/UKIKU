package knf.kuma.directory

import androidx.paging.PageKeyedDataSource
import androidx.paging.PagedList
import knf.kuma.App
import knf.kuma.commons.BypassUtil
import knf.kuma.custom.BackgroundExecutor
import knf.kuma.custom.MainExecutor
import knf.kuma.retrofit.Repository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DirectoryDataSource(val factory: knf.kuma.retrofit.Factory, val type: String, val retryCallback: (() -> Unit) -> Unit) : PageKeyedDataSource<Int, DirObjectCompact>() {

    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, DirObjectCompact>) {
        factory.getDirectory(BypassUtil.getStringCookie(App.context), BypassUtil.userAgent, type, 1).enqueue(object : Callback<DirectoryPageCompact> {
            override fun onFailure(call: Call<DirectoryPageCompact>, t: Throwable) {
                t.printStackTrace()
                retryCallback { loadInitial(params, callback) }
            }

            override fun onResponse(call: Call<DirectoryPageCompact>, response: Response<DirectoryPageCompact>) {
                response.body()?.let {
                    callback.onResult(it.list, null, 2)
                } ?: { retryCallback { loadInitial(params, callback) } }()
            }
        })
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, DirObjectCompact>) {
        factory.getDirectory(BypassUtil.getStringCookie(App.context), BypassUtil.userAgent, type, params.key).enqueue(object : Callback<DirectoryPageCompact> {
            override fun onFailure(call: Call<DirectoryPageCompact>, t: Throwable) {
                t.printStackTrace()
                retryCallback { loadAfter(params, callback) }
            }

            override fun onResponse(call: Call<DirectoryPageCompact>, response: Response<DirectoryPageCompact>) {
                response.body()?.let {
                    callback.onResult(it.list, if (it.hasNext) params.key + 1 else null)
                } ?: { retryCallback { loadAfter(params, callback) } }()
            }
        })
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, DirObjectCompact>) {

    }
}

fun createDirectoryPagedList(type: String, retryCallback: (() -> Unit) -> Unit): PagedList<DirObjectCompact> =
        PagedList.Builder<Int, DirObjectCompact>(DirectoryDataSource(Repository.getFactory("https://animeflv.net"), type, retryCallback), 24).apply {
            setFetchExecutor(BackgroundExecutor())
            setNotifyExecutor(MainExecutor())
        }.build()