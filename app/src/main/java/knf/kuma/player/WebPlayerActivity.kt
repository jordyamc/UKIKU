package knf.kuma.player

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import knf.kuma.databinding.ActivityBrowserBinding
import knf.kuma.uagen.randomPCUA

class WebPlayerActivity : AppCompatActivity() {
    val binding by lazy { ActivityBrowserBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        setContentView(binding.root)
        binding.webview.apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
            webViewClient = object : WebViewClient() {
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    //view?.loadUrl(url ?: return true)
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    binding.loading.isVisible = false
                }
            }
            //loadUrl(intent.dataString?:"about:blank")
            intent.dataString?.let {
                loadData(framed(it), "text/html; charset=utf-8", "UTF-8")
            } ?: finish()
        }
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
}

fun framed(link: String): String =
    "<html><body style='margin:0;padding:0;'><iframe src=\"$link\" scrolling=\"no\" allowfullscreen=\"\" width=\"100%\" height=\"100%\" frameborder=\"0\"></iframe></body></html>"

fun openWebPlayer(context: Context, link: String){
    context.startActivity(Intent(context,WebPlayerActivity::class.java).apply {
        data = Uri.parse(link)
    })
}