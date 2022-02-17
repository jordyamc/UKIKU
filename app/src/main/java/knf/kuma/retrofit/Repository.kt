package knf.kuma.retrofit

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import knf.kuma.App
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.directory.DirObject
import knf.kuma.directory.DirObjectCompact
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.RecentObject
import knf.kuma.pojos.Recents
import knf.kuma.recents.RecentsPage
import knf.kuma.search.SearchCompactDataSource
import knf.kuma.search.SearchObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import pl.droidsonroids.retrofit2.JspoonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Singleton

@Singleton
class Repository {

    val search: Flow<PagingData<SearchObject>>
        get() = getSearch("")

    fun reloadAllRecents() {
        reloadRecents()
        reloadRecentModels()
    }

    fun reloadRecents() {
        if (Network.isConnected) {
            var tryCount = 0
            val callback = object : Callback<Recents> {
                override fun onResponse(call: Call<Recents>, response: Response<Recents>) {
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            if (response.isSuccessful) {
                                val objects = RecentObject.create(response.body()?.list ?: listOf())
                                for ((i, recentObject) in objects.withIndex()) {
                                    recentObject.key = i
                                    recentObject.fileWrapper()
                                }
                                CacheDB.INSTANCE.recentsDAO().setCache(objects)
                            } else
                                onFailure(call, Exception("HTTP " + response.code()))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onFailure(call, e)
                        }
                    }
                }

                override fun onFailure(call: Call<Recents>, t: Throwable) {
                    t.printStackTrace()
                    if (tryCount < 3 && PrefsUtil.alwaysGenerateUA) {
                        tryCount++
                        getFactoryBack("https://animeflv.net/").getRecents(
                            BypassUtil.getStringCookie(
                                App.context
                            ), BypassUtil.userAgent, "https://animeflv.net"
                        ).enqueue(this)
                    }
                }
            }
            getFactoryBack("https://animeflv.net/").getRecents(
                BypassUtil.getStringCookie(App.context),
                BypassUtil.userAgent,
                "https://animeflv.net"
            ).enqueue(callback)
        }
    }

    fun reloadRecentModels() {
        if (Network.isConnected) {
            var tryCount = 0
            val callback = object : Callback<RecentsPage> {
                override fun onResponse(call: Call<RecentsPage>, response: Response<RecentsPage>) {
                    try {
                        if (response.isSuccessful) {
                            val list = response.body()?.list?.apply {
                                forEachIndexed { index, model ->
                                    model.key = index
                                }
                            } ?: emptyList()
                            CacheDB.INSTANCE.recentModelsDAO().setCache(list)
                        } else
                            onFailure(call, Exception("HTTP " + response.code()))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onFailure(call, e)
                    }

                }

                override fun onFailure(call: Call<RecentsPage>, t: Throwable) {
                    t.printStackTrace()
                    if (tryCount < 3 && PrefsUtil.alwaysGenerateUA) {
                        tryCount++
                        getFactoryBack("https://animeflv.net/").getRecentModels(
                            BypassUtil.getStringCookie(
                                App.context
                            ), BypassUtil.userAgent, "https://animeflv.net"
                        ).enqueue(this)
                    }
                }
            }
            getFactoryBack("https://animeflv.net/").getRecentModels(
                BypassUtil.getStringCookie(App.context),
                BypassUtil.userAgent,
                "https://animeflv.net"
            ).enqueue(callback)
        }
    }

    fun getAnime(
        context: Context,
        link: String,
        persist: Boolean,
        data: MutableLiveData<AnimeObject?> = MutableLiveData<AnimeObject?>()
    ): LiveData<AnimeObject?> {
        doAsync {
            var cacheUsed = false
            try {
                val base = link.substring(0, link.lastIndexOf("/") + 1)
                val rest = link.substring(link.lastIndexOf("/") + 1)
                val dao = CacheDB.INSTANCE.animeDAO()
                val dbLink = "%/${link.substringAfterLast("/")}"
                dao.getAnimeRaw(dbLink)?.let {
                    cacheUsed = true
                    doOnUI {
                        data.value = it
                    }
                }
                if (Network.isConnected) {
                    var tryCount = 0
                    val callback = object : Callback<AnimeObject.WebInfo> {
                        override fun onResponse(
                            call: Call<AnimeObject.WebInfo>,
                            response: Response<AnimeObject.WebInfo>
                        ) {
                            try {
                                if (response.body() == null || response.code() != 200) {
                                    onFailure(call, Exception("HTTP " + response.code()))
                                } else
                                    doAsync {
                                        val animeObject = AnimeObject(link, response.body())
                                        if (persist)
                                            dao.insert(animeObject)
                                        doOnUI { data.value = animeObject }
                                    }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                onFailure(call, e)
                            }
                        }

                        override fun onFailure(call: Call<AnimeObject.WebInfo>, t: Throwable) {
                            t.printStackTrace()
                            if (tryCount < 3 && PrefsUtil.alwaysGenerateUA) {
                                tryCount++
                                getFactory(base).getAnime(
                                    BypassUtil.getStringCookie(context),
                                    BypassUtil.userAgent,
                                    "https://animeflv.net",
                                    rest
                                ).enqueue(this)
                            } else
                                if (!cacheUsed) data.value = null
                        }
                    }
                    getFactory(base).getAnime(
                        BypassUtil.getStringCookie(context),
                        BypassUtil.userAgent,
                        "https://animeflv.net",
                        rest
                    ).enqueue(callback)
                } else if (!cacheUsed)
                    doOnUI { data.value = null }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!cacheUsed)
                    doOnUI { data.value = null }
            }
        }
        return data
    }

    fun getAnimeDir(): Flow<PagingData<DirObject>> {
        return Pager(
            PagingConfig(25), 0,
            when (PrefsUtil.dirOrder) {
                1 -> CacheDB.INSTANCE.animeDAO().animeDirVotes
                2 -> CacheDB.INSTANCE.animeDAO().animeDirID
                3 -> CacheDB.INSTANCE.animeDAO().animeDirAdded
                4 -> CacheDB.INSTANCE.animeDAO().animeDirFollowers
                else -> CacheDB.INSTANCE.animeDAO().animeDir
            }.asPagingSourceFactory(Dispatchers.IO)
        ).flow
    }

    fun getOvaDir(): Flow<PagingData<DirObject>> {
        return Pager(
            PagingConfig(25), 0,
            when (PrefsUtil.dirOrder) {
                1 -> CacheDB.INSTANCE.animeDAO().ovaDirVotes
                2 -> CacheDB.INSTANCE.animeDAO().ovaDirID
                3 -> CacheDB.INSTANCE.animeDAO().ovaDirAdded
                4 -> CacheDB.INSTANCE.animeDAO().ovaDirFollowers
                else -> CacheDB.INSTANCE.animeDAO().ovaDir
            }.asPagingSourceFactory(Dispatchers.IO)
        ).flow
    }

    fun getMovieDir(): Flow<PagingData<DirObject>> {
        return Pager(
            PagingConfig(25), 0,
            when (PrefsUtil.dirOrder) {
                1 -> CacheDB.INSTANCE.animeDAO().movieDirVotes
                2 -> CacheDB.INSTANCE.animeDAO().movieDirID
                3 -> CacheDB.INSTANCE.animeDAO().movieDirAdded
                4 -> CacheDB.INSTANCE.animeDAO().movieDirFollowers
                else -> CacheDB.INSTANCE.animeDAO().movieDir
            }.asPagingSourceFactory(Dispatchers.IO)
        ).flow
    }

    fun getSearch(query: String): Flow<PagingData<SearchObject>> {
        return Pager(
            PagingConfig(25), 0,
            when {
                query == "" -> CacheDB.INSTANCE.animeDAO().allSearch
                query.trim().matches("^#\\d+$".toRegex()) -> CacheDB.INSTANCE.animeDAO()
                    .getSearchID(query.replace("#", ""))
                PatternUtil.isCustomSearch(query) -> getFiltered(query, null)
                else -> CacheDB.INSTANCE.animeDAO().getSearch("%$query%")
            }.asPagingSourceFactory(Dispatchers.IO)
        ).flow
    }

    private fun getFiltered(query: String, genres: String?): DataSource.Factory<Int, SearchObject> {
        var tQuery = PatternUtil.getCustomSearch(query).trim { it <= ' ' }
        var fQuery = tQuery
        fQuery = if (fQuery != "") "%$fQuery%" else "%"
        when (PatternUtil.getCustomAttr(query).lowercase(Locale.getDefault())) {
            "emision" -> return if (genres == null)
                CacheDB.INSTANCE.animeDAO().getSearchS(fQuery, "En emisión")
            else
                CacheDB.INSTANCE.animeDAO().getSearchSG(fQuery, "En emisión", genres)
            "finalizado" -> return if (genres == null)
                CacheDB.INSTANCE.animeDAO().getSearchS(fQuery, "Finalizado")
            else
                CacheDB.INSTANCE.animeDAO().getSearchSG(fQuery, "Finalizado", genres)
            "anime" -> return if (genres == null)
                CacheDB.INSTANCE.animeDAO().getSearchTY(fQuery, "Anime")
            else
                CacheDB.INSTANCE.animeDAO().getSearchTYG(fQuery, "Anime", genres)
            "ova" -> return if (genres == null)
                CacheDB.INSTANCE.animeDAO().getSearchTY(fQuery, "OVA")
            else
                CacheDB.INSTANCE.animeDAO().getSearchTYG(fQuery, "OVA", genres)
            "pelicula" -> return if (genres == null)
                CacheDB.INSTANCE.animeDAO().getSearchTY(fQuery, "Película")
            else
                CacheDB.INSTANCE.animeDAO().getSearchTYG(fQuery, "Película", genres)
            "personalizado" -> {
                if (tQuery == "")
                    tQuery = "%"
                return if (genres == null)
                    CacheDB.INSTANCE.animeDAO().getSearch(tQuery)
                else
                    CacheDB.INSTANCE.animeDAO().getSearchTG(tQuery, genres)
            }
            else -> return if (genres == null)
                CacheDB.INSTANCE.animeDAO().getSearch(fQuery)
            else
                CacheDB.INSTANCE.animeDAO().getSearchTG(fQuery, genres)
        }
    }

    fun getSearch(query: String, genres: String): Flow<PagingData<SearchObject>> {
        return Pager(
            PagingConfig(25), 0,
            when {
                query == "" -> CacheDB.INSTANCE.animeDAO().getSearchG(genres)
                PatternUtil.isCustomSearch(query) -> getFiltered(query, genres)
                else -> CacheDB.INSTANCE.animeDAO().getSearchTG("%$query%", genres)
            }.asPagingSourceFactory(Dispatchers.IO)
        ).flow
    }

    fun getSearchCompact(
        query: String,
        onInit: (isEmpty: Boolean) -> Unit
    ): Flow<PagingData<DirObjectCompact>> {
        return Pager(
            config = PagingConfig(24),
            pagingSourceFactory = {
                SearchCompactDataSource(
                    getFactory("https://animeflv.net"),
                    query,
                    onInit
                )
            }
        ).flow
    }

    companion object {
        fun getFactory(link: String): Factory {
            val retrofit = Retrofit.Builder()
                .baseUrl(link)
                .client(NoSSLOkHttpClient.get())
                .addConverterFactory(JspoonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
            return retrofit.create(Factory::class.java)
        }
    }

    private fun getFactoryBack(link: String): Factory {
        val retrofit = Retrofit.Builder()
            .baseUrl(link)
            .client(NoSSLOkHttpClient.get())
            .addConverterFactory(JspoonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .callbackExecutor(Executors.newSingleThreadExecutor())
            .build()
        return retrofit.create(Factory::class.java)
    }
}
