package knf.kuma.ads

/*import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback*/
import android.app.Activity
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import knf.kuma.BuildConfig
import knf.kuma.commons.PrefsUtil
import knf.kuma.news.AdNewsObject
import knf.kuma.news.NewsObject
import knf.kuma.pojos.Achievement
import knf.kuma.pojos.AchievementAd
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.RecentObject
import org.jetbrains.anko.collections.forEachReversedWithIndex

object AdsUtilsMob {
    val RECENT_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.RECENT_BANNER
    val RECENT_BANNER2 get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.RECENT_BANNER2
    val FAVORITE_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.FAVORITE_BANNER
    val FAVORITE_BANNER2 get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.FAVORITE_BANNER2
    val DIRECTORY_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.DIRECTORY_BANNER
    val HOME_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.HOME_BANNER
    val HOME_BANNER2 get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.HOME_BANNER2
    val EMISSION_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.EMISSION_BANNER
    val SEEING_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.SEEING_BANNER
    val RECOMMEND_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.RECOMMEND_BANNER
    val QUEUE_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.QUEUE_BANNER
    val RECORD_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.RECORD_BANNER
    val RANDOM_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.RANDOM_BANNER
    val NEWS_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.NEWS_BANNER
    val INFO_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.INFO_BANNER
    val ACHIEVEMENT_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.ACHIEVEMENT_BANNER
    val EXPLORER_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.EXPLORER_BANNER
    val CAST_BANNER get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else AdmobID.CAST_BANNER
    val LIST_NATIVE get() = if (BuildConfig.DEBUG) "ca-app-pub-5390653757953587/5447863415" else AdmobID.LIST_NATIVE
    val REWARDED get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917" else AdmobID.REWARDED
    val INTERSTITIAL get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712" else AdmobID.INTERSTITIAL

    //val adRequest: AdRequest get() = AdRequest.Builder().build()
    val ACHIEVEMENT_NATIVE = "achievement_native"

    fun setUp(context: Activity, callback: () -> Unit) {
        /*MobileAds.initialize(context) {
            NativeManager
            callback()
        }
        if (!BuildConfig.DEBUG) return
        val builder = RequestConfiguration.Builder().setTestDeviceIds(listOf("FE18DCEC5EE5755C3927E4EC30CD4F9D"))
        MobileAds.setRequestConfiguration(builder.build())*/
    }
}

fun MutableList<RecentObject>.implAdsRecentMob() {
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

fun MutableList<FavoriteObject>.implAdsFavoriteMob() {
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
            this@implAdsFavoriteMob.add(index, AdFavoriteObject(adID))
        }
    }
    this@implAdsFavoriteMob.add(0, AdFavoriteObject(AdsUtilsMob.FAVORITE_BANNER))
}

