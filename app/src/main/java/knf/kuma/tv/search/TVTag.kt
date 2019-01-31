package knf.kuma.tv.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import knf.kuma.tv.TVBaseActivity

class TVTag : TVBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addFragment(TVTagFragment().apply { arguments = Bundle().apply { putString(keyGenre, intent.getStringExtra(keyGenre)) } })
    }

    companion object {

        private const val keyGenre = "genre"

        fun start(context: Context, genre: String) {
            context.startActivity(Intent(context, TVTag::class.java).putExtra(keyGenre, genre))
        }
    }
}
