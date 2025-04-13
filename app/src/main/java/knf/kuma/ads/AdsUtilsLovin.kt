package knf.kuma.ads

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinPrivacySettings
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.google.firebase.analytics.FirebaseAnalytics
import knf.kuma.App
import knf.kuma.R
import knf.kuma.commons.Economy
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.asPx
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

object AdsUtilsLovin {
    fun setUp(context: Activity, callback: () -> Unit) {
        val initConfig = AppLovinSdkInitializationConfiguration.builder("QHQI9Sl_Fltmz6OzT9WBg6sTUG3SlJOaLf6E7G4xMGsOake13NQHoHFK6dAUnG0u_18dllB1Q7mGheTwmEl8AD", context)
            .setMediationProvider(AppLovinMediationProvider.MAX)
            .build()
        AppLovinSdk.getInstance(context).initialize(initConfig) {
            if (!AppLovinPrivacySettings.hasUserConsent(context)) {
                AppLovinPrivacySettings.setHasUserConsent(true, context)
                AppLovinPrivacySettings.setDoNotSell(false, context)
            }
            callback()
        }
    }
}

fun MutableList<RecentObject>.implAdsRecentLovin() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    forEachReversedWithIndex { index, _ ->
        if (index % 5 == 0 && index > 0) {
            val adID: String = when (adIndex) {
                0 -> {
                    adIndex = 1
                    AdsUtilsMob.RECENT_BANNER
                }

                else -> {
                    adIndex = 0
                    AdsUtilsMob.RECENT_BANNER
                }
            }
            add(index, AdRecentObject(adID))
        }
    }
    add(0, AdRecentObject(AdsUtilsMob.RECENT_BANNER))
}

fun MutableList<FavoriteObject>.implAdsFavoriteLovin() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    forEachReversedWithIndex { index, _ ->
        if (index % 8 == 0 && index > 0 && !this[index - 1].isSection) {
            val adID: String = when (adIndex) {
                0 -> {
                    adIndex = 1
                    AdsUtilsMob.FAVORITE_BANNER
                }

                else -> {
                    adIndex = 0
                    AdsUtilsMob.FAVORITE_BANNER
                }
            }
            this@implAdsFavoriteLovin.add(index, AdFavoriteObject(adID))
        }
    }
    this@implAdsFavoriteLovin.add(0, AdFavoriteObject(AdsUtilsMob.FAVORITE_BANNER))
}

fun MutableList<NewsObject>.implAdsNewsLovin() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    forEachReversedWithIndex { index, _ ->
        if (index % 5 == 0 && index > 0) {
            val adID: String = when (adIndex) {
                0 -> {
                    adIndex = 1
                    AdsUtilsMob.NEWS_BANNER
                }

                else -> {
                    adIndex = 0
                    AdsUtilsMob.NEWS_BANNER
                }
            }
            add(index, AdNewsObject(adID))
        }
    }
    add(0, AdNewsObject(AdsUtilsMob.NEWS_BANNER))
}

fun MutableList<Achievement>.implAdsAchievementLovin() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    forEachReversedWithIndex { index, _ ->
        if (index % 8 == 0 && index > 0) {
            val adID: String = when (adIndex) {
                0 -> {
                    adIndex = 1
                    AdsUtilsMob.ACHIEVEMENT_BANNER
                }

                else -> {
                    adIndex = 0
                    AdsUtilsMob.ACHIEVEMENT_BANNER
                }
            }
            add(index, AchievementAd(adID))
        }
    }
    add(0, AchievementAd(AdsUtilsMob.ACHIEVEMENT_BANNER))
}

fun ViewGroup.implBannerCastLovin() {
    this.implBannerMob(AdsUtilsMob.CAST_BANNER)
}

fun ViewGroup.implBannerLovin(unitID: AdsType, isSmart: Boolean = false) {
    implBannerLovin()
}

fun ViewGroup.implBannerLovin() {
    if (PrefsUtil.isAdsEnabled) {
        GlobalScope.launch g@{
            if (this@implBannerLovin.tag == "AdView added")
                return@g
            if (this@implBannerLovin !is BannerContainerView) {
                GlobalScope.launch {
                    val adView = MaxAdView("91d782c7eb7efc75", App.context)
                    adView.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 50.asPx)
                    adView.setBackgroundColor(ContextCompat.getColor(App.context, R.color.cardview_background))
                    launch(Dispatchers.Main) {
                        addView(adView)
                        this@implBannerLovin.tag = "AdView added"
                        adView.loadAd()
                    }
                }
            } else {
                val adView = MaxAdView("91d782c7eb7efc75", App.context)
                adView.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 50.asPx)
                adView.setBackgroundColor(ContextCompat.getColor(App.context, R.color.cardview_background))
                launch(Dispatchers.Main) {
                    show(adView)
                    adView.loadAd()
                    this@implBannerLovin.tag = "AdView added"
                }
            }
        }
    }
}

fun getFAdLoaderInterstitialLovin(context: Activity, onUpdate: () -> Unit): FullscreenAdLoader = FAdLoaderInterstitialLovin(context, onUpdate)
fun getFAdLoaderRewardedLovin(context: Activity, onUpdate: () -> Unit): FullscreenAdLoader = FAdLoaderRewardedLovin(context, onUpdate)

