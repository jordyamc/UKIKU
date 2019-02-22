package knf.kuma.ads

import knf.kuma.pojos.RecentObject

data class AdRecentObject(private val adID: String) : RecentObject(), AdCallback {
    override fun getID(): String {
        return adID
    }
}