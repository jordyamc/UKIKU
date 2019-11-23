package knf.kuma.animeinfo.viewholders

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
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
import knf.kuma.commons.PicassoSingle
import knf.kuma.commons.bind
import knf.kuma.commons.doOnUI
import knf.kuma.commons.forceHide
import org.jetbrains.anko.sdk27.coroutines.onClick


class AnimeActivityHolder(activity: AppCompatActivity) {
    val appBarLayout: AppBarLayout by bind(activity, R.id.appBar)
    private val collapsingToolbarLayout: CollapsingToolbarLayout by bind(activity, R.id.collapsingToolbar)
    val imageView: ImageView by bind(activity, R.id.img)
    val toolbar: Toolbar by bind(activity, R.id.toolbar)
    private val tabLayout: TabLayout by bind(activity, R.id.tabs)
    val pager: ViewPager by bind(activity, R.id.pager)
    private val fab: FloatingActionButton by bind(activity, R.id.fab)

    private val intent: Intent = activity.intent
    private val animePagerAdapter: AnimePagerAdapter = AnimePagerAdapter(activity.supportFragmentManager)
    private val innerInterface: Interface = activity as Interface

    private val drawableHeartFull: Drawable by lazy { activity.getDrawable(R.drawable.heart_full) as Drawable }
    private val drawableHeartEmpty: Drawable by lazy { activity.getDrawable(R.drawable.heart_empty) as Drawable }

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
                if (tab?.position == 1) {
                    appBarLayout.setExpanded(false, true)
                    animePagerAdapter.onChaptersReselect()
                }
            }
        })
        fab.onClick { innerInterface.onFabClicked(fab) }
        imageView.onClick { innerInterface.onImgClicked(imageView) }
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

    fun showFAB() {
        doOnUI {
            fab.isEnabled = true
            //fab.show()
        }
    }

    fun hideFABForce() {
        fab.isEnabled = false
        fab.forceHide()
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

    interface Interface {
        fun onFabClicked(actionButton: FloatingActionButton)

        fun onImgClicked(imageView: ImageView)
    }
}
