package knf.kuma.ads

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdSize
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.diceOf
import knf.kuma.commons.noCrash
import knf.kuma.commons.noCrashLet
import knf.kuma.news.NewsObject
import knf.kuma.pojos.Achievement
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.RecentObject
import org.nield.kotlinstatistics.weightedCoinFlip

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
        if (BuildConfig.DEBUG)
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder().apply { minimumFetchIntervalInSeconds = 0 }
                    .build()
            )
        setDefaultsAsync(
            mapOf(
                "admob_enabled" to true,
                "appbrains_enabled" to false,
                "startapp_enabled" to false,
                "appodeal_enabled" to false,
                "applovin_enabled" to false,
                "admob_use_fallback" to false,
                "ads_forced" to false,
                "ads_remote_banner" to true,
                "ads_remote_full" to true,
                "ads_remote" to 1.0,
                "admob_percent" to 100.0,
                "appodeal_percent" to 100.0,
                "applovin_percent" to 100.0,
                "appbrains_percent" to 0.0,
                "startapp_percent" to 0.0,
                "appodeal_fullscreen_percent" to 100.0,
                "applovin_fullscreen_percent" to 100.0,
                "admob_fullscreen_percent" to 100.0,
                "appbrains_fullscreen_percent" to 100.0,
                "startappp_fullscreen_percent" to 100.0,
                "appodeal_fullscreen_percent" to 100.0,
                "rewarded_percent" to 90.0,
                "interstitial_percent" to 10.0,
                "disqus_version" to "9e3da5ae8d7caf8389087c4c35a6ca1b",
                "min_version" to 169L,
                "samsung_disable_foreground" to false,
                "bypass_show_reload" to false,
                "bypass_clear_cookies" to false,
                "bypass_max_tries" to 3L,
                "bypass_skip_captcha" to true,
                "bypass_use_dialog" to true,
                "bypass_dialog_style" to 1L,
                "full_show_extra_probability" to 60.0,
                "full_show_probability" to 70.0
            )
        )
        fetch().addOnCompleteListener {
            it.exception?.printStackTrace()
            if (it.isSuccessful) {
                FirebaseRemoteConfig.getInstance().activate()
            }
        }
    }

    val isRemoteAdsEnabled by lazy {
        weightedCoinFlip(remoteConfigs.getDouble("ads_remote"))
    }
    val isRemoteBannerEnabled get() = remoteConfigs.getBoolean("ads_remote_banner")
    val isRemoteFullEnabled get() = remoteConfigs.getBoolean("ads_remote_full")
    val isAdmobEnabled get() = remoteConfigs.getBoolean("admob_enabled")
    val isApplovinEnabled get() = remoteConfigs.getBoolean("applovin_enabled")

    fun setUp(context: Activity, callback: () -> Unit) {
        if (!isRemoteAdsEnabled || !listOf(isAdmobEnabled, isApplovinEnabled).any { it }) {
            Log.e("ADS", "All disabled")
            callback()
            return
        }
        if (isApplovinEnabled) {
            Log.e("ADS", "Applovin enabled")
            AdsUtilsLovin.setUp(context, callback)
        }
        if (isAdmobEnabled) {
            Log.e("ADS", "Admob enabled")
            AdsUtilsMob.setUp(context, callback)
        }
        callback()
    }
}

