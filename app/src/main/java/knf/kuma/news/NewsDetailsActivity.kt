package knf.kuma.news

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LevelListDrawable
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PicassoSingle
import kotlinx.android.synthetic.main.activity_news_details.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster

class NewsDetailsActivity : AppCompatActivity(), Html.ImageGetter {
    private val newsObject: NewsObject? by lazy { NewsCreator.currentNews }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_details)
        if (newsObject == null) {
            Toaster.toast("Error al abrir noticia")
            finish()
            return
        }
        title_details.text = newsObject?.title
        content.text = newsObject?.richContent(this)
    }

    override fun getDrawable(source: String?): Drawable {
        val levelListDrawable = LevelListDrawable()
        val empty = getDrawable(R.mipmap.ic_launcher)
        levelListDrawable.addLevel(0, 0, empty)
        levelListDrawable.setBounds(0, 0, empty.intrinsicWidth, empty.intrinsicHeight)
        doAsync {
            val bitmap = PicassoSingle[this@NewsDetailsActivity].load(source).get()
            val drawable = BitmapDrawable(resources, bitmap)
            levelListDrawable.addLevel(1, 1, drawable)
            levelListDrawable.setBounds(0, 0, bitmap.width, bitmap.height)
            levelListDrawable.level = 1
            launch(UI) {
                content.refreshDrawableState()
            }
        }
        return levelListDrawable
    }
}