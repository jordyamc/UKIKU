package knf.kuma

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.net.toUri
import com.afollestad.materialdialogs.MaterialDialog
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import knf.kuma.ads.AdsUtils
import knf.kuma.ads.FullscreenAdLoader
import knf.kuma.ads.getFAdLoaderInterstitial
import knf.kuma.ads.getFAdLoaderRewarded
import knf.kuma.backup.Backups
import knf.kuma.changelog.ChangelogActivity
import knf.kuma.changelog.ChangelogActivityMaterial
import knf.kuma.commons.DesignUtils
import knf.kuma.commons.EAUnlockActivity
import knf.kuma.commons.Economy
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.isFullMode
import knf.kuma.commons.safeShow
import knf.kuma.profile.TopActivity
import knf.kuma.profile.TopActivityMaterial
import knf.tools.kprobability.item
import knf.tools.kprobability.probabilityOf
import org.jetbrains.anko.support.v4.toast

class AppInfoFragment: MaterialAboutFragment() {
    private val paypalUri: Uri
        get() {
            val uriBuilder = Uri.Builder()
            uriBuilder.scheme("https").authority("www.paypal.com").path("cgi-bin/webscr")
            uriBuilder.appendQueryParameter("cmd", "_donations")
            uriBuilder.appendQueryParameter("business", "jordyamc@hotmail.com")
            uriBuilder.appendQueryParameter("lc", "US")
            uriBuilder.appendQueryParameter("item_name", "Donación UKIKU")
            uriBuilder.appendQueryParameter("no_note", "1")
            uriBuilder.appendQueryParameter("no_shipping", "1")
            uriBuilder.appendQueryParameter("currency_code", "USD")
            return uriBuilder.build()
        }

    private val isFlat: Boolean by lazy { requireArguments().getBoolean("isFlat",true) }
    private val rewardedAd: FullscreenAdLoader by lazy { getFAdLoaderRewarded(requireActivity()) }
    private lateinit var interstitial: FullscreenAdLoader
    private lateinit var videoItem: MaterialAboutActionItem

    private fun showAd() {
        probabilityOf<() -> Unit> {
            item({ rewardedAd.show() }, AdsUtils.remoteConfigs.getDouble("rewarded_percent"))
            item({ interstitial.show() }, AdsUtils.remoteConfigs.getDouble("interstitial_percent"))
        }.random()()
    }

    private fun setupUpdateCount() {
        Economy.rewardedVideoLiveData.observe(viewLifecycleOwner) {
            if (::videoItem.isInitialized) {
                videoItem.subText = "Vistos: $it"
                setMaterialAboutList(getMaterialAboutList(requireContext()))
            }
        }
    }


