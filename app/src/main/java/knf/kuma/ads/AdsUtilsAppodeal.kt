package knf.kuma.ads

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.appodeal.ads.Appodeal
import com.appodeal.ads.InterstitialCallbacks
import com.appodeal.ads.RewardedVideoCallbacks
import com.appodeal.ads.native_ad.views.NativeAdViewNewsFeed
import com.google.firebase.analytics.FirebaseAnalytics
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.commons.Economy
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.findActivity
import knf.kuma.custom.BannerContainerView
import knf.kuma.news.AdNewsObject
import knf.kuma.news.NewsObject
import knf.kuma.pojos.Achievement
import knf.kuma.pojos.AchievementAd
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.RecentObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.collections.forEachReversedWithIndex
import xdroid.toaster.Toaster.toast

object AdsUtilsAppodeal {

    fun setUp() {
        if (!BuildConfig.DEBUG) return
        //Appodeal.setTesting(true)
    }
}

fun MutableList<RecentObject>.implAdsRecentDeal() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    forEachReversedWithIndex { index, _ ->
        if (index % 5 == 0 && index > 0) {
            add(index, AdRecentObject("appodeal"))
        }
    }
    add(0, AdRecentObject("appodeal"))
}

fun MutableList<FavoriteObject>.implAdsFavoriteDeal() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    forEachReversedWithIndex { index, _ ->
        if (index % 8 == 0 && index > 0 && !this[index - 1].isSection) {
            this@implAdsFavoriteDeal.add(index, AdFavoriteObject("appodeal"))
        }
    }
    this@implAdsFavoriteDeal.add(0, AdFavoriteObject("appodeal"))
}

fun MutableList<NewsObject>.implAdsNewsDeal() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    forEachReversedWithIndex { index, _ ->
        if (index % 5 == 0 && index > 0) {
            add(index, AdNewsObject("appodeal"))
        }
    }
    add(0, AdNewsObject("appodeal"))
}

fun MutableList<Achievement>.implAdsAchievementDeal() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    forEachReversedWithIndex { index, _ ->
        if (index % 8 == 0 && index > 0) {
            add(index, AchievementAd("appodeal"))
        }
    }
    add(0, AchievementAd("appodeal"))
}

fun ViewGroup.implBannerCastDeal() {
    this.implBannerDeal(AdsType.CAST_BANNER)
}

fun ViewGroup.implBannerDeal(adsType: AdsType) {
    if (PrefsUtil.isAdsEnabled) {
        GlobalScope.launch(Dispatchers.Main) g@{
            if (this@implBannerDeal.tag == "AdView added")
                return@g
            if (this@implBannerDeal !is BannerContainerView) {
                if (Appodeal.isLoaded(Appodeal.NATIVE) && Appodeal.getAvailableNativeAdsCount() > 0) {
                    addView(NativeAdViewNewsFeed(context, Appodeal.getNativeAds(1).first()))
                    this@implBannerDeal.tag = "AdView added"
                } else {
                    val adView = Appodeal.getBannerView(context)
                    addView(adView)
                    this@implBannerDeal.tag = "AdView added"
                    Appodeal.show(context.findActivity(), Appodeal.BANNER_VIEW)
                }
            } else {
                show(Appodeal.getBannerView(context))
                Appodeal.show(context.findActivity(), Appodeal.BANNER_VIEW)
                this@implBannerDeal.tag = "AdView added"
            }
        }
    }
}

fun getFAdLoaderRewardedDeal(context: Activity, onUpdate: () -> Unit): FullscreenAdLoader = FAdLoaderMultiDeal(context, true, onUpdate)
fun getFAdLoaderInterstitialDeal(context: Activity, onUpdate: () -> Unit): FullscreenAdLoader = FAdLoaderMultiDeal(context, false, onUpdate)

class FAdLoaderMultiDeal(val context: Activity, private val rewarded: Boolean, private val onUpdate: () -> Unit) : FullscreenAdLoader {

    override fun load() {
        Appodeal.cache(context, Appodeal.INTERSTITIAL)
        Appodeal.cache(context, Appodeal.REWARDED_VIDEO)
    }

