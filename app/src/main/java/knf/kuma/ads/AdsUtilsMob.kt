package knf.kuma.ads

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdCallback
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import knf.kuma.BuildConfig
import knf.kuma.commons.Economy
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUI
import knf.kuma.news.AdNewsObject
import knf.kuma.news.NewsObject
import knf.kuma.pojos.Achievement
import knf.kuma.pojos.AchievementAd
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.RecentObject
import xdroid.toaster.Toaster.toast

object AdsUtilsMob {
    const val RECENT_BANNER = "ca-app-pub-5390653757953587/7671520004"
    const val RECENT_BANNER2 = "ca-app-pub-5390653757953587/9263803277"
    const val FAVORITE_BANNER = "ca-app-pub-5390653757953587/3484862982"
    const val FAVORITE_BANNER2 = "ca-app-pub-5390653757953587/5919454637"
    const val DIRECTORY_BANNER = "ca-app-pub-5390653757953587/3243144237"
    const val HOME_BANNER = "ca-app-pub-5390653757953587/5095281956"
    const val HOME_BANNER2 = "ca-app-pub-5390653757953587/9925964473"
    const val EMISSION_BANNER = "ca-app-pub-5390653757953587/8320211094"
    const val SEEING_BANNER = "ca-app-pub-5390653757953587/2035387232"
    const val RECOMMEND_BANNER = "ca-app-pub-5390653757953587/3304715801"
    const val QUEUE_BANNER = "ca-app-pub-5390653757953587/6258182200"
    const val RECORD_BANNER = "ca-app-pub-5390653757953587/9869042584"
    const val RANDOM_BANNER = "ca-app-pub-5390653757953587/5034969561"
    const val NEWS_BANNER = "ca-app-pub-5390653757953587/1562628412"
    const val INFO_BANNER = "ca-app-pub-5390653757953587/5488026017"
    const val ACHIEVEMENT_BANNER = "ca-app-pub-5390653757953587/4233626428"
    const val EXPLORER_BANNER = "ca-app-pub-5390653757953587/1041869769"
    const val CAST_BANNER = "ca-app-pub-5390653757953587/5535283585"
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
                    AdsUtilsMob.RECENT_BANNER2
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
                        AdsUtilsMob.FAVORITE_BANNER2
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
    if (PrefsUtil.isAdsEnabled)
        doOnUI {
            removeAllViews()
            val adView = AdView(context)
            adView.adSize = if (isSmart) AdSize.SMART_BANNER else AdSize.BANNER
            adView.adUnitId = unitID
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {

                }

                override fun onAdFailedToLoad(errorCode: Int) {

                }

                override fun onAdOpened() {

                }

                override fun onAdClicked() {
                    Answers.getInstance().logCustom(CustomEvent("Ad clicked"))
                }

                override fun onAdLeftApplication() {

                }

                override fun onAdClosed() {

                }
            }
            addView(adView)
            adView.loadAd(AdsUtilsMob.adRequest)
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
                Answers.getInstance().logCustom(CustomEvent("Rewarded Ad watched"))
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
                    Answers.getInstance().logCustom(CustomEvent("Interstitial Ad watched"))
                    interstitialAd.loadAd(AdsUtilsMob.adRequest)
                    Economy.reward(isAdClicked)
                    onUpdate()
                }

                override fun onAdClicked() {
                    isAdClicked = true
                    Answers.getInstance().logCustom(CustomEvent("Interstitial Ad clicked"))
                }
            }
        }
    }

    override fun load() {
        if (!interstitialAd.isLoaded) interstitialAd.loadAd(AdsUtilsMob.adRequest)
    }

    override fun show() {
        if (interstitialAd.isLoaded)
            interstitialAd.show()
        else
            toast("Anuncio aún cargando...")
    }
}