package knf.kuma.animeinfo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import knf.kuma.R
import knf.kuma.commons.doOnUI
import kotlinx.android.synthetic.main.lay_comments.view.*

class CommentariesDialog : BottomSheetDialogFragment(), LifecycleObserver {
    private var link: String = "about:blank"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.lay_comments, container, false)
        doOnUI {
            rootView.webview.apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        rootView.loading.visibility = View.GONE
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        view?.loadUrl(url)
                        return true
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return shouldOverrideUrlLoading(view, request?.url?.toString())
                    }
                }
                loadUrl(link)
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

        fun show(activity: AppCompatActivity, link: String) {
            CommentariesDialog().apply {
                this.link = link
                setUpOwner(activity)
            }.safeShow(activity.supportFragmentManager, "BottomSheetDialog")
        }

        fun show(fragment: Fragment, link: String) {
            CommentariesDialog().apply {
                this.link = link
                setUpOwner(fragment)
            }.safeShow(fragment.childFragmentManager, "BottomSheetDialog")
        }
    }
}
