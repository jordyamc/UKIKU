package knf.kuma.retrofit

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.firebase.crashlytics.FirebaseCrashlytics
import knf.kuma.App
import knf.kuma.commons.*
import knf.kuma.custom.BackgroundExecutor
import knf.kuma.custom.MainExecutor
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
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import pl.droidsonroids.retrofit2.JspoonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.util.concurrent.Executors
import javax.inject.Singleton

@Singleton
class Repository {

    val search: LiveData<PagedList<SearchObject>>
        get() = getSearch("")

    fun reloadRecents() {
        if (Network.isConnected) {
            getFactoryBack("https://animeflv.net/").getRecents(BypassUtil.getStringCookie(App.context), BypassUtil.userAgent, "https://animeflv.net").enqueue(object : Callback<Recents> {
                override fun onResponse(call: Call<Recents>, response: Response<Recents>) {
                    try {
                        if (response.isSuccessful) {
                            val objects = RecentObject.create(response.body()?.list ?: listOf())
                            for ((i, recentObject) in objects.withIndex())
                                recentObject.key = i
                            CacheDB.INSTANCE.recentsDAO().setCache(objects)
                        } else
                            onFailure(call, Exception("HTTP " + response.code()))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onFailure(call, e)
                    }

                }

                override fun onFailure(call: Call<Recents>, t: Throwable) {
                    t.printStackTrace()
                    FirebaseCrashlytics.getInstance().recordException(t)
                }
            })
        }
    }

