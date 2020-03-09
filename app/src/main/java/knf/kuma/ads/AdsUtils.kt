package knf.kuma.ads

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import com.appodeal.ads.Appodeal
import com.google.android.gms.ads.AdSize
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import knf.kuma.App
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

object AdsUtils {
    val remoteConfigs = FirebaseRemoteConfig.getInstance().apply {
        setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder().apply { fetchTimeoutInSeconds = 5 }.build())
        setDefaultsAsync(mapOf(
                "admob_enabled" to false,
                "appbrains_enabled" to false,
                "startapp_enabled" to false,
                "appodeal_enabled" to true,
                "admob_percent" to 90.0,
                "appodeal_percent" to 100.0,
                "appbrains_percent" to 10.0,
                "startapp_percent" to 100.0,
                "appodeal_percent" to 100.0,
                "appodeal_fullscreen_percent" to 100.0,
                "admob_fullscreen_percent" to 0.0,
                "appbrains_fullscreen_percent" to 0.0,
                "startappp_fullscreen_percent" to 0.0,
                "appodeal_fullscreen_percent" to 0.0,
                "rewarded_percent" to 90.0,
                "interstitial_percent" to 10.0)
        )
        fetchAndActivate().addOnSuccessListener {
            Log.e("Remote config", "Updated: $it")
        }
    }
}

fun Activity.preload(list: List<*>) {
    if (PrefsUtil.isAdsEnabled && list.isNotEmpty()) {
        if (AdsUtils.remoteConfigs.getBoolean("appodeal_enabled"))
            Appodeal.cache(this, Appodeal.NATIVE, list.size / 5)
    }
}

