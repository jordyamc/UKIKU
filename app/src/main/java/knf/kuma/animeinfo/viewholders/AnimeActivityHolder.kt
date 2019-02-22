package knf.kuma.animeinfo.viewholders

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityOptionsCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import knf.kuma.R
import knf.kuma.animeinfo.AnimePagerAdapter
import knf.kuma.animeinfo.img.ActivityImgFull
import knf.kuma.commons.BypassUtil.Companion.clearCookies
import knf.kuma.commons.BypassUtil.Companion.isLoading
import knf.kuma.commons.BypassUtil.Companion.isNeeded
import knf.kuma.commons.BypassUtil.Companion.saveCookies
import knf.kuma.commons.BypassUtil.Companion.userAgent
import knf.kuma.commons.PicassoSingle
import knf.kuma.commons.bind
import knf.kuma.commons.doOnUI
import knf.kuma.commons.optionalBind
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import xdroid.toaster.Toaster


class AnimeActivityHolder(activity: AppCompatActivity) {
    val appBarLayout: AppBarLayout by bind(activity, R.id.appBar)
    private val collapsingToolbarLayout: CollapsingToolbarLayout by bind(activity, R.id.collapsingToolbar)
    val imageView: ImageView by bind(activity, R.id.img)
    val toolbar: Toolbar by bind(activity, R.id.toolbar)
    private val tabLayout: TabLayout by bind(activity, R.id.tabs)
    val pager: ViewPager by bind(activity, R.id.pager)
    private val fab: FloatingActionButton by bind(activity, R.id.fab)
    private val webView: WebView? by optionalBind(activity, R.id.webview)

    private val intent: Intent = activity.intent
    private val animePagerAdapter: AnimePagerAdapter = AnimePagerAdapter(activity.supportFragmentManager)
    private val innerInterface: Interface = activity as Interface

    private val drawableHeartFull: Drawable by lazy { activity.getDrawable(R.drawable.heart_full) }
    private val drawableHeartEmpty: Drawable by lazy { activity.getDrawable(R.drawable.heart_empty) }
    private val drawableStarHeart: Drawable by lazy { activity.getDrawable(R.drawable.ic_star_heart) }
    private val drawableHalfStar: Drawable by lazy { activity.getDrawable(R.drawable.ic_seeing) }

    init {
        //fab.visibility = View.INVISIBLE
        fab.isEnabled = false
        populate(activity)
        pager.offscreenPageLimit = 2
        pager.adapter = animePagerAdapter
        tabLayout.setupWithViewPager(pager)
        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                appBarLayout.setExpanded(position == 0, true)
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
        if (activity.intent.getBooleanExtra("isRecord", false))
            pager.setCurrentItem(1, true)
        tabLayout.addOnTabSelectedListener(object : TabLayout.ViewPagerOnTabSelectedListener(pager) {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tab?.position == 1)
                    animePagerAdapter.onChaptersReselect()
            }
        })
        fab.onClick { innerInterface.onFabClicked(fab) }
        imageView.onClick { innerInterface.onImgClicked(imageView) }
        checkBypass(activity)
    }

    fun setTitle(title: String) {
        collapsingToolbarLayout.post { collapsingToolbarLayout.title = title }
    }

    fun loadImg(link: String, listener: View.OnClickListener) {
        imageView.post {
            PicassoSingle.get().load(link).noPlaceholder().into(imageView)
            imageView.setOnClickListener(listener)
        }
    }

    fun setFABState(isFav: Boolean) {
        doOnUI {
            fab.setImageDrawable(if (isFav) drawableHeartFull else drawableHeartEmpty)
            fab.invalidate()
        }
    }

    fun setFABSeeing() {
        doOnUI {
            fab.setImageDrawable(drawableHalfStar)
            fab.invalidate()
        }
    }

    fun setFABState(isFav: Boolean, isSeeing: Boolean = false) {
        doOnUI {
            fab.setImageDrawable(when {
                isFav && isSeeing -> drawableStarHeart
                isSeeing -> drawableHalfStar
                isFav -> drawableHeartFull
                else -> drawableHeartEmpty
            })
            fab.invalidate()
        }
    }

    fun showFAB() {
        doOnUI {
            fab.isEnabled = true
            //fab.show()
        }
    }

    fun hideFAB() {
        doOnUI {
            fab.isEnabled = false
            //fab.visibility = View.INVISIBLE
            fab.startAnimation(AnimationUtils.loadAnimation(fab.context, R.anim.scale_down))
        }
    }

    fun hideFABForce() {
        doOnUI {
            fab.isEnabled = false
            //fab.internalSetVisibility(View.INVISIBLE,true)
            fab.hide()
        }
    }

    private fun populate(activity: AppCompatActivity) {
        val title = intent.getStringExtra("title")
        if (title != null)
            collapsingToolbarLayout.title = title
        val img = intent.getStringExtra("img")
        if (img != null) {
            PicassoSingle.get().load(img).into(imageView)
            imageView.setOnClickListener { activity.startActivity(Intent(activity, ActivityImgFull::class.java).setData(Uri.parse(img)).putExtra("title", title), ActivityOptionsCompat.makeSceneTransitionAnimation(activity, imageView, "img").toBundle()) }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun checkBypass(context: Context) {
        if (webView != null)
            doAsync {
                if (isNeeded(context) && !isLoading) {
                    isLoading = true
                    clearCookies()
                    webView?.settings?.javaScriptEnabled = true
                    webView?.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            if (request?.url.toString() == "https://animeflv.net/") {
                                if (saveCookies(context)) {
                                    Toaster.toast("Bypass actualizado")
                                    PicassoSingle.clear()
                                }
                                innerInterface.onNeedRecreate()
                            }
                            isLoading = false
                            return false
                        }
                    }
                    webView?.settings?.userAgentString = userAgent
                    webView?.loadUrl("https://animeflv.net/")
                }
            }
    }

    interface Interface {
        fun onFabClicked(actionButton: FloatingActionButton)

        fun onImgClicked(imageView: ImageView)

        fun onNeedRecreate()
    }
}
