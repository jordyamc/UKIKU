package knf.kuma.tv.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import knf.kuma.tv.TVBaseActivity

class TVSearch : TVBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addFragment(TVSearchFragment())
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, TVSearch::class.java))
        }
    }
}
