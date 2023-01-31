package knf.kuma.news

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import knf.kuma.ads.AdCallback
import knf.kuma.commons.doOnUIGlobal
import knf.kuma.commons.toast
import org.jetbrains.anko.doAsync
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.jsoup.select.Elements
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object NewsCreator {
    private val liveData = MutableLiveData<MutableList<NewsObject>?>()
    fun createNews(): LiveData<MutableList<NewsObject>?> {
        reload()
        return liveData
    }

    fun reload() {
        doAsync {
            try {
                val document = Jsoup.connect("https://somoskudasai.com/feed/").parser(Parser.xmlParser()).get()
                val items = document.select("item")
                val news = mutableListOf<NewsObject>()
                items.forEach {
                    val title = it.select("title").first().noCDText()
                    val link = it.select("link").first().noCDText()
                    val author = it.select("dc|creator").first().noCDText()
                    val date = it.select("pubDate").first().noCDText()
                    val categories = it.select("category").getAllStringNoCD()
                    val description = it.select("description").first().noCDText()
                    val content = it.select("content|encoded").first().noCDHtml()
                    news.add(NewsObject(
                            title, link, date, author, categories, description, content
                    ))
                }
                doOnUIGlobal {
                    liveData.value = news
                }
            } catch (e: Exception) {
                e.printStackTrace()
                doOnUIGlobal {
                    liveData.value = null
                }
            }
        }
    }

    fun destroy() {
        doOnUIGlobal {
            liveData.value = null
        }
    }

    fun openNews(activity: AppCompatActivity, newsObject: NewsObject) {
        try {
            //CustomTabsIntent.Builder().build().launchUrl(activity, Uri.parse(newsObject.link))
            NewsDialog.show(activity, newsObject.link)
        } catch (e: ActivityNotFoundException) {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(newsObject.link)))
            } catch (anfe: ActivityNotFoundException) {
                "No se encontró ningun navegador para abrir noticia".toast()
            }
        } catch (ex: Exception) {
            "Error al abrir noticia".toast()
        }
    }

    private fun Element.noCDText(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(text().cdRemoved(), Html.FROM_HTML_MODE_LEGACY).toString()
        else
            Html.fromHtml(text().cdRemoved()).toString()
    }

    private fun Element.noCDHtml(): String {
        return text().cdRemoved()
    }

    private fun Elements.getAllStringNoCD(): List<String> {
        val list = mutableListOf<String>()
        this.forEach {
            list.add(it.noCDText())
        }
        return list
    }

    private fun String.cdRemoved(): String {
        if (!this.trim().startsWith("<![CDATA["))
            return this.trim()
        val pattern = Pattern.compile(
                if (this.contains("se publicó primero en"))
                    if (this.contains("Saber más"))
                        "<!\\[CDATA\\[(.*\\.\\.\\.) <a.*\\s?]]>"
                    else
                        "<!\\[CDATA\\[(.*)<p>La entrada <a.*\\s?]]>"
                else
                    "<!\\[CDATA\\[(.*)\\s?]]>")
        val matcher = pattern.matcher(this)
        matcher.find()
        return matcher.group(1).trim()
    }
}

open class NewsObject(
        val title: String,
        val link: String,
        val date: String,
        val author: String,
        val categories: List<String>,
        val description: String,
        val content: String
) {

    fun metaData(): String {
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss Z", Locale.ENGLISH)
        val formated = dateFormat.parse(date)
        //return "$author — ${TimeAgo.using(formated.time)}"
        val simpleDate = SimpleDateFormat("EEE, dd MMM hh:mmaa", Locale.getDefault())
        return "$author — ${simpleDate.format(formated)}"
    }

    override fun equals(other: Any?): Boolean {
        return other is NewsObject && other.link == link
    }

    override fun hashCode(): Int {
        return link.hashCode()
    }
}

class AdNewsObject(private val adId: String) : NewsObject("", "", "", "", emptyList(), "", ""), AdCallback {
    override fun getID(): String = adId
}