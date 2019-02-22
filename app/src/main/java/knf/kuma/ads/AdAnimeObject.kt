package knf.kuma.ads

import knf.kuma.pojos.AnimeObject

data class AdAnimeObject(private val adID: String) : AnimeObject(), AdCallback {
    override fun getID(): String {
        return adID
    }
}