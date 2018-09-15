package knf.kuma.emision

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import knf.kuma.R
import knf.kuma.commons.EAHelper
import kotlinx.android.synthetic.main.activity_emision.*
import java.util.*

class EmisionActivity : AppCompatActivity(), TabLayout.OnTabSelectedListener {
    private var pagerAdapter: EmisionPagerAdapter? = null

    private val currentDay: Int
        get() {
            var day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            day--
            return if (day == 0)
                7
            else
                day
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emision)
        toolbar.title = "Emisión"
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowHomeEnabled(false)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        pager.offscreenPageLimit = 7
        pagerAdapter = EmisionPagerAdapter(supportFragmentManager)
        pager.adapter = pagerAdapter
        tabs.setupWithViewPager(pager)
        tabs.addOnTabSelectedListener(this)
        pager.setCurrentItem(currentDay - 1, true)
        EAHelper.clear2()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_emision, menu)
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_hidden", false))
            menu.findItem(R.id.action_hideshow).setIcon(R.drawable.ic_hide)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val show = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_hidden", false)
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("show_hidden", !show).apply()
        invalidateOptionsMenu()
        if (pagerAdapter != null)
            pagerAdapter!!.reloadPages()
        return super.onOptionsItemSelected(item)
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        EAHelper.enter2(getDayByPos(tab.position).toString())
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {

    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        EAHelper.enter2(getDayByPos(tab.position).toString())
    }

    private fun getDayByPos(position: Int): Int {
        var pos = position
        pos += 2
        if (pos == 8)
            pos = 1
        return pos
    }

    companion object {

        fun open(context: Activity) {
            context.startActivityForResult(Intent(context, EmisionActivity::class.java), 4987)
        }
    }
}
