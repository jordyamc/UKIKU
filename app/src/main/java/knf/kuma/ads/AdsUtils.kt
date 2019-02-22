package knf.kuma.ads

import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import knf.kuma.commons.PrefsUtil
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.RecentObject
import xdroid.core.Global

object AdsUtils {
    const val RECENT_BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val RECENT_BANNER2 = "ca-app-pub-3940256099942544/6300978111"
    const val FAVORITE_BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val FAVORITE_BANNER2 = "ca-app-pub-3940256099942544/6300978111"
    const val CAST_BANNER = "ca-app-pub-3940256099942544/6300978111"
}

fun MutableList<RecentObject>.implAdsRecent() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    ArrayList(this).forEachIndexed { index, _ ->
        if (index % 5 == 0 && index > 0) {
            val adID: String = when (adIndex) {
                0 -> {
                    adIndex = 1
                    AdsUtils.RECENT_BANNER
                }
                else -> {
                    adIndex = 0
                    AdsUtils.RECENT_BANNER2
                }
            }
            add(index, AdRecentObject(adID))
        }
    }
}

fun MutableList<FavoriteObject>.implAdsFavorite() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    ArrayList(this).apply {
        forEachIndexed { index, _ ->
            if (index % 8 == 0 && index > 0 && !this[index - 1].isSection) {
                val adID: String = when (adIndex) {
                    0 -> {
                        adIndex = 1
                        AdsUtils.FAVORITE_BANNER
                    }
                    else -> {
                        adIndex = 0
                        AdsUtils.FAVORITE_BANNER2
                    }
                }
                this@implAdsFavorite.add(index, AdFavoriteObject(adID))
            }
        }
    }
}

fun ViewGroup.implBanner(unitID: String, isSmart: Boolean = false) {
    val adSize = if (isSmart) {
        val dm = Global.getResources().displayMetrics
        val density = (dm.density * 160).toDouble()
        val x = Math.pow(dm.widthPixels / density, 2.0)
        val y = Math.pow(dm.heightPixels / density, 2.0)
        val screenInches = Math.sqrt(x + y)
        when {
            screenInches > 8 -> // > 728 X 90
                AdSize.LEADERBOARD
            screenInches > 6 -> // > 468 X 60
                AdSize.MEDIUM_RECTANGLE
            else -> // > 320 X 50
                AdSize.BANNER
        }
    } else
        AdSize.BANNER
    removeAllViews()
    val adView = AdView(context)
    adView.adSize = adSize
    adView.adUnitId = unitID
    addView(adView)
    adView.loadAd(AdRequest.Builder().build())
}