    private fun showAd() {
        Appodeal.setRewardedVideoCallbacks(object : RewardedCallback() {
            override fun onRewardedVideoClosed(finished: Boolean) {
                FirebaseAnalytics.getInstance(App.context).logEvent("Rewarded_Ad_watched", Bundle())
                Economy.reward(baseReward = 2)
                onUpdate()
            }
        })
        Appodeal.setInterstitialCallbacks(object : InterstitialCallback() {
            override fun onInterstitialClosed() {
                FirebaseAnalytics.getInstance(App.context).logEvent("Interstitial_Ad_watched", Bundle())
                Economy.reward(false)
                onUpdate()
            }
        })
        if (rewarded) {
            if (Appodeal.isLoaded(Appodeal.REWARDED_VIDEO)) {
                Appodeal.show(context, Appodeal.REWARDED_VIDEO)
            } else {
                Appodeal.show(context, Appodeal.INTERSTITIAL)
            }
        } else {
            if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
                Appodeal.show(context, Appodeal.INTERSTITIAL)
            } else {
                Appodeal.show(context, Appodeal.REWARDED_VIDEO)
            }
        }
    }

    override fun show() {
        when {
            Appodeal.isLoaded(Appodeal.INTERSTITIAL) || Appodeal.isLoaded(Appodeal.REWARDED_VIDEO) -> showAd()
            Network.isAdsBlocked -> toast("Anuncios bloqueados por host")
            else -> toast("Anuncio aún cargando...")
        }
    }
}

class FAdLoaderInterstitialLazyDeal(val context: AppCompatActivity) : FullscreenAdLoader {

    private fun showAd() {
        Appodeal.setRewardedVideoCallbacks(object : RewardedCallback() {
            override fun onRewardedVideoClosed(finished: Boolean) {
                FirebaseAnalytics.getInstance(App.context).logEvent("Rewarded_Ad_watched", Bundle())
                Economy.reward(baseReward = 2)
                load()
            }
        })
        Appodeal.setInterstitialCallbacks(object : InterstitialCallback() {
            override fun onInterstitialClosed() {
                FirebaseAnalytics.getInstance(App.context).logEvent("Interstitial_Ad_watched", Bundle())
                Economy.reward(false)
                load()
            }
        })
        if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
            Appodeal.show(context, Appodeal.INTERSTITIAL)
        } else {
            Appodeal.show(context, Appodeal.REWARDED_VIDEO)
        }
    }

    override fun load() {
        Appodeal.cache(context, Appodeal.INTERSTITIAL)
        Appodeal.cache(context, Appodeal.REWARDED_VIDEO)
    }

    override fun show() {
        when {
            Appodeal.isLoaded(Appodeal.INTERSTITIAL) || Appodeal.isLoaded(Appodeal.REWARDED_VIDEO) -> showAd()
            Network.isAdsBlocked -> toast("Anuncios bloqueados por host")
            else -> context.lifecycleScope.launch(Dispatchers.Main) {
                var tryCount = 11
                load()
                while (!Appodeal.isLoaded(Appodeal.INTERSTITIAL) && tryCount > 0) {
                    delay(250)
                    tryCount--
                }
                if (Appodeal.isLoaded(Appodeal.INTERSTITIAL))
                    show()
            }
        }
    }
}

abstract class RewardedCallback: RewardedVideoCallbacks {
    override fun onRewardedVideoClicked() {}
    override fun onRewardedVideoClosed(finished: Boolean) {}
    override fun onRewardedVideoExpired() {}
    override fun onRewardedVideoFailedToLoad() {}
    override fun onRewardedVideoFinished(amount: Double, currency: String?) {}
    override fun onRewardedVideoLoaded(isPrecache: Boolean) {}
    override fun onRewardedVideoShowFailed() {}
    override fun onRewardedVideoShown() {}
}

abstract class InterstitialCallback: InterstitialCallbacks {
    override fun onInterstitialClicked() {}
    override fun onInterstitialClosed() {}
    override fun onInterstitialExpired() {}
    override fun onInterstitialFailedToLoad() {}
    override fun onInterstitialLoaded(isPrecache: Boolean) {}
    override fun onInterstitialShowFailed() {}
    override fun onInterstitialShown() {}
}