package knf.kuma.tv.directory

import android.content.Context
import android.content.Intent
import android.os.Bundle
import knf.kuma.tv.TVBaseActivity

class TVDir : TVBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addFragment(TVDirFragment())
    }

    companion object {
        fun start(context: Context?) {
            context?.startActivity(Intent(context, TVDir::class.java))
        }
    }
}
