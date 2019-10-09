package knf.kuma.recommended

import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.removeAll
import knf.kuma.database.CacheDB
import knf.kuma.pojos.GenreStatusObject
import org.jetbrains.anko.doAsync

/**
 * Created by jordy on 26/03/2018.
 */

object RecommendHelper {
    fun registerAll(list: MutableList<String>, type: RankType) {
        doAsync {
            for (genre in list)
                register(genre, type)
            syncData { genres() }
        }
    }

    private fun register(name: String, type: RankType) {
        doAsync {
            val status = getStatus(name)
            if (!status.isBlocked) {
                when (type) {
                    RankType.FAV -> status.add(3)
                    RankType.UNFAV -> status.sub(3)
                    RankType.FOLLOW -> status.add(2)
                    RankType.UNFOLLOW -> status.sub(2)
                    RankType.CHECK -> status.add(1)
                    RankType.SEARCH -> status.add(1)
                }
                CacheDB.INSTANCE.genresDAO().insertStatus(status)
            }
        }
    }

    fun block(name: String) {
        doAsync {
            val status = getStatus(name)
            status.block()
            CacheDB.INSTANCE.genresDAO().insertStatus(status)
            syncData { genres() }
        }
    }

    fun reset(name: String) {
        doAsync {
            val status = getStatus(name)
            status.reset()
            CacheDB.INSTANCE.genresDAO().insertStatus(status)
            syncData { genres() }
        }
    }

    private fun getStatus(name: String): GenreStatusObject {
        var status: GenreStatusObject? = CacheDB.INSTANCE.genresDAO().getStatus(name)
        if (status == null) status = GenreStatusObject(name)
        return status
    }

    fun createRecommended(onCreate: (list: List<AnimeShortObject>) -> Unit) {
        doAsync {
            val status = CacheDB.INSTANCE.genresDAO().top
            if (status.size <= 2) {
                onCreate(emptyList())
                return@doAsync
            }
            val excludeList = LinkedHashSet<String>().apply {
                addAll(CacheDB.INSTANCE.favsDAO().allAids)
                addAll(CacheDB.INSTANCE.seeingDAO().allAids)
            }.toList()
            val abc = getList(status[0], status[1], status[2])
            val ab = getList(status[0], status[1])
            ab.removeAll(abc)
            val ac = getList(status[0], status[2])
            ac.removeAll(abc, ab)
            val bc = getList(status[1], status[2])
            bc.removeAll(abc, ab, ac)
            val a = getList(status[0])
            a.removeAll(abc, ab, ac, bc)
            val b = getList(status[1])
            b.removeAll(abc, ab, ac, bc, a)
            val c = getList(status[2])
            c.removeAll(abc, ab, ac, bc, a, b)
            removeFavs(excludeList, abc, ab, ac, bc, a, b, c)
            val list = mutableListOf<AnimeShortObject>().apply {
                addAll(getAnimeList(abc))
                addAll(getAnimeList(ab))
                addAll(getAnimeList(ac))
                addAll(getAnimeList(bc))
                addAll(getAnimeList(a))
                addAll(getAnimeList(b))
                addAll(getAnimeList(c))
            }
            onCreate(list)
        }
    }

    @SafeVarargs
    private fun removeFavs(excludeList: List<String>, vararg lists: MutableList<String>) {
        lists.forEach { list ->
            list.removeAll(list.filter { excludeList.contains(it) })
        }
    }

    private fun getList(vararg status: GenreStatusObject): MutableList<String> {
        return CacheDB.INSTANCE.animeDAO().getAidsByGenresLimited(getString(*status))
    }

    private fun getAnimeList(list: List<String>): MutableList<AnimeShortObject> {
        val chunk = list.chunked(900)
        val animes = mutableListOf<AnimeShortObject>()
        chunk.forEach {
            animes.addAll(CacheDB.INSTANCE.animeDAO().getAnimesByAids(it))
        }
        return animes
    }

    private fun getString(vararg status: GenreStatusObject): String {
        val builder = StringBuilder("%")
        for (s in status) {
            builder.append(s.name)
                    .append("%")
        }
        return builder.toString()
    }
}
