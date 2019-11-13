package knf.kuma.ads

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.startapp.android.publish.ads.banner.Banner
import com.startapp.android.publish.ads.banner.BannerListener
import com.startapp.android.publish.adsCommon.Ad
import com.startapp.android.publish.adsCommon.StartAppAd
import com.startapp.android.publish.adsCommon.adListeners.AdDisplayListener
import knf.kuma.commons.Economy
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUI
import knf.kuma.custom.BannerContainerView
import knf.kuma.news.AdNewsObject
import knf.kuma.news.NewsObject
import knf.kuma.pojos.Achievement
import knf.kuma.pojos.AchievementAd
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.RecentObject

object AdsUtilsStartApp {
    const val RECENT_BANNER = "recent_banner"
    const val RECENT_BANNER2 = "recent_banner_2"
    const val FAVORITE_BANNER = "favorite_banner"
    const val FAVORITE_BANNER2 = "favorite_banner_2"
    const val DIRECTORY_BANNER = "directory_banner"
    const val HOME_BANNER = "home_banner"
    const val HOME_BANNER2 = "home_banner_2"
    const val EMISSION_BANNER = "emission_banner"
    const val SEEING_BANNER = "seeing_banner"
    const val RECOMMEND_BANNER = "recommend_banner"
    const val QUEUE_BANNER = "queue_banner"
    const val RECORD_BANNER = "record_banner"
    const val RANDOM_BANNER = "random_banner"
    const val NEWS_BANNER = "news_banner"
    const val INFO_BANNER = "info_banner"
    const val ACHIEVEMENT_BANNER = "achievement_banner"
    const val EXPLORER_BANNER = "explorer_banner"
    const val CAST_BANNER = "cast_banner"
    const val REWARDED = "rewarded"
    const val INTERSTITIAL = "interstitial"
}

fun MutableList<RecentObject>.implAdsRecentStartApp() {
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

fun MutableList<FavoriteObject>.implAdsFavoriteStartApp() {
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
                this@implAdsFavoriteStartApp.add(index, AdFavoriteObject(adID))
            }
        }
    }
}

fun MutableList<NewsObject>.implAdsNewsStartApp() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    ArrayList(this).forEachIndexed { index, _ ->
        if (index % 5 == 0 && index > 0) {
            val adID: String = when (adIndex) {
                0 -> {
                    adIndex = 1
                    AdsUtilsBrains.NEWS_BANNER
                }
                else -> {
                    adIndex = 0
                    AdsUtilsBrains.NEWS_BANNER
                }
            }
            add(index, AdNewsObject(adID))
        }
    }
}

fun MutableList<Achievement>.implAdsAchievementStartApp() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    ArrayList(this).forEachIndexed { index, _ ->
        if (index % 8 == 0 && index > 0) {
            val adID: String = when (adIndex) {
                0 -> {
                    adIndex = 1
                    AdsUtilsBrains.ACHIEVEMENT_BANNER
                }
                else -> {
                    adIndex = 0
                    AdsUtilsBrains.ACHIEVEMENT_BANNER
                }
            }
            add(index, AchievementAd(adID))
        }
    }
}

fun ViewGroup.implBannerCastStartApp() {
    this.implBannerStartApp(AdsUtilsBrains.CAST_BANNER)
}

