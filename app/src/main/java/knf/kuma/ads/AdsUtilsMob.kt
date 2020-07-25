package knf.kuma.ads

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdCallback
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.commons.*
import knf.kuma.custom.BannerContainerView
import knf.kuma.news.AdNewsObject
import knf.kuma.news.NewsObject
import knf.kuma.pojos.Achievement
import knf.kuma.pojos.AchievementAd
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.RecentObject
import kotlinx.android.synthetic.main.admob_ad.view.*
import xdroid.toaster.Toaster.toast

object AdsUtilsMob {
    val RECENT_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/7671520004"
    val RECENT_BANNER2 get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/9263803277"
    val FAVORITE_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/3484862982"
    val FAVORITE_BANNER2 get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/5919454637"
    val DIRECTORY_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/3243144237"
    val HOME_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/5095281956"
    val HOME_BANNER2 get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/9925964473"
    val EMISSION_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/8320211094"
    val SEEING_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/2035387232"
    val RECOMMEND_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/3304715801"
    val QUEUE_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/6258182200"
    val RECORD_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/9869042584"
    val RANDOM_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/5034969561"
    val NEWS_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/1562628412"
    val INFO_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/5488026017"
    val ACHIEVEMENT_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/4233626428"
    val EXPLORER_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/1041869769"
    val CAST_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5390653757953587/5535283585"
    val LIST_NATIVE get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1044960115" else "ca-app-pub-5390653757953587/5447863415"
    const val REWARDED = "ca-app-pub-5390653757953587/5420761189"
    const val INTERSTITIAL = "ca-app-pub-5390653757953587/5880297311"
    val adRequest: AdRequest
        get() = AdRequest.Builder().apply {
            if (BuildConfig.DEBUG) {
                addTestDevice("13973677C2742599D2DE55DC380C9A99")
                addTestDevice("C0D57A3687415A6ED689296FCD543DB9")
            }
        }.build()
}

fun MutableList<RecentObject>.implAdsRecentMob() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    ArrayList(this).forEachIndexed { index, _ ->
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
}

fun MutableList<FavoriteObject>.implAdsFavoriteMob() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    ArrayList(this).apply {
        forEachIndexed { index, _ ->
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
                this@implAdsFavoriteMob.add(index, AdFavoriteObject(adID))
            }
        }
    }
}

fun MutableList<NewsObject>.implAdsNewsMob() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    ArrayList(this).forEachIndexed { index, _ ->
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
}

