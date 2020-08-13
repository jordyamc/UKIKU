package knf.kuma.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.app.ActivityOptionsCompat
import androidx.core.widget.addTextChangedListener
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.commons.EAHelper
import knf.kuma.commons.setSurfaceBars
import knf.kuma.custom.GenericActivity
import kotlinx.android.synthetic.main.activity_search.*

class SearchActivity : GenericActivity() {

    private val model by viewModels<SearchViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setSurfaceBars()
        setContentView(R.layout.activity_search)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        etSearch.setText(model.queryListener.value)
        etSearch.requestFocus()
        etSearch.addTextChangedListener(afterTextChanged = {
            EAHelper.checkStart(it.toString())
            AchievementManager.onSearch(it.toString())
            model.sendQuery(it?.toString())
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out)
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, SearchActivity::class.java), ActivityOptionsCompat.makeCustomAnimation(context, R.anim.fade_in, R.anim.fade_out).toBundle())
        }
    }
}