package knf.kuma.animeinfo

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import knf.kuma.R
import knf.kuma.commons.safeShow
import knf.kuma.pojos.AnimeObject
import java.net.URLEncoder
import java.util.*

class CommentsDialog(private val chapters: MutableList<AnimeObject.WebInfo.AnimeChapter>) {
    lateinit var spinner: Spinner
    lateinit var progressBar: ProgressBar
    lateinit var scrollView: ScrollView
    lateinit var webView: WebView

    private val eps: Array<String>
        get() {
            val eps = ArrayList<String>()
            for (chapter in chapters) {
                eps.add(chapter.number)
            }
            return eps.toTypedArray()
        }

    @SuppressLint("SetJavaScriptEnabled")
    fun show(activity: Activity) {
        val dialog = MaterialDialog(activity)
                .customView(R.layout.layout_comments_dialog)
        with(dialog.getCustomView()!!) {
            spinner = findViewById(R.id.spinner)
            progressBar = findViewById(R.id.progress)
            scrollView = findViewById(R.id.scroll)
            webView = findViewById(R.id.webview)
        }
        webView.settings.javaScriptEnabled = true
        val newUA = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0"
        webView.settings.userAgentString = newUA
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onHide()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onShow()
            }
        }
        spinner.adapter = ArrayAdapter(activity, R.layout.item_simple_spinner, eps)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                webView.loadUrl(getLink(chapters[position].link))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        webView.loadUrl(getLink(chapters[0].link))
        dialog.safeShow()
    }

    private fun getLink(link: String): String {
        return try {
            "https://web.facebook.com/plugins/comments.php?api_key=156149244424100&channel_url=https%3A%2F%2Fstaticxx.facebook.com%2Fconnect%2Fxd_arbiter%2Fr%2FlY4eZXm_YWu.js%3Fversion%3D42%23cb%3Df3448d0a8b0514c%26domain%3Danimeflv.net%26origin%3Dhttps%253A%252F%252Fanimeflv.net%252Ff304e603e6a096%26relation%3Dparent.parent&href=" +
                    URLEncoder.encode(link, "UTF-8") +
                    "&locale=es_LA&numposts=50&sdk=joey&version=v2.9&width=100%25"
        } catch (e: Exception) {
            e.printStackTrace()
            link
        }

    }

    private fun onHide() {
        progressBar.visibility = View.VISIBLE
        webView.visibility = View.INVISIBLE
        scrollView.fullScroll(ScrollView.FOCUS_UP)
    }

    private fun onShow() {
        progressBar.visibility = View.GONE
        webView.visibility = View.VISIBLE
    }
}
