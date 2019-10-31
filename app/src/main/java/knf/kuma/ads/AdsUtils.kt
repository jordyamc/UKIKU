package knf.kuma.ads

import android.app.Activity
import android.view.ViewGroup
import com.google.android.gms.ads.AdSize
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.news.NewsObject
import knf.kuma.pojos.Achievement
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.RecentObject
import knf.tools.kprobability.item
import knf.tools.kprobability.probabilityOf

enum class AdsType {
    RECENT_BANNER,
    RECENT_BANNER2,
    FAVORITE_BANNER,
    FAVORITE_BANNER2,
    DIRECTORY_BANNER,
    HOME_BANNER,
    HOME_BANNER2,
    EMISSION_BANNER,
    SEEING_BANNER,
    RECOMMEND_BANNER,
    QUEUE_BANNER,
    RECORD_BANNER,
    NEWS_BANNER,
    RANDOM_BANNER,
    INFO_BANNER,
    ACHIEVEMENT_BANNER,
    EXPLORER_BANNER,
    CAST_BANNER,
    REWARDED,
    INTERSTITIAL
}

fun MutableList<RecentObject>.implAdsRecent() {
    if (PrefsUtil.isAdsEnabled)
        probabilityOf<() -> Unit> {
            if (!Network.isAdsBlocked && BuildConfig.BUILD_TYPE == "playstore")
                item({ implAdsRecentMob() }, 90.0)
            item({ implAdsRecentBrains() }, 10.0)
        }.random()()
}

fun MutableList<FavoriteObject>.implAdsFavorite() {
    probabilityOf<() -> Unit> {
        if (!Network.isAdsBlocked && BuildConfig.BUILD_TYPE == "playstore")
            item({ implAdsFavoriteMob() }, 90.0)
        item({ implAdsFavoriteBrains() }, 10.0)
    }.random()()
}

fun MutableList<NewsObject>.implAdsNews() {
    probabilityOf<() -> Unit> {
        if (!Network.isAdsBlocked && BuildConfig.BUILD_TYPE == "playstore")
            item({ implAdsNewsMob() }, 90.0)
        item({ implAdsNewsBrain() }, 10.0)
    }.random()()
}

fun MutableList<Achievement>.implAdsAchievement() {
    probabilityOf<() -> Unit> {
        if (!Network.isAdsBlocked && BuildConfig.BUILD_TYPE == "playstore")
            item({ implAdsAchievementMob() }, 90.0)
        item({ implAdsAchievementBrain() }, 10.0)
    }.random()()
}

fun ViewGroup.implBannerCast() {
    probabilityOf<() -> Unit> {
        if (!Network.isAdsBlocked && BuildConfig.BUILD_TYPE == "playstore")
            item({ implBannerCastMob() }, 90.0)
        item({ implBannerCastBrains() }, 10.0)
    }.random()()
}

fun ViewGroup.implBanner(unitID: String, isSmart: Boolean = false) {
    probabilityOf<() -> Unit> {
        if (!Network.isAdsBlocked && BuildConfig.BUILD_TYPE == "playstore")
            item({ implBannerMob(unitID, isSmart) }, 90.0)
        item({ implBannerBrains(unitID, isSmart) }, 10.0)
    }.random()()
}

fun ViewGroup.implBanner(unitID: AdsType, isSmart: Boolean = false) {
    probabilityOf<() -> Unit> {
        if (!Network.isAdsBlocked && BuildConfig.BUILD_TYPE == "playstore")
            item({ implBannerMob(unitID, isSmart) }, 90.0)
        item({ implBannerBrains(unitID, isSmart) }, 10.0)
    }.random()()
}

fun getFAdLoaderRewarded(context: Activity, onUpdate: () -> Unit = {}): FullscreenAdLoader = getFAdLoaderRewardedMob(context, onUpdate)

fun getFAdLoaderInterstitial(context: Activity, onUpdate: () -> Unit = {}): FullscreenAdLoader =
        probabilityOf<() -> FullscreenAdLoader> {
            if (!Network.isAdsBlocked && BuildConfig.BUILD_TYPE == "playstore")
                item({ getFAdLoaderInterstitialMob(context, onUpdate) }, 90.0)
            item({ getFAdLoaderBrains(context, onUpdate) }, 10.0)
        }.random()()

fun getAdSize(width: Float): AdSize {
    val metrics = App.context.resources.displayMetrics
    val density = metrics.density
    var adWidthPixels = width
    if (adWidthPixels == 0f) {
        adWidthPixels = metrics.widthPixels.toFloat()
    }

    val adWidth = (adWidthPixels / density).toInt()
    return AdSize.getCurrentOrientationBannerAdSizeWithWidth(App.context, adWidth)

}

interface FullscreenAdLoader {
    fun load()
    fun show()
}