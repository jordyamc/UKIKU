package knf.kuma

import android.content.Context
import android.content.Intent
import android.net.Uri

import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutActivity
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import knf.kuma.changelog.ChangelogActivity
import knf.kuma.commons.EAHelper
import xdroid.toaster.Toaster

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

    override fun getMaterialAboutList(context: Context): MaterialAboutList {
        val infoCard = MaterialAboutCard.Builder()
        infoCard.addItem(ConvenienceBuilder.createAppTitleItem(this))
        infoCard.addItem(ConvenienceBuilder.createVersionActionItem(this, getDrawable(R.drawable.ic_version), "Versión", true))
        infoCard.addItem(MaterialAboutActionItem.Builder().text("Changelog").icon(R.drawable.ic_changelog_get).setOnClickAction { ChangelogActivity.open(this@AppInfo) }.build())
        val authorCard = MaterialAboutCard.Builder()
        authorCard.title("Autor")
        authorCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_author), "Jordy Mendoza", true, Uri.parse("https://t.me/UnbarredStream")))
        val donateCard = MaterialAboutCard.Builder()
        donateCard.title("Donar")
        donateCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_paypal), "Paypal", false, paypalUri))
        donateCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_patreon), "Patreon", false, Uri.parse("https://www.patreon.com/animeflvapp")))
        val extraCard = MaterialAboutCard.Builder()
        extraCard.title("Extras")
        extraCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_web), "Página web", true, Uri.parse("https://ukiku.ga")))
        extraCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_github), "Proyecto en github", true, Uri.parse("https://github.com/jordyamc/UKIKU")))
        extraCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_facebook), "Facebook", true, Uri.parse("https://www.facebook.com/ukikuapp")))
        extraCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_facebook_group), "Grupo de Facebook", true, Uri.parse("https://www.facebook.com/groups/ukikugroup/")))
        extraCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_discord), "Discord", false, Uri.parse("https://discord.gg/6hzpua6")))
        extraCard.addItem(ConvenienceBuilder.createWebsiteActionItem(this@AppInfo, getDrawable(R.drawable.ic_beta), "Grupo Beta", false, Uri.parse("https://t.me/joinchat/A3tvqEKOzGVyaZhQPc14_Q")))
        extraCard.addItem(MaterialAboutActionItem.Builder().text("Easter egg").icon(R.drawable.ic_egg).setOnClickAction { Toaster.toastLong(EAHelper.eaMessage) }.build())
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

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, AppInfo::class.java))
        }
    }
}