class FAdLoaderInterstitialLovin(val context: Activity, private val onUpdate: () -> Unit) : FullscreenAdLoader {
    private var interstitialAd: MaxInterstitialAd = MaxInterstitialAd("e5f776a3ccb9282e", context)
    private var isLoading = false

    init {
        isLoading = true
        interstitialAd.setListener(object : MaxAdListener {
            var isClicked = false
            override fun onAdLoaded(p0: MaxAd) {
                isLoading = false
            }

            override fun onAdLoadFailed(p0: String, p1: MaxError) {
                GlobalScope.launch {
                    delay(2000)
                    interstitialAd.loadAd()
                }
            }

            override fun onAdHidden(p0: MaxAd) {
                isLoading = true
                interstitialAd.loadAd()
                FirebaseAnalytics.getInstance(App.context).logEvent("Interstitial_Ad_watched", Bundle())
                Economy.reward(isClicked)
                isClicked = false
                onUpdate()
            }

            override fun onAdDisplayed(p0: MaxAd) {}
            override fun onAdClicked(p0: MaxAd) {
                isClicked = true
            }

            override fun onAdDisplayFailed(p0: MaxAd, p1: MaxError) {}
        })
        interstitialAd.loadAd()
    }

    override fun load() {
        if (!isLoading && !interstitialAd.isReady) {
            isLoading = true
            interstitialAd.loadAd()
        }
    }

    override fun show() {
        when {
            !AdsUtils.isRemoteAdsEnabled || !AdsUtils.isRemoteFullEnabled -> return
            interstitialAd.isReady -> {
                interstitialAd.showAd()
            }

            Network.isAdsBlocked -> toast("Anuncios bloqueados por host")
            else -> toast("Anuncio aún cargando...")
        }
    }
}

class FAdLoaderRewardedLovin(val context: Activity, private val onUpdate: () -> Unit) : FullscreenAdLoader {
    private var rewardedAd: MaxRewardedAd = MaxRewardedAd.getInstance("e3b2506478ae074c", context)
    private var isLoading = false

    init {
        isLoading = true
        rewardedAd.setListener(object : MaxRewardedAdListener {
            var isClicked = false
            override fun onAdLoaded(p0: MaxAd) {
                isLoading = false
            }

            override fun onAdDisplayed(p0: MaxAd) {}
            override fun onAdHidden(p0: MaxAd) {
                isClicked = false
                isLoading = true
                rewardedAd.loadAd()
                onUpdate()
            }

            override fun onAdClicked(p0: MaxAd) {
                isClicked = true
            }

            override fun onAdLoadFailed(p0: String, p1: MaxError) {}
            override fun onAdDisplayFailed(p0: MaxAd, p1: MaxError) {}
            override fun onUserRewarded(p0: MaxAd, p1: MaxReward) {
                FirebaseAnalytics.getInstance(App.context).logEvent("Rewarded_Ad_watched", Bundle())
                Economy.reward(isClicked)
            }
        })
        rewardedAd.loadAd()
    }

    override fun load() {
        if (!isLoading && !rewardedAd.isReady) {
            isLoading = true
            rewardedAd.loadAd()
        }
    }

    override fun show() {
        when {
            !AdsUtils.isRemoteAdsEnabled || !AdsUtils.isRemoteFullEnabled -> return
            rewardedAd.isReady -> {
                rewardedAd.showAd()
            }

            Network.isAdsBlocked -> toast("Anuncios bloqueados por host")
            else -> toast("Anuncio aún cargando...")
        }
    }
}

class FAdLoaderInterstitialLazyLovin(val context: AppCompatActivity) : FullscreenAdLoader {
    private var interstitialAd: MaxInterstitialAd = MaxInterstitialAd("e5f776a3ccb9282e", context)
    private var isLoading = false

    init {
        isLoading = true
        interstitialAd.setListener(object : MaxAdListener {
            var isClicked = false
            override fun onAdLoaded(p0: MaxAd) {
                isLoading = false
            }

            override fun onAdLoadFailed(p0: String, p1: MaxError) {
                GlobalScope.launch {
                    delay(2000)
                    interstitialAd.loadAd()
                }
            }

            override fun onAdHidden(p0: MaxAd) {
                isLoading = true
                interstitialAd.loadAd()
                FirebaseAnalytics.getInstance(App.context).logEvent("Interstitial_Ad_watched", Bundle())
                Economy.reward(isClicked)
                isClicked = false
            }

            override fun onAdDisplayed(p0: MaxAd) {}
            override fun onAdClicked(p0: MaxAd) {
                isClicked = true
            }

            override fun onAdDisplayFailed(p0: MaxAd, p1: MaxError) {}
        })
        interstitialAd.loadAd()
    }

    override fun load() {
        if (!isLoading && !interstitialAd.isReady) {
            isLoading = true
            interstitialAd.loadAd()
        }
    }

    override fun show() {
        when {
            !AdsUtils.isRemoteAdsEnabled || !AdsUtils.isRemoteFullEnabled -> return
            interstitialAd.isReady -> {
                interstitialAd.showAd()
            }

            Network.isAdsBlocked -> toast("Anuncios bloqueados por host")
            else -> context.lifecycleScope.launch(Dispatchers.Main) {
                var tryCount = 11
                while (!interstitialAd.isReady && tryCount > 0) {
                    delay(250)
                    tryCount--
                }
                if (interstitialAd.isReady)
                    show()
            }
        }
    }
}