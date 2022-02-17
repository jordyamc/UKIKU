package knf.kuma.ads

//import com.appbrain.*
import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUI
import knf.kuma.news.AdNewsObject
import knf.kuma.news.NewsObject
import knf.kuma.pojos.Achievement
import knf.kuma.pojos.AchievementAd
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.RecentObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xdroid.toaster.Toaster

object AdsUtilsBrains {
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

fun MutableList<RecentObject>.implAdsRecentBrains() {
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

fun MutableList<FavoriteObject>.implAdsFavoriteBrains() {
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
                this@implAdsFavoriteBrains.add(index, AdFavoriteObject(adID))
            }
        }
    }
}

fun MutableList<NewsObject>.implAdsNewsBrain() {
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

fun MutableList<Achievement>.implAdsAchievementBrain() {
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

fun ViewGroup.implBannerCastBrains() {
    this.implBannerBrains(AdsUtilsBrains.CAST_BANNER)
}

fun ViewGroup.implBannerBrains(unitID: AdsType, isSmart: Boolean = false) {
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
    implBannerBrains(id, isSmart)
}

fun ViewGroup.implBannerBrains(unitID: String, isSmart: Boolean = false) {
    if (PrefsUtil.isAdsEnabled)
        doOnUI {
            /*val adView = AppBrainBanner(context)
            adView.bannerListener = object : BannerListener {
                override fun onClick() {
                    FirebaseAnalytics.getInstance(App.context).logEvent("Ad_clicked", Bundle())
                }

                override fun onAdRequestDone(p0: Boolean) {
                }
            }
            if (this is BannerContainerView) {
                show(adView)
            } else {
                removeAllViews()
                addView(adView)
            }*/
        }
}

fun getFAdLoaderBrains(context: Context, onUpdate: () -> Unit): FullscreenAdLoader = FAdLoaderBrains(context, onUpdate)

class FAdLoaderBrains(private val context: Context, onUpdate: () -> Unit) : FullscreenAdLoader {
    var isAdClicked = false
    /*private val builder: InterstitialBuilder by lazy {
        InterstitialBuilder.create().apply {
            adId = AdId.DEFAULT
            setOnDoneCallback { builder.preload(context) }
            listener = object : InterstitialListener {
                override fun onClick() {
                    FirebaseAnalytics.getInstance(App.context).logEvent("Interstitial_Ad_clicked", Bundle())
                    isAdClicked = true
                }

                override fun onDismissed(p0: Boolean) {
                    FirebaseAnalytics.getInstance(App.context).logEvent("Interstitial_Ad_watched", Bundle())
                    Economy.reward(isAdClicked)
                    onUpdate()
                }

                override fun onAdFailedToLoad(p0: InterstitialListener.InterstitialError?) {
                }

                override fun onPresented() {
                }

                override fun onAdLoaded() {
                }
            }
        }.also { it.preload(context) }
    }*/

    override fun load() {
        ///builder.preload(context)
    }

    override fun show() {
        if (Network.isAdsBlocked)
            Toaster.toast("Anuncios bloqueados por host")
        /*else
            builder.show(context)*/
    }
}

class FAdLoaderInterstitialLazyBrains(val context: AppCompatActivity) : FullscreenAdLoader {
    var isAdClicked = false

    /*private val builder: InterstitialBuilder by lazy {
        InterstitialBuilder.create().apply {
            adId = AdId.DEFAULT
            setOnDoneCallback { builder.preload(context) }
            listener = object : InterstitialListener {
                override fun onClick() {
                    FirebaseAnalytics.getInstance(App.context).logEvent("Interstitial_Ad_clicked", Bundle())
                    isAdClicked = true
                }

                override fun onDismissed(p0: Boolean) {
                    FirebaseAnalytics.getInstance(App.context).logEvent("Interstitial_Ad_watched", Bundle())
                    Economy.reward(isAdClicked)
                }

                override fun onAdFailedToLoad(p0: InterstitialListener.InterstitialError?) {
                }

                override fun onPresented() {
                }

                override fun onAdLoaded() {
                }
            }
        }
    }*/

    init {
        load()
    }

    override fun load() {
        //builder.preload(context)
    }

    override fun show() {
        if (Network.isAdsBlocked)
            Toaster.toast("Anuncios bloqueados por host")
        else
            context.lifecycleScope.launch(Dispatchers.Main) {
                var tryCount = 11
                /*while (!builder.show(context)) {
                    delay(250)
                    tryCount--
                }*/
            }
    }
}