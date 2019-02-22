package knf.kuma.ads

import knf.kuma.pojos.FavoriteObject

data class AdFavoriteObject(private val adID: String) : FavoriteObject(), AdCallback {
    override fun getID(): String {
        return adID
    }
}