fun MutableList<RecentObject>.implAdsRecent() {
    if (PrefsUtil.isAdsEnabled)
        probabilityOf({ implAdsRecentBrains() }) {
            if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
                item({ implAdsRecentMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
            if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                item({ implAdsRecentBrains() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
            if (AdsUtils.remoteConfigs.getBoolean("startapp_enabled"))
                item({ implAdsRecentStartApp() }, AdsUtils.remoteConfigs.getDouble("startapp_percent"))
            if (AdsUtils.remoteConfigs.getBoolean("appodeal_enabled"))
                item({ implAdsRecentAppOdeal() }, AdsUtils.remoteConfigs.getDouble("appodeal_percent"))
        }.random()()
}

fun MutableList<FavoriteObject>.implAdsFavorite() {
    probabilityOf({ implAdsFavoriteBrains() }) {
        if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
            item({ implAdsFavoriteMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
            item({ implAdsFavoriteBrains() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("startapp_enabled"))
            item({ implAdsFavoriteStartApp() }, AdsUtils.remoteConfigs.getDouble("startapp_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("appodeal_enabled"))
            item({ implAdsFavoriteAppOdeal() }, AdsUtils.remoteConfigs.getDouble("appodeal_percent"))
    }.random()()
}

fun MutableList<NewsObject>.implAdsNews() {
    probabilityOf({ implAdsNewsBrain() }) {
        if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
            item({ implAdsNewsMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
            item({ implAdsNewsBrain() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("startapp_enabled"))
            item({ implAdsNewsStartApp() }, AdsUtils.remoteConfigs.getDouble("startapp_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("appodeal_enabled"))
            item({ implAdsNewsAppOdeal() }, AdsUtils.remoteConfigs.getDouble("appodeal_percent"))
    }.random()()
}

fun MutableList<Achievement>.implAdsAchievement() {
    probabilityOf({ implAdsAchievementBrain() }) {
        if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
            item({ implAdsAchievementMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
            item({ implAdsAchievementBrain() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("startapp_enabled"))
            item({ implAdsAchievementStartApp() }, AdsUtils.remoteConfigs.getDouble("startapp_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("appodeal_enabled"))
            item({ implAdsAchievementAppOdeal() }, AdsUtils.remoteConfigs.getDouble("appodeal_percent"))
    }.random()()
}

fun ViewGroup.implBannerCast() {
    probabilityOf({ implBannerCastBrains() }) {
        if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
            item({ implBannerCastMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
            item({ implBannerCastBrains() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("startapp_enabled"))
            item({ implBannerCastStartApp() }, AdsUtils.remoteConfigs.getDouble("startapp_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("appodeal_enabled"))
            item({ implBannerCastAppOdeal() }, AdsUtils.remoteConfigs.getDouble("appodeal_percent"))
    }.random()()
}

fun ViewGroup.implBanner(unitID: String, isSmart: Boolean = false) {
    probabilityOf({ implBannerBrains(unitID, isSmart) }) {
        if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
            item({ implBannerMob(unitID, isSmart) }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
            item({ implBannerBrains(unitID, isSmart) }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("startapp_enabled"))
            item({ implBannerStartApp(unitID, isSmart) }, AdsUtils.remoteConfigs.getDouble("startapp_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("appodeal_enabled"))
            item({ implBannerAppOdeal(unitID, isSmart) }, AdsUtils.remoteConfigs.getDouble("appodeal_percent"))
    }.random()()
}

fun ViewGroup.implBanner(unitID: AdsType, isSmart: Boolean = false) {
    probabilityOf({ implBannerBrains(unitID, isSmart) }) {
        if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
            item({ implBannerMob(unitID, isSmart) }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
            item({ implBannerBrains(unitID, isSmart) }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("startapp_enabled"))
            item({ implBannerStartApp(unitID, isSmart) }, AdsUtils.remoteConfigs.getDouble("startapp_percent"))
        if (AdsUtils.remoteConfigs.getBoolean("appodeal_enabled"))
            item({ implBannerAppOdeal(unitID, isSmart) }, AdsUtils.remoteConfigs.getDouble("appodeal_percent"))
    }.random()()
}

fun getFAdLoaderRewarded(context: Activity, onUpdate: () -> Unit = {}): FullscreenAdLoader =
        probabilityOf({ getFAdLoaderBrains(context, onUpdate) }) {
            if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
                item({ getFAdLoaderRewardedMob(context, onUpdate) }, AdsUtils.remoteConfigs.getDouble("admob_fullscreen_percent"))
            if (AdsUtils.remoteConfigs.getBoolean("startapp_enabled"))
                item({ getFAdLoaderRewardedStartApp(context, onUpdate) }, AdsUtils.remoteConfigs.getDouble("startapp_fullscreen_percent"))
            if (AdsUtils.remoteConfigs.getBoolean("appodeal_enabled"))
                item({ getFAdLoaderRewardedAppOdeal(context, onUpdate) }, AdsUtils.remoteConfigs.getDouble("appodeal_fullscreen_percent"))
        }.random()()

fun getFAdLoaderInterstitial(context: Activity, onUpdate: () -> Unit = {}): FullscreenAdLoader =
        probabilityOf({ getFAdLoaderBrains(context, onUpdate) }) {
            if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
                item({ getFAdLoaderInterstitialMob(context, onUpdate) }, AdsUtils.remoteConfigs.getDouble("admob_fullscreen_percent"))
            if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                item({ getFAdLoaderBrains(context, onUpdate) }, AdsUtils.remoteConfigs.getDouble("appbrains_fullscreen_percent"))
            if (AdsUtils.remoteConfigs.getBoolean("startapp_enabled"))
                item({ getFAdLoaderInterstitialStartApp(context, onUpdate) }, AdsUtils.remoteConfigs.getDouble("startapp_fullscreen_percent"))
            if (AdsUtils.remoteConfigs.getBoolean("appodeal_enabled"))
                item({ getFAdLoaderInterstitialAppOdeal(context, onUpdate) }, AdsUtils.remoteConfigs.getDouble("appodeal_fullscreen_percent"))
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