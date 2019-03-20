package knf.kuma.news

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LevelListDrawable
import android.os.Bundle
import android.text.Html
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PicassoSingle
import knf.kuma.commons.doOnUI
import knf.kuma.custom.GenericActivity
import kotlinx.android.synthetic.main.activity_news_details.*
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster

class NewsDetailsActivity : GenericActivity(), Html.ImageGetter {
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
        getDrawable(R.mipmap.ic_launcher)?.let {
            levelListDrawable.addLevel(0, 0, it)
            levelListDrawable.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
            doAsync {
                val bitmap = PicassoSingle.get().load(source).get()
                val drawable = BitmapDrawable(resources, bitmap)
                levelListDrawable.addLevel(1, 1, drawable)
                levelListDrawable.setBounds(0, 0, bitmap.width, bitmap.height)
                levelListDrawable.level = 1
                doOnUI {
                    content.refreshDrawableState()
                }
            }
        }
        return levelListDrawable
    }
}