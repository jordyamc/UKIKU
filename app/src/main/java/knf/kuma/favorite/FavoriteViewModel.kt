package knf.kuma.favorite

import androidx.lifecycle.ViewModel
import knf.kuma.commons.PrefsUtil
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.FavoriteAV1
import knf.kuma.pojos.av1.FavoriteBase
import knf.kuma.pojos.av1.FavoriteSectionAV1
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest

class FavoriteViewModel : ViewModel() {

    val favoriteListFlow = PrefsUtil.favsOrderFlow.flatMapLatest { sortType ->
        CacheDB.INSTANCE.favoriteAV1DAO().byCategoryFlow.mapLatest {
            if (PrefsUtil.showFavSections()) {
                val map = mutableMapOf<String, MutableList<FavoriteAV1>>()
                it.forEach { item ->
                    val list = map[item.category] ?: mutableListOf()
                    list.add(item)
                    map[item.category] =
                        (if (sortType == 1) list.sortedBy { it.aid } else list.sortedBy { it.name }).toMutableList()
                }
                val list = mutableListOf<FavoriteBase>()
                map.forEach {
                    list.add(FavoriteSectionAV1(it.key))
                    list.addAll(it.value)
                }
                list
            } else {
                if (sortType == 1) it.sortedBy { it.aid } else it.sortedBy { it.name }
            }
        }
    }

}
