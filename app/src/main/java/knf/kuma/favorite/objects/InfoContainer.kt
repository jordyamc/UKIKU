package knf.kuma.favorite.objects

import knf.kuma.pojos.FavSection
import knf.kuma.pojos.FavoriteObject
import java.util.*

class InfoContainer {
    var updated: MutableList<FavoriteObject>? = arrayListOf()
    var needReload = false
    var from: Int = 0
    var to: Int = 0
    private var current: MutableList<FavoriteObject>? = null

    fun setLists(current: MutableList<FavoriteObject>, updated: MutableList<FavoriteObject>) {
        this.current = ArrayList(current)
        this.updated = ArrayList(updated)
    }

    fun reload(favoriteObject: FavoriteObject?) {
        when {
            favoriteObject == null -> needReload = true
            favoriteObject is FavSection -> needReload = true
            updated?.contains(favoriteObject) == false -> needReload = true
            current?.size != updated?.size -> needReload = true
            else -> {
                needReload = false
                from = current?.indexOf(favoriteObject) ?: -1
                to = updated?.indexOf(favoriteObject) ?: -1
            }
        }
    }
}
