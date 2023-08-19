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
                "admob_use_fallback" to false,
                "ads_forced" to false,
                "admob_percent" to 100.0,
                "appodeal_percent" to 100.0,
                "appbrains_percent" to 0.0,
                "startapp_percent" to 0.0,
                "appodeal_fullscreen_percent" to 100.0,
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

    val isAdmobEnabled get() = remoteConfigs.getBoolean("admob_enabled")
    val isAppodealEnabled get() = remoteConfigs.getBoolean("appodeal_enabled")

    fun setUp(context: Activity, callback: () -> Unit) {
        if (isAppodealEnabled) {
            Log.e("ADS", "Appodeal enabled")
            AdsUtilsAppodeal.setUp(context, callback)
        }
        if (isAdmobEnabled) {
            Log.e("ADS", "Admob enabled")
            AdsUtilsMob.setUp(context, callback)
        }
    }
}

fun MutableList<RecentObject>.implAdsRecent() {
    if (PrefsUtil.isAdsEnabled)
        noCrash {
            diceOf({ implAdsRecentBrains() }) {
                if (AdsUtils.isAdmobEnabled)
                    put({ implAdsRecentMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
                if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                    put({ implAdsRecentBrains() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
                if (AdsUtils.isAppodealEnabled)
                    put({ implAdsRecentDeal() }, AdsUtils.remoteConfigs.getDouble("appodeal_percent"))
            }()
        }
}

fun MutableList<FavoriteObject>.implAdsFavorite() {
    noCrash {
        diceOf({ implAdsFavoriteBrains() }) {
            if (AdsUtils.isAdmobEnabled)
                put({ implAdsFavoriteMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
            if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                put({ implAdsFavoriteBrains() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
            if (AdsUtils.isAppodealEnabled)
                put({ implAdsFavoriteDeal() }, AdsUtils.remoteConfigs.getDouble("appodeal_percent"))
        }()
    }
}

fun MutableList<NewsObject>.implAdsNews() {
    noCrash {
        diceOf({ implAdsNewsBrain() }) {
            if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
                put({ implAdsNewsMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
            if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                put({ implAdsNewsBrain() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
            if (AdsUtils.isAppodealEnabled)
                put({ implAdsNewsDeal() }, AdsUtils.remoteConfigs.getDouble("appodeal_percent"))
        }()
    }
}

fun MutableList<Achievement>.implAdsAchievement() {
    noCrash {
        diceOf({ implAdsAchievementBrain() }) {
            if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
                put({ implAdsAchievementMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
            if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                put({ implAdsAchievementBrain() }, AdsUtils.remoteConfigs.getDouble("appbrains_percent"))
            if (AdsUtils.isAppodealEnabled)
                put({ implAdsAchievementDeal() }, AdsUtils.remoteConfigs.getDouble("appodeal_percent"))
        }()
    }
}

fun ViewGroup.implBannerCast() {
    noCrash {
        diceOf({ implBannerCastBrains() }) {
            if (AdsUtils.remoteConfigs.getBoolean("admob_enabled"))
                put({ implBannerCastMob() }, AdsUtils.remoteConfigs.getDouble("admob_percent"))
            if (AdsUtils.remoteConfigs.getBoolean("appbrains_enabled"))
                put(
                    { implBannerCastBrains() },
                    AdsUtils.remoteConfigs.getDouble("appbrains_percent")
                )
        }()
    }
}

fun ViewGroup.implBanner(unitID: String, isSmart: Boolean = false) {
    noCrash {
        diceOf({ implBannerBrains(unitID, isSmart) }) {
            if (AdsUtils.isAdmobEnabled)
                put(
                    { implBannerMob(unitID, isSmart) },
                    AdsUtils.remoteConfigs.getDouble("admob_percent")
                )
            if (AdsUtils.isAppodealEnabled)
                put(
                    { implBannerDeal(AdsType.RECENT_BANNER) },
                    AdsUtils.remoteConfigs.getDouble("appodeal_percent")
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
    noCrash {
        diceOf({ implBannerBrains(unitID, isSmart) }) {
            if (AdsUtils.isAdmobEnabled)
                put(
                    { implBannerMob(unitID, isSmart) },
                    AdsUtils.remoteConfigs.getDouble("admob_percent")
                )
            if (AdsUtils.isAppodealEnabled)
                put(
                    { implBannerDeal(unitID) },
                    AdsUtils.remoteConfigs.getDouble("appodeal_percent")
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
            if (AdsUtils.isAppodealEnabled)
                put(
                    { getFAdLoaderRewardedDeal(context, onUpdate) },
                    AdsUtils.remoteConfigs.getDouble("appodeal_fullscreen_percent")
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
            if (AdsUtils.isAppodealEnabled)
                put(
                    { getFAdLoaderInterstitialDeal(context, onUpdate) },
                    AdsUtils.remoteConfigs.getDouble("appodeal_fullscreen_percent")
                )
        }()
    }

fun showRandomInterstitial(
    context: AppCompatActivity,
    probability: Float = PrefsUtil.fullAdsProbability
) {
    if (PrefsUtil.isAdsEnabled && PrefsUtil.isFullAdsEnabled && probability > 0) {
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
            if (AdsUtils.isAppodealEnabled)
                put(
                    { FAdLoaderInterstitialLazyDeal(context).show() },
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