fun MutableList<Achievement>.implAdsAchievementMob() {
    if (!PrefsUtil.isAdsEnabled || isEmpty()) return
    var adIndex = 0
    ArrayList(this).forEachIndexed { index, _ ->
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
}

fun ViewGroup.implBannerCastMob() {
    this.implBannerMob(AdsUtilsMob.CAST_BANNER)
}

fun ViewGroup.implBannerMob(unitID: AdsType, isSmart: Boolean = false) {
    val id = when (unitID) {
        AdsType.RECENT_BANNER -> AdsUtilsMob.RECENT_BANNER
        AdsType.RECENT_BANNER2 -> AdsUtilsMob.RECENT_BANNER2
        AdsType.FAVORITE_BANNER -> AdsUtilsMob.FAVORITE_BANNER
        AdsType.FAVORITE_BANNER2 -> AdsUtilsMob.FAVORITE_BANNER2
        AdsType.DIRECTORY_BANNER -> AdsUtilsMob.DIRECTORY_BANNER
        AdsType.HOME_BANNER -> AdsUtilsMob.HOME_BANNER
        AdsType.HOME_BANNER2 -> AdsUtilsMob.HOME_BANNER2
        AdsType.EMISSION_BANNER -> AdsUtilsMob.EMISSION_BANNER
        AdsType.SEEING_BANNER -> AdsUtilsMob.SEEING_BANNER
        AdsType.RECOMMEND_BANNER -> AdsUtilsMob.RECOMMEND_BANNER
        AdsType.QUEUE_BANNER -> AdsUtilsMob.QUEUE_BANNER
        AdsType.RECORD_BANNER -> AdsUtilsMob.RECORD_BANNER
        AdsType.RANDOM_BANNER -> AdsUtilsMob.RANDOM_BANNER
        AdsType.NEWS_BANNER -> AdsUtilsMob.NEWS_BANNER
        AdsType.INFO_BANNER -> AdsUtilsMob.INFO_BANNER
        AdsType.ACHIEVEMENT_BANNER -> AdsUtilsMob.ACHIEVEMENT_BANNER
        AdsType.EXPLORER_BANNER -> AdsUtilsMob.EXPLORER_BANNER
        AdsType.CAST_BANNER -> AdsUtilsMob.CAST_BANNER
        AdsType.REWARDED -> AdsUtilsMob.REWARDED
        AdsType.INTERSTITIAL -> AdsUtilsMob.INTERSTITIAL
    }
    implBannerMob(id, isSmart)
}

fun ViewGroup.implBannerMob(unitID: String, isSmart: Boolean = false) {
    if (PrefsUtil.isAdsEnabled) {
        if (this !is BannerContainerView) {
            AdLoader.Builder(context, AdsUtilsMob.LIST_NATIVE)
                    .forUnifiedNativeAd {
                        doOnUI {
                            Log.e("Admob", "Native load")
                            val adView = when (unitID) {
                                AdsUtilsMob.RECENT_BANNER, AdsUtilsMob.FAVORITE_BANNER -> {
                                    inflate(context, R.layout.admob_ad_card).apply {
                                        admobAd.setNativeAd(it)
                                    }
                                }
                                AdsUtilsMob.NEWS_BANNER -> {
                                    inflate(context, R.layout.admob_ad_news).apply {
                                        admobAd.setNativeAd(it)
                                    }
                                }
                                AdsUtilsMob.ACHIEVEMENT_BANNER -> {
                                    inflate(context, R.layout.admob_ad_plain).apply {
                                        admobAd.setNativeAd(it)
                                    }
                                }
                                AdsUtilsMob.CAST_BANNER -> {
                                    inflate(context, R.layout.admob_ad_alone).apply {
                                        admobAd.setNativeAd(it)
                                    }
                                }
                                else -> return@doOnUI
                            }
                            if (this is BannerContainerView) {
                                show(adView)
                            } else {
                                removeAllViews()
                                addView(adView)
                            }
                        }
                    }.withAdListener(object : AbsAdListener() {
                        override fun onAdFailedToLoad(p0: Int) {
                            doOnUI {
                                Log.e("Admob", "Native failed: $p0")
                                val adView = AdView(context)
                                adView.adSize = getAdSize(width.toFloat())
                                adView.adUnitId = unitID
                                adView.adListener = object : AbsAdListener() {
                                    override fun onAdClicked() {
                                        FirebaseAnalytics.getInstance(App.context).logEvent("Ad_clicked", Bundle())
                                    }
                                }
                                if (this@implBannerMob is BannerContainerView) {
                                    show(adView)
                                } else {
                                    removeAllViews()
                                    addView(adView)
                                }
                                adView.loadAd(AdsUtilsMob.adRequest)
                            }
                        }
                    }).build().loadAd(AdsUtilsMob.adRequest)
        } else
            doOnUI {
                Log.e("Admob", "Banner load")
                val adView = AdView(context)
                adView.adSize = getAdSize(width.toFloat())
                adView.adUnitId = unitID
                adView.adListener = object : AbsAdListener() {
                    override fun onAdClicked() {
                        FirebaseAnalytics.getInstance(App.context).logEvent("Ad_clicked", Bundle())
                    }
                }
                show(adView)
                adView.loadAd(AdsUtilsMob.adRequest)
            }
    }
}

fun getFAdLoaderRewardedMob(context: Activity, onUpdate: () -> Unit): FullscreenAdLoader = FAdLoaderRewardedMob(context, onUpdate)
fun getFAdLoaderInterstitialMob(context: Activity, onUpdate: () -> Unit): FullscreenAdLoader = FAdLoaderInterstitialMob(context, onUpdate)

class FAdLoaderRewardedMob(val context: Activity, private val onUpdate: () -> Unit) : FullscreenAdLoader {

    private lateinit var rewardedAd: RewardedAd

    private fun createAndLoadRewardAd() {
        rewardedAd = RewardedAd(context, AdsUtilsMob.REWARDED)
        rewardedAd.loadAd(AdsUtilsMob.adRequest, object : RewardedAdLoadCallback() {

            override fun onRewardedAdFailedToLoad(code: Int) {
                Log.e("Ad", "Ad failed to load, code: $code")
                createAndLoadRewardAd()
            }
        })
    }

    private fun showRewarded() {
        rewardedAd.show(context, object : RewardedAdCallback() {

            override fun onUserEarnedReward(item: RewardItem) {
                FirebaseAnalytics.getInstance(App.context).logEvent("Rewarded_Ad_watched", Bundle())
                Economy.reward()
                onUpdate()
            }

            override fun onRewardedAdClosed() {
                createAndLoadRewardAd()
            }
        })
    }

    override fun load() {
        if (!::rewardedAd.isInitialized || !rewardedAd.isLoaded) createAndLoadRewardAd()
    }

    override fun show() {
        if (::rewardedAd.isInitialized && rewardedAd.isLoaded)
            showRewarded()
        else if (Network.isAdsBlocked)
            toast("Anuncios bloqueados por host")
        else
            toast("Anuncio aún cargando...")
    }
}

class FAdLoaderInterstitialMob(val context: Activity, private val onUpdate: () -> Unit) : FullscreenAdLoader {
    var isAdClicked = false
    private val interstitialAd: InterstitialAd by lazy {
        InterstitialAd(context).apply {
            adUnitId = AdsUtilsMob.INTERSTITIAL
            adListener = object : AdListener() {
                override fun onAdClosed() {
                    FirebaseAnalytics.getInstance(App.context).logEvent("Interstitial_Ad_watched", Bundle())
                    interstitialAd.loadAd(AdsUtilsMob.adRequest)
                    Economy.reward(isAdClicked)
                    onUpdate()
                }

                override fun onAdClicked() {
                    isAdClicked = true
                    FirebaseAnalytics.getInstance(App.context).logEvent("Interstitial_Ad_clicked", Bundle())
                }
            }
        }
    }

    override fun load() {
        if (!interstitialAd.isLoaded) interstitialAd.loadAd(AdsUtilsMob.adRequest)
    }

    override fun show() {
        when {
            interstitialAd.isLoaded -> interstitialAd.show()
            Network.isAdsBlocked -> toast("Anuncios bloqueados por host")
            else -> toast("Anuncio aún cargando...")
        }
    }
}

abstract class AbsAdListener : AdListener()