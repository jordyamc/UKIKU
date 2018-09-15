package knf.kuma.recommended

import knf.kuma.database.CacheDB
import knf.kuma.pojos.GenreStatusObject

/**
 * Created by jordy on 26/03/2018.
 */

object RecommendHelper {
    fun registerAll(list: MutableList<String>, type: RankType) {
        for (genre in list)
            register(genre, type)
    }

    private fun register(name: String, type: RankType) {
        val status = getStatus(name)
        if (status.isBlocked) return
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

    fun block(name: String) {
        val status = getStatus(name)
        status.block()
        CacheDB.INSTANCE.genresDAO().insertStatus(status)
    }

    fun reset(name: String) {
        val status = getStatus(name)
        status.reset()
        CacheDB.INSTANCE.genresDAO().insertStatus(status)
    }

    private fun getStatus(name: String): GenreStatusObject {
        var status: GenreStatusObject? = CacheDB.INSTANCE.genresDAO().getStatus(name)
        if (status == null) status = GenreStatusObject(name)
        return status
    }
}
