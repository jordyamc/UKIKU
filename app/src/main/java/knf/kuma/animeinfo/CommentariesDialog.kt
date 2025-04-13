package knf.kuma.animeinfo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import knf.kuma.R
import knf.kuma.commons.jsoupCookies
import knf.kuma.commons.urlEncode
import knf.kuma.databinding.LayCommentsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentariesDialog : BottomSheetDialogFragment(), LifecycleObserver {
    private var link: String = "about:blank"
    private var version: String = "1.0"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.lay_comments, container, false)
        val binding = LayCommentsBinding.bind(rootView)
        lifecycleScope.launch {
            val html = withContext(Dispatchers.IO) { jsoupCookies(link).execute().body() }
            val regex = Regex("this.page.url = '([^']*)';\\s+this.page.identifier = '([^']*)").find(html)
            val (target, identifier) = regex!!.destructured
            val url = "https://disqus.com/embed/comments/?base=default&f=https-myanimelist-net-2&t_i=$identifier&t_u=${urlEncode(target)}&s_o=default#version=$version"
            binding.webview.apply {
                setInitialScale(1)
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        binding.loading.visibility = View.GONE
                    }
                }
                loadUrl(url)
            }
        }
        return rootView
    }

    fun setUpOwner(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPaused() {
        safeDismiss()
    }

    fun safeShow(manager: FragmentManager, tag: String) {
        try {
            show(manager, tag)
        } catch (e: Exception) {
            //
        }

    }

    private fun safeDismiss() {
        try {
            dismiss()
        } catch (e: Exception) {
            //
        }
    }

    companion object {

        fun show(fragment: Fragment, link: String, version: String) {
            CommentariesDialog().apply {
                this.link = link
                this.version = version
                setUpOwner(fragment)
            }.safeShow(fragment.childFragmentManager, "BottomSheetDialog")
        }
    }
}
