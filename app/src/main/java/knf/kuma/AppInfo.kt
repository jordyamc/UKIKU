package knf.kuma

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutActivity
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import knf.kuma.ads.FullscreenAdLoader
import knf.kuma.ads.getFAdLoaderInterstitial
import knf.kuma.ads.getFAdLoaderRewarded
import knf.kuma.changelog.ChangelogActivity
import knf.kuma.commons.EAUnlockActivity
import knf.kuma.commons.Economy
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.profile.TopActivity
import knf.tools.kprobability.item
import knf.tools.kprobability.probabilityOf

/**
 * Created by jordy on 05/03/2018.
 */

class AppInfo : MaterialAboutActivity() {

    private val paypalUri: Uri
        get() {
            val uriBuilder = Uri.Builder()
            uriBuilder.scheme("https").authority("www.paypal.com").path("cgi-bin/webscr")
            uriBuilder.appendQueryParameter("cmd", "_donations")
            uriBuilder.appendQueryParameter("business", "jordyamc@hotmail.com")
            uriBuilder.appendQueryParameter("lc", "US")
            uriBuilder.appendQueryParameter("item_name", "Donación")
            uriBuilder.appendQueryParameter("no_note", "1")
            uriBuilder.appendQueryParameter("no_shipping", "1")
            uriBuilder.appendQueryParameter("currency_code", "USD")
            return uriBuilder.build()
        }

    private val rewardedAd: FullscreenAdLoader by lazy { getFAdLoaderRewarded(this) }
    private var interstitial: FullscreenAdLoader = getFAdLoaderInterstitial(this)
    private lateinit var videoItem: MaterialAboutActionItem

    private fun showAd() {
        probabilityOf<() -> Unit> {
            if (!Network.isAdsBlocked && BuildConfig.BUILD_TYPE == "playstore")
                item({ rewardedAd.show() }, 80.0)
            item({ interstitial.show() }, 20.0)
        }.random()()
    }

    private fun setupUpdateCount() {
        Economy.rewardedVideoLiveData.observe(this, Observer {
            if (::videoItem.isInitialized) {
                videoItem.subText = "Vistos: $it"
                setMaterialAboutList(getMaterialAboutList(this))
            }
        })
    }


    override fun getMaterialAboutList(context: Context): MaterialAboutList {
        val infoCard = MaterialAboutCard.Builder()
        infoCard.addItem(ConvenienceBuilder.createAppTitleItem(this))
        infoCard.addItem(ConvenienceBuilder.createVersionActionItem(this, getDrawable(R.drawable.ic_version), "Versión", true))
        infoCard.addItem(MaterialAboutActionItem.Builder().text("Changelog").icon(R.drawable.ic_changelog_get).setOnClickAction { ChangelogActivity.open(this@AppInfo) }.build())
        infoCard.addItem(MaterialAboutActionItem.Builder().text("Diagnóstico").icon(R.drawable.ic_diagnostic).setOnClickAction { Diagnostic.open(this@AppInfo) }.build())
        val authorCard = MaterialAboutCard.Builder()
        authorCard.title("Autor")
        authorCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_author), "Jordy Mendoza", true, Uri.parse("https://t.me/UnbarredStream")))
        val donateCard = MaterialAboutCard.Builder()
        donateCard.title("Donar")
        donateCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_paypal), "Paypal", false, paypalUri))
        donateCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_patreon), "Patreon", false, Uri.parse("https://www.patreon.com/animeflvapp")))
        donateCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_cuplogo), "Ko-fi", false, Uri.parse("https://ko-fi.com/unbarredstream")))
        if (BuildConfig.BUILD_TYPE != "playstore")
            donateCard.addItem(MaterialAboutActionItem.Builder().text("Ver anuncio").subText("Vistos: ${PrefsUtil.userRewardedVideoCount}").icon(R.drawable.ic_cash).setOnClickAction {
                showAd()
            }.build().also { videoItem = it })

        val extraCard = MaterialAboutCard.Builder()
        extraCard.title("Extras")
        extraCard.addItem(MaterialAboutActionItem.Builder().text("Cartera de loli-coins").icon(R.drawable.ic_coin).setOnClickAction { Economy.showWallet(this, true) { showAd() } }.build())
        extraCard.addItem(MaterialAboutActionItem.Builder().text("Top videos vistos").icon(R.drawable.ic_podium).setOnClickAction { TopActivity.open(this) }.build())
        extraCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_web), "Página web", true, Uri.parse("https://ukiku.ga")))
        extraCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_github), "Proyecto en github", true, Uri.parse("https://github.com/jordyamc/UKIKU")))
        extraCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_facebook), "Facebook", true, Uri.parse("https://www.facebook.com/ukikuapp")))
        extraCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_facebook_group), "Grupo de Facebook", true, Uri.parse("https://www.facebook.com/groups/ukikugroup")))
        extraCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_discord), "Discord", false, Uri.parse("https://discord.gg/6hzpua6")))
        extraCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_beta), "Grupo Beta", false, Uri.parse("https://t.me/ukiku_beta")))
        extraCard.addItem(MaterialAboutActionItem.Builder().text("Easter egg").icon(R.drawable.ic_egg).setOnClickAction { EAUnlockActivity.start(this) }.build())
        return MaterialAboutList.Builder()
                .addCard(infoCard.build())
                .addCard(authorCard.build())
                .addCard(donateCard.build())
                .addCard(extraCard.build())
                .build()
    }

    override fun getActivityTitle(): CharSequence? {
        return "Acerca de"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rewardedAd.load()
        interstitial.load()
        setupUpdateCount()
    }

    override fun onResume() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            val appIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            setTaskDescription(ActivityManager.TaskDescription(getString(R.string.app_name), appIcon, ContextCompat.getColor(this, R.color.colorPrimary)))
        }
        super.onResume()
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, AppInfo::class.java))
        }
    }
}
