package knf.kuma.ads

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import org.jetbrains.anko.find


class AdCardItemHolder(parent: ViewGroup, @LayoutRes type: Int = TYPE_NORMAL) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(type, parent, false)) {
    private val container = itemView.find<ViewGroup>(R.id.container)

    @UiThread
    fun loadAd(ad: AdCallback?) {
        if (ad == null) return
        container.implBanner(ad.getID(), true)
    }

    companion object {
        const val TYPE_NORMAL = R.layout.item_ad
        const val TYPE_FAV = R.layout.item_ad_fav
        const val TYPE_ACHIEVEMENT = R.layout.item_ad_achievements
        const val TYPE_NEWS = R.layout.item_ad_news
    }
}