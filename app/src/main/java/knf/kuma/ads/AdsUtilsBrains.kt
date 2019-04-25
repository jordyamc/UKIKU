package knf.kuma.ads

import android.view.ViewGroup
import com.appbrain.AppBrainBanner
import knf.kuma.commons.PrefsUtil
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.RecentObject

object AdsUtilsBrains {
    const val RECENT_BANNER = "recent_banner"
    const val RECENT_BANNER2 = "recent_banner_2"
    const val FAVORITE_BANNER = "favorite_banner"
    const val FAVORITE_BANNER2 = "favorite_banner_2"
    const val CAST_BANNER = "cast_banner"
}

fun MutableList<RecentObject>.implAdsRecentBrains() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    ArrayList(this).forEachIndexed { index, _ ->
        if (index % 5 == 0 && index > 0) {
            val adID: String = when (adIndex) {
                0 -> {
                    adIndex = 1
                    AdsUtilsBrains.RECENT_BANNER
                }
                else -> {
                    adIndex = 0
                    AdsUtilsBrains.RECENT_BANNER2
                }
            }
            add(index, AdRecentObject(adID))
        }
    }
}

fun MutableList<FavoriteObject>.implAdsFavoriteBrains() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    ArrayList(this).apply {
        forEachIndexed { index, _ ->
            if (index % 8 == 0 && index > 0 && !this[index - 1].isSection) {
                val adID: String = when (adIndex) {
                    0 -> {
                        adIndex = 1
                        AdsUtilsBrains.FAVORITE_BANNER
                    }
                    else -> {
                        adIndex = 0
                        AdsUtilsBrains.FAVORITE_BANNER2
                    }
                }
                this@implAdsFavoriteBrains.add(index, AdFavoriteObject(adID))
            }
        }
    }
}

fun ViewGroup.implBannerCastBrains() {
    this.implBannerBrains(AdsUtilsBrains.CAST_BANNER)
}

fun ViewGroup.implBannerBrains(unitID: String, isSmart: Boolean = false) {
    removeAllViews()
    val adView = AppBrainBanner(context)
    addView(adView)
}