fun MutableList<NewsObject>.implAdsNewsMob() {
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

fun MutableList<Achievement>.implAdsAchievementMob() {
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
    /*if (PrefsUtil.isAdsEnabled) {
        GlobalScope.launch g@{
            if (this@implBannerMob.tag == "AdView added")
                return@g
            if (this@implBannerMob !is BannerContainerView) {
                NativeManager.take(GlobalScope, 1) {
                    if (it.isEmpty())
                        GlobalScope.launch {
                            val adView = AdView(context)
                            adView.setAdSize(getAdSize(width.toFloat()))
                            adView.adUnitId = unitID
                            adView.adListener = object : AbsAdListener() {
                                override fun onAdClicked() {
                                    FirebaseAnalytics.getInstance(App.context).logEvent("Ad_clicked", Bundle())
                                }
                            }
                            launch(Dispatchers.Main) {
                                addView(adView)
                                this@implBannerMob.tag = "AdView added"
                                adView.loadAd(AdsUtilsMob.adRequest)
                            }
                        }
                    else
                        GlobalScope.launch {
                            val adView = when (unitID) {
                                AdsUtilsMob.RECENT_BANNER, AdsUtilsMob.FAVORITE_BANNER -> {
                                    asyncInflate(context, R.layout.admob_ad_card).apply {
                                        Log.e("Ad", "On recent")
                                        find<AdTemplateView>(R.id.admobAd).setNativeAd(it[0])
                                    }
                                }
                                AdsUtilsMob.NEWS_BANNER -> {
                                    asyncInflate(context, R.layout.admob_ad_news).apply {
                                        Log.e("Ad", "On news")
                                        find<AdTemplateView>(R.id.admobAd).setNativeAd(it[0])
                                    }
                                }
                                AdsUtilsMob.ACHIEVEMENT_BANNER, AdsUtilsMob.ACHIEVEMENT_NATIVE -> {
                                    asyncInflate(context, R.layout.admob_ad_plain).apply {
                                        Log.e("Ad", "On Achievement")
                                        find<AdTemplateView>(R.id.admobAd).setNativeAd(it[0])
                                    }
                                }
                                AdsUtilsMob.CAST_BANNER -> {
                                    asyncInflate(context, R.layout.admob_ad_alone).apply {
                                        Log.e("Ad", "On Cast")
                                        find<AdTemplateView>(R.id.admobAd).setNativeAd(it[0])
                                    }
                                }
                                else -> return@launch
                            }
                            launch(Dispatchers.Main) {
                                addView(adView)
                                this@implBannerMob.tag = "AdView added"
                            }
                        }
                }
            } else {
                val adView = AdView(context)
                adView.setAdSize(getAdSize(width.toFloat()))
                adView.adUnitId = unitID
                adView.adListener = object : AbsAdListener() {
                    override fun onAdClicked() {
                        FirebaseAnalytics.getInstance(App.context).logEvent("Ad_clicked", Bundle())
                    }
                }
                launch(Dispatchers.Main) {
                    show(adView)
                    adView.loadAd(AdsUtilsMob.adRequest)
                    this@implBannerMob.tag = "AdView added"
                }
            }
        }
    }*/
}

fun getFAdLoaderRewardedMob(context: Activity, onUpdate: () -> Unit): FullscreenAdLoader = FAdLoaderRewardedMob(context, onUpdate)
fun getFAdLoaderInterstitialMob(context: Activity, onUpdate: () -> Unit): FullscreenAdLoader = FAdLoaderInterstitialMob(context, onUpdate)

class FAdLoaderRewardedMob(val context: Activity, private val onUpdate: () -> Unit) : FullscreenAdLoader {

    /*private var rewardedAd: RewardedAd? = null

    private fun createAndLoadRewardAd() {
        rewardedAd = null
        RewardedAd.load(context, AdsUtilsMob.REWARDED, AdsUtilsMob.adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(p0: RewardedAd) {
                rewardedAd = p0
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                Log.e("Ad", "Ad failed to load, code: ${p0.code}")
                GlobalScope.launch(Dispatchers.Main) {
                    delay(2000)
                    if (!context.isFinishing) {
                        createAndLoadRewardAd()
                    }
                }
            }
        })
    }*/

    private fun showRewarded() {
        /*rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                createAndLoadRewardAd()
            }
        }
        rewardedAd?.show(context) {
            FirebaseAnalytics.getInstance(App.context).logEvent("Rewarded_Ad_watched", Bundle())
            Economy.reward(baseReward = 2)
            onUpdate()
        }*/
    }

    override fun load() {
        //if (rewardedAd == null) createAndLoadRewardAd()
    }

    override fun show() {
        /*when {
            !AdsUtils.isRemoteAdsEnabled || !AdsUtils.isRemoteFullEnabled -> return
            rewardedAd != null -> showRewarded()
            Network.isAdsBlocked -> toast("Anuncios bloqueados por host")
            else -> toast("Anuncio aún cargando...")
        }*/
    }
}

class FAdLoaderInterstitialMob(val context: Activity, private val onUpdate: () -> Unit) : FullscreenAdLoader {
    /*private var interstitialAd: InterstitialAd? = null

    private fun createAndLoad() {
        interstitialAd = null
        InterstitialAd.load(context, AdsUtilsMob.INTERSTITIAL, AdsUtilsMob.adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(p0: InterstitialAd) {
                interstitialAd = p0
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                Log.e("Ad", "Ad failed to load, code: ${p0.code}")
                GlobalScope.launch(Dispatchers.Main) {
                    delay(2000)
                    if (!context.isFinishing) {
                        createAndLoad()
                    }
                }
            }
        })
    }*/

    override fun load() {
        //if (interstitialAd == null) createAndLoad()
    }

    override fun show() {
        /* when {
             !AdsUtils.isRemoteAdsEnabled || !AdsUtils.isRemoteFullEnabled -> return
             interstitialAd != null -> {
                 interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                     override fun onAdDismissedFullScreenContent() {
                         FirebaseAnalytics.getInstance(App.context).logEvent("Interstitial_Ad_watched", Bundle())
                         createAndLoad()
                         Economy.reward(false)
                         onUpdate()
                     }
                 }
                 interstitialAd?.show(context)
             }
             Network.isAdsBlocked -> toast("Anuncios bloqueados por host")
             else -> toast("Anuncio aún cargando...")
         }*/
    }
}

class FAdLoaderInterstitialLazyMob(val context: AppCompatActivity) : FullscreenAdLoader {
    //private var interstitialAd: InterstitialAd? = null

    init {
        //createAndLoad()
    }

    /*private fun createAndLoad() {
        interstitialAd = null
        InterstitialAd.load(context, AdsUtilsMob.INTERSTITIAL, AdsUtilsMob.adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(p0: InterstitialAd) {
                interstitialAd = p0
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                Log.e("Ad", "Ad failed to load, code: ${p0.code}")
                GlobalScope.launch(Dispatchers.Main) {
                    delay(3000)
                    if (!context.isFinishing) {
                        createAndLoad()
                    }
                }
            }
        })
    }*/

    override fun load() {
        //if (interstitialAd == null) createAndLoad()
    }

    override fun show() {
        /*when {
            interstitialAd != null -> {
                interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        FirebaseAnalytics.getInstance(App.context).logEvent("Interstitial_Ad_watched", Bundle())
                        createAndLoad()
                        Economy.reward(false)
                    }
                }
                interstitialAd?.show(context)
            }
            Network.isAdsBlocked -> toast("Anuncios bloqueados por host")
            else -> context.lifecycleScope.launch(Dispatchers.Main) {
                var tryCount = 11
                while (interstitialAd == null && tryCount > 0) {
                    delay(250)
                    tryCount--
                }
                if (interstitialAd != null)
                    show()
            }
        }*/
    }
}

/*fun getAdSize(width: Float): AdSize {
    val metrics = App.context.resources.displayMetrics
    val density = metrics.density
    var adWidthPixels = width
    if (adWidthPixels == 0f) {
        adWidthPixels = metrics.widthPixels.toFloat()
    }

    val adWidth = (adWidthPixels / density).toInt()
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(App.context, adWidth)

}*/

//abstract class AbsAdListener : AdListener()

object AdmobID {
    const val RECENT_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val RECENT_BANNER2 = "ca-app-pub-8896186846999141/5663284088"
    const val FAVORITE_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val FAVORITE_BANNER2 = "ca-app-pub-8896186846999141/5663284088"
    const val DIRECTORY_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val HOME_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val HOME_BANNER2 = "ca-app-pub-8896186846999141/5663284088"
    const val EMISSION_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val SEEING_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val RECOMMEND_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val QUEUE_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val RECORD_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val RANDOM_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val NEWS_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val INFO_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val ACHIEVEMENT_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val EXPLORER_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val CAST_BANNER = "ca-app-pub-8896186846999141/5663284088"
    const val LIST_NATIVE = "ca-app-pub-8896186846999141/5113437989"
    const val REWARDED = "ca-app-pub-8896186846999141/9219385713"
    const val INTERSTITIAL = "ca-app-pub-8896186846999141/7231764047"
}