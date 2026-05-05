package knf.kuma.retrofit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import knf.kuma.commons.Network
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUIGlobal
import knf.kuma.commons.jsoupCookies
import knf.kuma.commons.jsoupCookiesAdapter
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
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import pl.droidsonroids.jspoon.Jspoon
import java.util.Locale
import javax.inject.Singleton

@Singleton
class Repository {

    val search: Flow<PagingData<SearchObject>>
        get() = getSearch("")

    fun reloadAllRecents() {
        reloadRecents()
        reloadRecentsAlt()
    }

    fun reloadRecents() {
        if (Network.isConnected) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val recents = Jspoon.create().adapter(Recents::class.java).fromHtml(jsoupCookies("https://www3.animeflv.net/").get().outerHtml())
                    val objects = RecentObject.create(recents.list)
                    for ((i, recentObject) in objects.withIndex()) {
                        recentObject.key = i
                        recentObject.fileWrapper()
                    }
                    CacheDB.INSTANCE.recentsDAO().setCache(objects)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun reloadRecentsAlt() {
        if (Network.isConnected) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val page = Jspoon.create().adapter(RecentsPage::class.java).fromHtml(jsoupCookies("https://animeflv.net/").get().outerHtml())
                    val list = page.list.apply {
                        forEachIndexed { index, model ->
                            model.key = index
                        }
                    }
                    withContext(Dispatchers.IO) {
                        CacheDB.INSTANCE.recentModelsDAO().setCache(list)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getAnime(
        link: String,
        persist: Boolean,
        data: MutableLiveData<AnimeObject?> = MutableLiveData<AnimeObject?>()
    ): LiveData<AnimeObject?> {
        doAsync {
            var cacheUsed = false
            try {
                val dao = CacheDB.INSTANCE.animeDAO()
                val dbLink = "%/${link.substringAfterLast("/")}"
                dao.getAnimeRaw(dbLink)?.let {
                    cacheUsed = true
                    doOnUIGlobal {
                        data.value = it
                    }
                }
                if (Network.isConnected) {
                    val animeObject = AnimeObject(link, jsoupCookiesAdapter(link, AnimeObject.WebInfo::class.java))
                    if (persist)
                        dao.insert(animeObject)
                    doOnUIGlobal { data.value = animeObject }
                } else if (!cacheUsed)
                    doOnUIGlobal { data.value = null }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!cacheUsed)
                    doOnUIGlobal { data.value = null }
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
                    query,
                    onInit
                )
            }
        ).flow
    }
}