fun MutableList<RecentObject>.implAdsRecent() {
    if (AdsUtils.isRemoteAdsEnabled && AdsUtils.isRemoteBannerEnabled && PrefsUtil.isAdsEnabled)
        noCrash {
            diceOf({ implAdsRecentBrains() }) {
                if (AdsUtils.isAdmobEnabled)
                    put({ implAdsRecentMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
                if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                    put({ implAdsRecentBrains() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
                if (AdsUtils.isApplovinEnabled)
                    put({ implAdsRecentLovin() }, AdsUtils.remoteConfigs.getDouble("applovin_percent"))
            }()
        }
}

fun MutableList<FavoriteObject>.implAdsFavorite() {
    if (AdsUtils.isRemoteAdsEnabled && AdsUtils.isRemoteBannerEnabled && PrefsUtil.isAdsEnabled)
        noCrash {
            diceOf({ implAdsFavoriteBrains() }) {
                if (AdsUtils.isAdmobEnabled)
                    put({ implAdsFavoriteMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
                if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                    put({ implAdsFavoriteBrains() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
                if (AdsUtils.isApplovinEnabled)
                    put({ implAdsFavoriteLovin() }, AdsUtils.remoteConfigs.getDouble("applovin_percent"))
            }()
        }
}

fun MutableList<NewsObject>.implAdsNews() {
    if (AdsUtils.isRemoteAdsEnabled && AdsUtils.isRemoteBannerEnabled && PrefsUtil.isAdsEnabled)
        noCrash {
            diceOf({ implAdsNewsBrain() }) {
                if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
                    put({ implAdsNewsMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
                if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                    put({ implAdsNewsBrain() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
                if (AdsUtils.isApplovinEnabled)
                    put({ implAdsNewsLovin() }, AdsUtils.remoteConfigs.getDouble("applovin_percent"))
            }()
        }
}

fun MutableList<Achievement>.implAdsAchievement() {
    if (AdsUtils.isRemoteAdsEnabled && AdsUtils.isRemoteBannerEnabled && PrefsUtil.isAdsEnabled)
        noCrash {
            diceOf({ implAdsAchievementBrain() }) {
                if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
                    put({ implAdsAchievementMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
                if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                    put({ implAdsAchievementBrain() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
                if (AdsUtils.isApplovinEnabled)
                    put({ implAdsAchievementLovin() }, AdsUtils.remoteConfigs.getDouble("applovin_percent"))
            }()
        }
}

fun ViewGroup.implBannerCast() {
    if (AdsUtils.isRemoteAdsEnabled && AdsUtils.isRemoteBannerEnabled && PrefsUtil.isAdsEnabled)
        noCrash {
            diceOf({ implBannerCastBrains() }) {
                if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
                    put({ implBannerCastMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
                if (AdsUtils.remoteConfigs.getBoolean("applovin_enabled"))
                    put({ implBannerCastLovin() }, AdsUtils.remoteConfigs.getDouble("applovin_percent"))
                if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                    put({ implBannerCastBrains() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
            }()
        }
}

fun ViewGroup.implBanner(unitID: String, isSmart: Boolean = false) {
    if (AdsUtils.isRemoteAdsEnabled && AdsUtils.isRemoteBannerEnabled && PrefsUtil.isAdsEnabled)
        noCrash {
            diceOf({ implBannerBrains(unitID, isSmart) }) {
                if (AdsUtils.isAdmobEnabled)
                    put(
                        { implBannerMob(unitID, isSmart) },
                        AdsUtils.remoteConfigs.getDouble("admob_percent")
                    )
                if (AdsUtils.isApplovinEnabled)
                    put(
                        { implBannerLovin(AdsType.RECENT_BANNER) },
                        AdsUtils.remoteConfigs.getDouble("applovin_percent")
                    )
                if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                    put(
                        { implBannerBrains(unitID, isSmart) },
                        AdsUtils.remoteConfigs.getDouble("appbrains_percent")
                    )
            }()
        }
}

fun ViewGroup.implBanner(unitID: AdsType, isSmart: Boolean = false) {
    if (AdsUtils.isRemoteAdsEnabled && AdsUtils.isRemoteBannerEnabled && PrefsUtil.isAdsEnabled)
        noCrash {
            diceOf({ implBannerBrains(unitID, isSmart) }) {
                if (AdsUtils.isAdmobEnabled)
                    put(
                        { implBannerMob(unitID, isSmart) },
                        AdsUtils.remoteConfigs.getDouble("admob_percent")
                    )
                if (AdsUtils.isApplovinEnabled)
                    put(
                        { implBannerLovin(unitID) },
                        AdsUtils.remoteConfigs.getDouble("applovin_percent")
                    )
                if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                    put(
                        { implBannerBrains(unitID, isSmart) },
                        AdsUtils.remoteConfigs.getDouble("appbrains_percent")
                    )
            }()
        }
}

fun getFAdLoaderRewarded(context: Activity, onUpdate: () -> Unit = {}): FullscreenAdLoader =
    noCrashLet(getFAdLoaderBrains(context, onUpdate)) {
        diceOf({ getFAdLoaderBrains(context, onUpdate) }) {
            if (AdsUtils.isAdmobEnabled)
                put(
                    { getFAdLoaderRewardedMob(context, onUpdate) },
                    AdsUtils.remoteConfigs.getDouble("admob_fullscreen_percent")
                )
            if (AdsUtils.isApplovinEnabled)
                put(
                    { getFAdLoaderInterstitialLovin(context, onUpdate) },
                    AdsUtils.remoteConfigs.getDouble("applovin_fullscreen_percent")
                )
        }()
    }

fun getFAdLoaderInterstitial(context: Activity, onUpdate: () -> Unit = {}): FullscreenAdLoader =
    noCrashLet(getFAdLoaderBrains(context, onUpdate)) {
        diceOf({ getFAdLoaderBrains(context, onUpdate) }) {
            if (AdsUtils.isAdmobEnabled)
                put(
                    { getFAdLoaderInterstitialMob(context, onUpdate) },
                    AdsUtils.remoteConfigs.getDouble("admob_fullscreen_percent")
                )
            if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                put(
                    { getFAdLoaderBrains(context, onUpdate) },
                    AdsUtils.remoteConfigs.getDouble("appbrains_fullscreen_percent")
                )
            if (AdsUtils.isApplovinEnabled)
                put(
                    { getFAdLoaderInterstitialLovin(context, onUpdate) },
                    AdsUtils.remoteConfigs.getDouble("applovin_fullscreen_percent")
                )
        }()
    }

fun showRandomInterstitial(
    context: AppCompatActivity,
    probability: Float = PrefsUtil.fullAdsProbability
) {
    if (AdsUtils.isRemoteAdsEnabled && PrefsUtil.isAdsEnabled && PrefsUtil.isFullAdsEnabled && probability > 0) {
        val probDefault = 100f - probability
        diceOf<() -> Unit> {
            if (AdsUtils.isAdmobEnabled)
                put(
                    { FAdLoaderInterstitialLazyMob(context).show() },
                    probability.toDouble()
                )
            if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                put(
                    { FAdLoaderInterstitialLazyBrains(context).show() },
                    probability.toDouble()
                )
            if (AdsUtils.isApplovinEnabled)
                put(
                    { FAdLoaderInterstitialLazyLovin(context).show() },
                    probability.toDouble()
                )
            put({}, probDefault.toDouble())
        }()
    }
}

fun getAdSize(width: Float): AdSize {
    val metrics = App.context.resources.displayMetrics
    val density = metrics.density
    var adWidthPixels = width
    if (adWidthPixels == 0f) {
        adWidthPixels = metrics.widthPixels.toFloat()
    }

    val adWidth = (adWidthPixels / density).toInt()
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(App.context, adWidth)

}

interface FullscreenAdLoader {
    fun load()
    fun show()
}