    fun reloadRecentModels() {
        if (Network.isConnected) {
            getFactoryBack("https://animeflv.net/").getRecentModels(BypassUtil.getStringCookie(App.context), BypassUtil.userAgent, "https://animeflv.net").enqueue(object : Callback<RecentsPage> {
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
                    FirebaseCrashlytics.getInstance().recordException(t)
                }
            })
        }
    }

    fun getAnime(context: Context, link: String, persist: Boolean): LiveData<AnimeObject?> {
        return getAnime(context, link, persist, MutableLiveData())
    }

    fun getAnime(context: Context, link: String, persist: Boolean, data: MutableLiveData<AnimeObject?>): LiveData<AnimeObject?> {
        doAsync {
            var cacheUsed = false
            try {
                val base = link.substring(0, link.lastIndexOf("/") + 1)
                val rest = link.substring(link.lastIndexOf("/") + 1)
                val dao = CacheDB.INSTANCE.animeDAO()
                dao.getAnimeRaw(link)?.let {
                    if (it.checkIntegrity()) {
                        cacheUsed = true
                        doOnUI {
                            data.value = it
                        }
                    }
                }
                if (Network.isConnected)
                    getFactory(base).getAnime(BypassUtil.getStringCookie(context), BypassUtil.userAgent, "https://animeflv.net", rest).enqueue(object : Callback<AnimeObject.WebInfo> {
                        override fun onResponse(call: Call<AnimeObject.WebInfo>, response: Response<AnimeObject.WebInfo>) {
                            try {
                                if (response.body() == null || response.code() != 200) {
                                    if (!cacheUsed) data.value = null
                                    return
                                }
                                doAsync {
                                    val animeObject = AnimeObject(link, response.body())
                                    if (persist)
                                        dao.insert(animeObject)
                                    doOnUI { data.value = animeObject }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                if (!cacheUsed) data.value = null
                            }
                        }

                        override fun onFailure(call: Call<AnimeObject.WebInfo>, t: Throwable) {
                            t.printStackTrace()
                            doOnUI { data.value = withContext(Dispatchers.IO) { CacheDB.INSTANCE.animeDAO().getAnimeRaw(link) } }
                        }
                    })
                else if (!cacheUsed)
                    doOnUI { data.value = null }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!cacheUsed)
                    doOnUI { data.value = null }
            }
        }
        return data
    }

    fun getAnimeDir(): LiveData<PagedList<DirObject>> {
        return when (PrefsUtil.dirOrder) {
            1 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().animeDirVotes, pagedConfig(25)).build()
            2 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().animeDirID, pagedConfig(25)).build()
            3 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().animeDirAdded, pagedConfig(25)).build()
            4 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().animeDirFollowers, pagedConfig(25)).build()
            else -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().animeDir, pagedConfig(25)).build()
        }
    }

    fun getOvaDir(): LiveData<PagedList<DirObject>> {
        return when (PrefsUtil.dirOrder) {
            1 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().ovaDirVotes, pagedConfig(25)).build()
            2 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().ovaDirID, pagedConfig(25)).build()
            3 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().ovaDirAdded, pagedConfig(25)).build()
            4 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().ovaDirFollowers, pagedConfig(25)).build()
            else -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().ovaDir, pagedConfig(25)).build()
        }
    }

    fun getMovieDir(): LiveData<PagedList<DirObject>> {
        return when (PrefsUtil.dirOrder) {
            1 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().movieDirVotes, pagedConfig(25)).build()
            2 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().movieDirID, pagedConfig(25)).build()
            3 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().movieDirAdded, pagedConfig(25)).build()
            4 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().movieDirFollowers, pagedConfig(25)).build()
            else -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().movieDir, pagedConfig(25)).build()
        }
    }

    fun getSearch(query: String): LiveData<PagedList<SearchObject>> {
        return when {
            query == "" -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().allSearch, pagedConfig(25)).setInitialLoadKey(0).build()
            query.trim { it <= ' ' }.matches("^#\\d+$".toRegex()) -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchID(query.replace("#", "")), pagedConfig(25)).setInitialLoadKey(0).build()
            PatternUtil.isCustomSearch(query) -> getFiltered(query, null)
            else -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearch("%$query%"), pagedConfig(25)).setInitialLoadKey(0).build()
        }
    }

    private fun getFiltered(query: String, genres: String?): LiveData<PagedList<SearchObject>> {
        var tQuery = PatternUtil.getCustomSearch(query).trim { it <= ' ' }
        var fQuery = tQuery
        fQuery = if (fQuery != "") "%$fQuery%" else "%"
        when (PatternUtil.getCustomAttr(query).toLowerCase()) {
            "emision" -> return if (genres == null)
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchS(fQuery, "En emisión"), pagedConfig(25)).setInitialLoadKey(0).build()
            else
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchSG(fQuery, "En emisión", genres), pagedConfig(25)).setInitialLoadKey(0).build()
            "finalizado" -> return if (genres == null)
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchS(fQuery, "Finalizado"), pagedConfig(25)).setInitialLoadKey(0).build()
            else
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchSG(fQuery, "Finalizado", genres), pagedConfig(25)).setInitialLoadKey(0).build()
            "anime" -> return if (genres == null)
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTY(fQuery, "Anime"), pagedConfig(25)).setInitialLoadKey(0).build()
            else
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTYG(fQuery, "Anime", genres), pagedConfig(25)).setInitialLoadKey(0).build()
            "ova" -> return if (genres == null)
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTY(fQuery, "OVA"), pagedConfig(25)).setInitialLoadKey(0).build()
            else
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTYG(fQuery, "OVA", genres), pagedConfig(25)).setInitialLoadKey(0).build()
            "pelicula" -> return if (genres == null)
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTY(fQuery, "Película"), pagedConfig(25)).setInitialLoadKey(0).build()
            else
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTYG(fQuery, "Película", genres), pagedConfig(25)).setInitialLoadKey(0).build()
            "personalizado" -> {
                if (tQuery == "")
                    tQuery = "%"
                return if (genres == null)
                    LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearch(tQuery), pagedConfig(25)).setInitialLoadKey(0).build()
                else
                    LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTG(tQuery, genres), pagedConfig(25)).setInitialLoadKey(0).build()
            }
            else -> return if (genres == null)
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearch(fQuery), pagedConfig(25)).setInitialLoadKey(0).build()
            else
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTG(fQuery, genres), pagedConfig(25)).setInitialLoadKey(0).build()
        }
    }

    fun getSearch(query: String, genres: String): LiveData<PagedList<SearchObject>> {
        return when {
            query == "" -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchG(genres), pagedConfig(25)).setInitialLoadKey(0).build()
            PatternUtil.isCustomSearch(query) -> getFiltered(query, genres)
            else -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTG("%$query%", genres), pagedConfig(25)).setInitialLoadKey(0).build()
        }
    }

    fun getSearchCompact(query: String, onInit: (isEmpty: Boolean) -> Unit): PagedList<DirObjectCompact> {
        return PagedList.Builder<Int, DirObjectCompact>(SearchCompactDataSource(getFactory("https://animeflv.net"), query, onInit), 24).apply {
            setFetchExecutor(BackgroundExecutor())
            setNotifyExecutor(MainExecutor())
        }.build()
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
