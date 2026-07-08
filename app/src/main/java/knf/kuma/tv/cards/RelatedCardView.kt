package knf.kuma.tv.cards

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import knf.kuma.R
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.loadGlide
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.av1.Relation
import knf.kuma.tv.BindableCardView
import org.jetbrains.anko.find

class RelatedCardView(context: Context) : BindableCardView<Relation>(context) {

    override val imageView: ImageView
        get() = find(R.id.img)
    override val layoutResource: Int
        get() = R.layout.item_tv_card_chapter

    override fun bind(data: Relation) {
        imageView.loadGlide(data.imageUrl)
        find<TextView>(R.id.title).text = data.name
        find<TextView>(R.id.chapter).text = getRelation(data.relation)
    }

    fun getRelation(id: Int): String {
        return when(id) {
            8 -> "Historia Principal"
            1 -> "Precuela"
            2 -> "Secuela"
            4 -> "Versión Alternativa"
            3 -> "Ambientación Alternativa"
            7 -> "Historia Completa"
            5 -> "Historia Paralela"
            6 -> "Resumen"
            else -> "Otro"
        }
    }
}
