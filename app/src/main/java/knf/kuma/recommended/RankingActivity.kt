package knf.kuma.recommended

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.safeShow
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import kotlinx.android.synthetic.main.recycler_ranking.*

class RankingActivity : GenericActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recycler_ranking)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(false)
        }
        with(toolbar) {
            title = "Ranking"
            setNavigationIcon(R.drawable.ic_close)
            setNavigationOnClickListener { finish() }
        }
        with(recycler) {
            layoutManager = LinearLayoutManager(this@RankingActivity)
            adapter = RankingAdapter()
        }
        setResult(1234)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_rating, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear ->
                MaterialDialog(this@RankingActivity).safeShow {
                    message(text = "¿Deseas reiniciar la puntuación de todos los géneros?")
                    positiveButton(text = "continuar") {
                        setResult(4321)
                        CacheDB.INSTANCE.genresDAO().reset()
                        finish()
                    }
                    negativeButton(text = "cancelar")
                }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        fun open(activity: Activity) {
            activity.startActivityForResult(Intent(activity, RankingActivity::class.java), 46897)
        }
    }
}