    override fun getMaterialAboutList(context: Context): MaterialAboutList {
        val infoCard = MaterialAboutCard.Builder()
        infoCard.outline(isFlat)
        infoCard.addItem(ConvenienceBuilder.createAppTitleItem(requireContext()))
        infoCard.addItem(ConvenienceBuilder.createVersionActionItem(requireContext(), getDrawable(requireContext(),R.drawable.ic_version), "Versión", true))
        infoCard.addItem(MaterialAboutActionItem.Builder().text("Changelog").icon(R.drawable.ic_changelog_get).setOnClickAction { openChangelog() }.build())
        infoCard.addItem(MaterialAboutActionItem.Builder().text("Diagnóstico").icon(R.drawable.ic_diagnostic).setOnClickAction { openDiagnostic() }.build())
        infoCard.addItem(MaterialAboutActionItem.Builder().text("Suscripción").icon(R.drawable.ic_key).setOnClickAction {
            if (Backups.isKeyInstalled) {
                val intent = requireContext().packageManager.getLaunchIntentForPackage("knf.kuma.key")
                intent?.let { startActivity(intent) } ?: toast("Error al abrir UKIKU Key")
            } else {
                MaterialDialog(requireContext()).safeShow {
                    message(text = "Con la suscripción podrás usar el backup por Firestore sin activar los anuncios!")
                    positiveButton(text = "Suscribirse") {
                        startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=knf.kuma.key".toUri()))
                    }
                    negativeButton(text = "cancelar")
                }
            }
        }.build())
        val authorCard = MaterialAboutCard.Builder()
        authorCard.outline(isFlat)
        authorCard.title("Autor")
        authorCard.addItem(
            ConvenienceBuilder.createWebsiteActionItem(
                requireContext(),
                getDrawable(requireContext(), R.drawable.ic_author),
                "Jordy Mendoza",
                true,
                "https://t.me/unbarred_stream".toUri()
            )
        )
        val donateCard = MaterialAboutCard.Builder()
        if (isFullMode) {
            donateCard.outline(isFlat)
            donateCard.title("Donar")
            donateCard.addItem(
                ConvenienceBuilder.createWebsiteActionItem(
                    requireContext(),
                    getDrawable(requireContext(), R.drawable.ic_paypal),
                    "Paypal",
                    false,
                    "https://www.paypal.com/donate/?hosted_button_id=9WXSCG3AP639J".toUri()
                )
            )
            donateCard.addItem(
                ConvenienceBuilder.createWebsiteActionItem(
                    requireContext(),
                    getDrawable(requireContext(), R.drawable.ic_bitcoin),
                    "Binance",
                    false,
                    "https://app.binance.com/uni-qr/4ARGHuhS".toUri()
                )
            )
            donateCard.addItem(
                ConvenienceBuilder.createWebsiteActionItem(
                    requireContext(),
                    getDrawable(requireContext(), R.drawable.ic_patreon),
                    "Patreon",
                    false,
                    "https://www.patreon.com/animeflvapp".toUri()
                )
            )
            donateCard.addItem(
                ConvenienceBuilder.createWebsiteActionItem(
                    requireContext(),
                    getDrawable(requireContext(), R.drawable.ic_cuplogo),
                    "Ko-fi",
                    false,
                    "https://ko-fi.com/unbarredstream".toUri()
                )
            )
            donateCard.addItem(
                MaterialAboutActionItem.Builder().text("Ver anuncio")
                    .subText("Vistos: ${PrefsUtil.userRewardedVideoCount}").icon(R.drawable.ic_cash)
                    .setOnClickAction {
                        showAd()
                    }.build().also { videoItem = it })
        }
        val extraCard = MaterialAboutCard.Builder()
        extraCard.outline(isFlat)
        extraCard.title("Extras")
        extraCard.addItem(
            MaterialAboutActionItem.Builder().text("Cartera de loli-coins").icon(R.drawable.ic_coin)
                .setOnClickAction { Economy.showWallet(requireActivity(), true) { showAd() } }
                .build()
        )
        extraCard.addItem(
            MaterialAboutActionItem.Builder().text("Top videos vistos").icon(R.drawable.ic_podium)
                .setOnClickAction { openTop() }.build()
        )
        extraCard.addItem(
            ConvenienceBuilder.createWebsiteActionItem(
                requireContext(),
                getDrawable(requireContext(), R.drawable.ic_web),
                "Politica de privacidad",
                true,
                "https://ukiku.app/policy.html".toUri()
            )
        )
        extraCard.addItem(
            ConvenienceBuilder.createWebsiteActionItem(
                requireContext(),
                getDrawable(requireContext(), R.drawable.ic_github),
                "Proyecto en github",
                true,
                "https://github.com/jordyamc/UKIKU".toUri()
            )
        )
        extraCard.addItem(
            ConvenienceBuilder.createWebsiteActionItem(
                requireContext(),
                getDrawable(requireContext(), R.drawable.ic_twitter),
                "Twitter (X)",
                false,
                "https://x.com/AppsKnf".toUri()
            )
        )
        extraCard.addItem(
            ConvenienceBuilder.createWebsiteActionItem(
                requireContext(),
                getDrawable(requireContext(), R.drawable.ic_discord),
                "Discord",
                false,
                "https://discord.gg/6hzpua6".toUri()
            )
        )
        extraCard.addItem(
            ConvenienceBuilder.createWebsiteActionItem(
                requireContext(),
                getDrawable(requireContext(), R.drawable.ic_beta),
                "Grupo Beta",
                false,
                "https://t.me/ukiku_group".toUri()
            )
        )
        extraCard.addItem(
            MaterialAboutActionItem.Builder().text("Easter egg").icon(R.drawable.ic_egg)
                .setOnClickAction { EAUnlockActivity.start(requireContext()) }.build()
        )
        return MaterialAboutList.Builder().apply {
            addCard(infoCard.build())
            addCard(authorCard.build())
            donateCard.build().let {
                if (it.items.isNotEmpty()) addCard(it)
            }
            addCard(extraCard.build())
        }.build()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        interstitial = getFAdLoaderInterstitial(requireActivity())
        rewardedAd.load()
        interstitial.load()
        setupUpdateCount()
    }

    fun openChangelog(){
        if (DesignUtils.isFlat)
            ChangelogActivityMaterial.open(requireContext())
        else
            ChangelogActivity.open(requireContext())
    }

    fun openDiagnostic(){
        if (DesignUtils.isFlat)
            DiagnosticMaterial.open(requireContext())
        else
            Diagnostic.open(requireContext())
    }

    fun openTop() {
        if (DesignUtils.isFlat)
            TopActivityMaterial.open(requireContext())
        else
            TopActivity.open(requireContext())
    }
}