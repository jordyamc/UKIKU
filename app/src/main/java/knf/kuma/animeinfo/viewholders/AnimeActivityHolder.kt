package knf.kuma.animeinfo.viewholders

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityOptionsCompat
import androidx.viewpager.widget.ViewPager
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnLongClick
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import knf.kuma.R
import knf.kuma.animeinfo.ActivityImgFull
import knf.kuma.animeinfo.AnimePagerAdapter
import knf.kuma.commons.BypassUtil.Companion.clearCookies
import knf.kuma.commons.BypassUtil.Companion.isLoading
import knf.kuma.commons.BypassUtil.Companion.isNeeded
import knf.kuma.commons.BypassUtil.Companion.saveCookies
import knf.kuma.commons.BypassUtil.Companion.userAgent
import knf.kuma.commons.PicassoSingle
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster

@SuppressLint("RestrictedApi")
class AnimeActivityHolder(activity: AppCompatActivity) {
    @BindView(R.id.appBar)
    lateinit var appBarLayout: AppBarLayout
    @BindView(R.id.collapsingToolbar)
    lateinit var collapsingToolbarLayout: CollapsingToolbarLayout
    @BindView(R.id.img)
    lateinit var imageView: ImageView
    @BindView(R.id.toolbar)
    lateinit var toolbar: Toolbar
    @BindView(R.id.tabs)
    lateinit var tabLayout: TabLayout
    @BindView(R.id.pager)
    lateinit var pager: ViewPager
    @BindView(R.id.fab)
    lateinit var fab: FloatingActionButton
    @BindView(R.id.webview)
    @JvmField
    var webView: WebView? = null

    private val intent: Intent = activity.intent
    private val animePagerAdapter: AnimePagerAdapter = AnimePagerAdapter(activity.supportFragmentManager)
    private val innerInterface: Interface = activity as Interface

    private val drawableHeartFull: Drawable by lazy { activity.getDrawable(R.drawable.heart_full) }
    private val drawableHeartEmpty: Drawable by lazy { activity.getDrawable(R.drawable.heart_empty) }
    private val drawableStarHeart: Drawable by lazy { activity.getDrawable(R.drawable.ic_star_heart) }
    private val drawableHalfStar: Drawable by lazy { activity.getDrawable(R.drawable.ic_seeing) }

    init {
        ButterKnife.bind(this, activity.findViewById<View>(android.R.id.content))
        fab.visibility = View.INVISIBLE
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
                if (tab!!.position == 1)
                    animePagerAdapter.onChaptersReselect()
            }
        })
        checkBypass(activity)
    }

    fun setTitle(title: String) {
        collapsingToolbarLayout.post { collapsingToolbarLayout.title = title }
    }

    fun loadImg(link: String, listener: View.OnClickListener) {
        imageView.post {
            PicassoSingle[imageView.context].load(link).noPlaceholder().into(imageView)
            imageView.setOnClickListener(listener)
        }
    }

    @OnClick(R.id.fab)
    internal fun onFabClick(actionButton: FloatingActionButton) {
        innerInterface.onFabClicked(actionButton)
    }

    @OnLongClick(R.id.fab)
    internal fun onFabLongClick(actionButton: FloatingActionButton): Boolean {
        innerInterface.onFabLongClicked(actionButton)
        return true
    }

    @OnClick(R.id.img)
    internal fun onImgClick(imageView: ImageView) {
        innerInterface.onImgClicked(imageView)
    }

    fun setFABState(isFav: Boolean) {
        launch(UI) { fab.setImageDrawable(if (isFav) drawableHeartFull else drawableHeartEmpty) }
    }

    fun setFABSeeing() {
        launch(UI) { fab.setImageDrawable(drawableHalfStar) }
    }

    fun setFABState(isFav: Boolean, isSeeing: Boolean) {
        launch(UI) {
            fab.setImageDrawable(when {
                isFav && isSeeing -> drawableStarHeart
                isSeeing -> drawableHalfStar
                isFav -> drawableHeartFull
                else -> drawableHeartEmpty
            })
        }
    }

    fun showFAB() {
        launch(UI) {
            fab.isEnabled = true
            fab.visibility = View.VISIBLE
            fab.startAnimation(AnimationUtils.loadAnimation(fab.context, R.anim.scale_up))
        }
    }

    fun hideFAB() {
        launch(UI) {
            fab.isEnabled = false
            fab.visibility = View.INVISIBLE
            fab.startAnimation(AnimationUtils.loadAnimation(fab.context, R.anim.scale_down))
        }
    }

    fun hideFABForce() {
        launch(UI) {
            fab.isEnabled = false
            fab.visibility = View.INVISIBLE
        }
    }

    private fun populate(activity: AppCompatActivity) {
        val title = intent.getStringExtra("title")
        if (title != null)
            collapsingToolbarLayout.title = title
        val img = intent.getStringExtra("img")
        if (img != null) {
            PicassoSingle[activity].load(img).into(imageView)
            imageView.setOnClickListener { activity.startActivity(Intent(activity, ActivityImgFull::class.java).setData(Uri.parse(img)).putExtra("title", title), ActivityOptionsCompat.makeSceneTransitionAnimation(activity, imageView, "img").toBundle()) }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun checkBypass(context: Context) {
        if (webView != null)
            doAsync {
                if (isNeeded(context) && !isLoading) {
                    isLoading = true
                    Log.e("CloudflareBypass", "is needed")
                    clearCookies()
                    webView!!.settings.javaScriptEnabled = true
                    webView!!.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            Log.e("CloudflareBypass", "Override ${request!!.url}")
                            if (request.url.toString() == "https://animeflv.net/") {
                                Log.e("CloudflareBypass", "Cookies: " + CookieManager.getInstance().getCookie("https://animeflv.net/"))
                                saveCookies(context)
                                Toaster.toast("Bypass actualizado")
                                PicassoSingle.clear()
                                innerInterface.onNeedRecreate()
                            }
                            isLoading = false
                            return false
                        }
                    }
                    webView!!.settings.userAgentString = userAgent
                    webView!!.loadUrl("https://animeflv.net/")
                } else {
                    Log.e("CloudflareBypass", "Not needed")
                }
            }
    }

    interface Interface {
        fun onFabClicked(actionButton: FloatingActionButton)

        fun onFabLongClicked(actionButton: FloatingActionButton)

        fun onImgClicked(imageView: ImageView)

        fun onNeedRecreate()
    }
}
