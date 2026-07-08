package knf.kuma.tv.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import knf.kuma.pojos.av1.Genre
import knf.kuma.tv.TVBaseActivity

class TVTag : TVBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addFragment(TVTagFragment().apply { arguments = intent.extras })
    }

    companion object {

        private const val keyName= "name"
        private const val keySlug = "slug"


        fun start(context: Context, genre: Genre) {
            context.startActivity(Intent(context, TVTag::class.java)
                .putExtra(keyName, genre.name)
                .putExtra(keySlug, genre.slug)
            )
        }
    }
}
