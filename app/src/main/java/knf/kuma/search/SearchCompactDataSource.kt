package knf.kuma.search

import androidx.paging.PageKeyedDataSource
import knf.kuma.App
import knf.kuma.commons.BypassUtil
import knf.kuma.directory.DirObjectCompact
import knf.kuma.directory.DirectoryPageCompact
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchCompactDataSource(val factory: knf.kuma.retrofit.Factory, val query: String, val onInit: (isEmpty: Boolean) -> Unit) : PageKeyedDataSource<Int, DirObjectCompact>() {
    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, DirObjectCompact>) {
        val call = factory.getSearch(BypassUtil.getStringCookie(App.context), BypassUtil.userAgent, query, 1)
        val clb = object : Callback<DirectoryPageCompact> {
            override fun onFailure(call: Call<DirectoryPageCompact>, t: Throwable) {
                onInit(true)
            }

            override fun onResponse(call: Call<DirectoryPageCompact>, response: Response<DirectoryPageCompact>) {
                response.body()?.let {
                    onInit(it.list.isEmpty())
                    callback.onResult(it.list, null, if (it.hasNext) 2 else null)
                } ?: onInit(true)
            }
        }
        call.enqueue(clb)
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, DirObjectCompact>) {
        val page = params.key
        val call = factory.getSearch(BypassUtil.getStringCookie(App.context), BypassUtil.userAgent, query, page)
        val clb = object : Callback<DirectoryPageCompact> {
            override fun onFailure(call: Call<DirectoryPageCompact>, t: Throwable) {

            }

            override fun onResponse(call: Call<DirectoryPageCompact>, response: Response<DirectoryPageCompact>) {
                response.body()?.let {
                    callback.onResult(it.list, if (it.hasNext) page + 1 else null)
                }
            }
        }
        call.enqueue(clb)
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, DirObjectCompact>) {
    }
}