fun ViewGroup.implBannerStartApp(unitID: AdsType, isSmart: Boolean = false) {
    val id = when (unitID) {
        AdsType.RECENT_BANNER -> AdsUtilsBrains.RECENT_BANNER
        AdsType.RECENT_BANNER2 -> AdsUtilsBrains.RECENT_BANNER2
        AdsType.FAVORITE_BANNER -> AdsUtilsBrains.FAVORITE_BANNER
        AdsType.FAVORITE_BANNER2 -> AdsUtilsBrains.FAVORITE_BANNER2
        AdsType.DIRECTORY_BANNER -> AdsUtilsBrains.DIRECTORY_BANNER
        AdsType.HOME_BANNER -> AdsUtilsBrains.HOME_BANNER
        AdsType.HOME_BANNER2 -> AdsUtilsBrains.HOME_BANNER2
        AdsType.EMISSION_BANNER -> AdsUtilsBrains.EMISSION_BANNER
        AdsType.SEEING_BANNER -> AdsUtilsBrains.SEEING_BANNER
        AdsType.RECOMMEND_BANNER -> AdsUtilsBrains.RECOMMEND_BANNER
        AdsType.QUEUE_BANNER -> AdsUtilsBrains.QUEUE_BANNER
        AdsType.RECORD_BANNER -> AdsUtilsBrains.RECORD_BANNER
        AdsType.RANDOM_BANNER -> AdsUtilsBrains.RANDOM_BANNER
        AdsType.NEWS_BANNER -> AdsUtilsBrains.NEWS_BANNER
        AdsType.INFO_BANNER -> AdsUtilsBrains.INFO_BANNER
        AdsType.ACHIEVEMENT_BANNER -> AdsUtilsBrains.ACHIEVEMENT_BANNER
        AdsType.EXPLORER_BANNER -> AdsUtilsBrains.EXPLORER_BANNER
        AdsType.CAST_BANNER -> AdsUtilsBrains.CAST_BANNER
        AdsType.REWARDED -> AdsUtilsBrains.REWARDED
        AdsType.INTERSTITIAL -> AdsUtilsBrains.INTERSTITIAL
    }
    implBannerStartApp(id, isSmart)
}

fun ViewGroup.implBannerStartApp(unitID: String, isSmart: Boolean = false) {
    if (PrefsUtil.isAdsEnabled)
        doOnUI {
            val adView = Banner(context, object : BannerListener {
                override fun onClick(p0: View?) {
                    Answers.getInstance().logCustom(CustomEvent("Ad clicked"))
                }

                override fun onFailedToReceiveAd(p0: View?) {
                }

                override fun onReceiveAd(p0: View?) {
                }
            })
            if (this is BannerContainerView) {
                show(adView)
            } else {
                removeAllViews()
                addView(adView)
            }
            adView.loadAd()
            adView.showBanner()
        }
}

fun getFAdLoaderRewardedStartApp(context: Activity, onUpdate: () -> Unit): FullscreenAdLoader = FAdLoaderRewardedStartApp(context, onUpdate)
fun getFAdLoaderInterstitialStartApp(context: Context, onUpdate: () -> Unit): FullscreenAdLoader = FAdLoaderInterstitialStartApp(context, onUpdate)

class FAdLoaderRewardedStartApp(private val context: Context, val onUpdate: () -> Unit) : FullscreenAdLoader {
    var isAdClicked = false
    private val startAppAd: StartAppAd by lazy {
        StartAppAd(context).apply {
            setVideoListener {
                Answers.getInstance().logCustom(CustomEvent("Rewarded Ad watched"))
                this@FAdLoaderRewardedStartApp.load()
                Economy.reward(isAdClicked)
                onUpdate()
            }
        }
    }

    override fun load() {
        startAppAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO)
    }

    override fun show() {
        startAppAd.showAd(object : AdDisplayListener {
            override fun adHidden(p0: Ad?) {

            }

            override fun adDisplayed(p0: Ad?) {

            }

            override fun adNotDisplayed(p0: Ad?) {
            }

            override fun adClicked(p0: Ad?) {
                isAdClicked = true
            }
        })
    }
}

class FAdLoaderInterstitialStartApp(private val context: Context, val onUpdate: () -> Unit) : FullscreenAdLoader {
    var isAdClicked = false
    private val startAppAd: StartAppAd by lazy { StartAppAd(context) }

    override fun load() {
        startAppAd.loadAd()
    }

    override fun show() {
        startAppAd.showAd(object : AdDisplayListener {
            override fun adHidden(p0: Ad?) {

            }

            override fun adDisplayed(p0: Ad?) {
                Answers.getInstance().logCustom(CustomEvent("Interstitial Ad watched"))
                load()
                Economy.reward(isAdClicked)
                onUpdate()
            }

            override fun adNotDisplayed(p0: Ad?) {
            }

            override fun adClicked(p0: Ad?) {
                isAdClicked = true
            }
        })
    }
}