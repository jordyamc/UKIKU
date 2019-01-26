package knf.kuma.retrofit

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.crashlytics.android.Crashlytics
import knf.kuma.App
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.RecentObject
import knf.kuma.pojos.Recents
import pl.droidsonroids.retrofit2.JspoonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import xdroid.toaster.Toaster
import java.util.concurrent.Executors
import javax.inject.Singleton

@Singleton
class Repository {

    val search: LiveData<PagedList<AnimeObject>>
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
                            val dao = CacheDB.INSTANCE.recentsDAO()
                            dao.setCache(objects)
                        } else
                            onFailure(call, Exception("HTTP " + response.code()))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onFailure(call, e)
                    }

                }

                override fun onFailure(call: Call<Recents>, t: Throwable) {
                    Toaster.toast("Error al obtener - " + t.message)
                    t.printStackTrace()
                    Crashlytics.logException(t)
                }
            })
        }
    }

    fun getAnime(context: Context, link: String, persist: Boolean): LiveData<AnimeObject> {
        val data = MutableLiveData<AnimeObject>()
        try {
            val base = link.substring(0, link.lastIndexOf("/") + 1)
            val rest = link.substring(link.lastIndexOf("/") + 1)
            val dao = CacheDB.INSTANCE.animeDAO()
            if (!Network.isConnected && dao.existLink(link))
                return CacheDB.INSTANCE.animeDAO().getAnime(link)
            getFactory(base).getAnime(BypassUtil.getStringCookie(context), BypassUtil.userAgent, "https://animeflv.net", rest).enqueue(object : Callback<AnimeObject.WebInfo> {
                override fun onResponse(call: Call<AnimeObject.WebInfo>, response: Response<AnimeObject.WebInfo>) {
                    try {
                        if (response.body() == null || response.code() != 200) {
                            data.value = CacheDB.INSTANCE.animeDAO().getAnimeRaw(link)
                            return
                        }
                        val animeObject = AnimeObject(link, response.body())
                        if (persist)
                            dao.insert(animeObject)
                        data.value = animeObject
                    } catch (e: Exception) {
                        data.value = null
                    }

                }

                override fun onFailure(call: Call<AnimeObject.WebInfo>, t: Throwable) {
                    t.printStackTrace()
                    data.value = CacheDB.INSTANCE.animeDAO().getAnimeRaw(link)
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            data.value = null
        }

        return data
    }

    fun getAnimeDir(): LiveData<PagedList<AnimeObject>> {
        return when (PrefsUtil.dirOrder) {
            0 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().animeDir, 25).build()
            1 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().animeDirVotes, 25).build()
            2 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().animeDirID, 25).build()
            3 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().animeDirAdded, 25).build()
            else -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().animeDir, 25).build()
        }
    }

    fun getOvaDir(): LiveData<PagedList<AnimeObject>> {
        return when (PrefsUtil.dirOrder) {
            0 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().ovaDir, 25).build()
            1 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().ovaDirVotes, 25).build()
            2 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().ovaDirID, 25).build()
            3 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().ovaDirAdded, 25).build()
            else -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().ovaDir, 25).build()
        }
    }

    fun getMovieDir(): LiveData<PagedList<AnimeObject>> {
        return when (PrefsUtil.dirOrder) {
            0 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().movieDir, 25).build()
            1 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().movieDirVotes, 25).build()
            2 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().movieDirID, 25).build()
            3 -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().movieDirAdded, 25).build()
            else -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().movieDir, 25).build()
        }
    }

    fun getSearch(query: String): LiveData<PagedList<AnimeObject>> {
        return when {
            query == "" -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().all, 25).setInitialLoadKey(0).build()
            query.trim { it <= ' ' }.matches("^#\\d+$".toRegex()) -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchID(query.replace("#", "")), 25).setInitialLoadKey(0).build()
            PatternUtil.isCustomSearch(query) -> getFiltered(query, null)
            else -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearch("%$query%"), 25).setInitialLoadKey(0).build()
        }
    }

    private fun getFiltered(query: String, genres: String?): LiveData<PagedList<AnimeObject>> {
        var tQuery = PatternUtil.getCustomSearch(query).trim { it <= ' ' }
        var fQuery = tQuery
        fQuery = if (fQuery != "") "%$fQuery%" else "%"
        when (PatternUtil.getCustomAttr(query).toLowerCase()) {
            "emision" -> return if (genres == null)
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchS(fQuery, "En emisión"), 25).setInitialLoadKey(0).build()
            else
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchSG(fQuery, "En emisión", genres), 25).setInitialLoadKey(0).build()
            "finalizado" -> return if (genres == null)
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchS(fQuery, "Finalizado"), 25).setInitialLoadKey(0).build()
            else
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchSG(fQuery, "Finalizado", genres), 25).setInitialLoadKey(0).build()
            "anime" -> return if (genres == null)
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTY(fQuery, "Anime"), 25).setInitialLoadKey(0).build()
            else
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTYG(fQuery, "Anime", genres), 25).setInitialLoadKey(0).build()
            "ova" -> return if (genres == null)
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTY(fQuery, "OVA"), 25).setInitialLoadKey(0).build()
            else
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTYG(fQuery, "OVA", genres), 25).setInitialLoadKey(0).build()
            "pelicula" -> return if (genres == null)
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTY(fQuery, "Película"), 25).setInitialLoadKey(0).build()
            else
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTYG(fQuery, "Película", genres), 25).setInitialLoadKey(0).build()
            "personalizado" -> {
                if (tQuery == "")
                    tQuery = "%"
                return if (genres == null)
                    LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearch(tQuery), 25).setInitialLoadKey(0).build()
                else
                    LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTG(tQuery, genres), 25).setInitialLoadKey(0).build()
            }
            else -> return if (genres == null)
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearch(fQuery), 25).setInitialLoadKey(0).build()
            else
                LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTG(fQuery, genres), 25).setInitialLoadKey(0).build()
        }
    }

    fun getSearch(query: String, genres: String): LiveData<PagedList<AnimeObject>> {
        return when {
            query == "" -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchG(genres), 25).setInitialLoadKey(0).build()
            PatternUtil.isCustomSearch(query) -> getFiltered(query, genres)
            else -> LivePagedListBuilder(CacheDB.INSTANCE.animeDAO().getSearchTG("%$query%", genres), 25).setInitialLoadKey(0).build()
        }
    }

    private fun getFactory(link: String): Factory {
        val retrofit = Retrofit.Builder()
                .baseUrl(link)
                .client(NoSSLOkHttpClient.get())
                .addConverterFactory(JspoonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
        return retrofit.create(Factory::class.java)
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
