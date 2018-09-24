package knf.kuma.seeing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.bind

class SeeingActivity : AppCompatActivity() {
    val toolbar: Toolbar by bind(R.id.toolbar)
    val tabs: TabLayout by bind(R.id.tabs)
    val pager: ViewPager by bind(R.id.pager)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seening)
        toolbar.title = "Siguiendo"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }
        pager.adapter = SeeingPagerAdapter(supportFragmentManager)
        pager.offscreenPageLimit = 5
        tabs.setupWithViewPager(pager)
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                (pager.adapter as? SeeingPagerAdapter)!!.fragmentList[pager.currentItem].onSelected()
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {

            }

            override fun onTabSelected(p0: TabLayout.Tab?) {

            }
        })
        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                (pager.adapter as? SeeingPagerAdapter)!!.fragmentList[position].clickCount = 0
            }
        })
    }

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, SeeingActivity::class.java))
        }
    }
}
