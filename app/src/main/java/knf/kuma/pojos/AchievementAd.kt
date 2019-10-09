package knf.kuma.pojos

import knf.kuma.ads.AdCallback

class AchievementAd(private val adId: String) : Achievement(0, "", "", 0, false), AdCallback {
    override fun getID(): String = adId
}