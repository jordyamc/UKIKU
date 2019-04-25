package knf.kuma.ads

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import kotlinx.android.synthetic.main.item_ad.view.*


class AdCardItemHolder(parent: ViewGroup, @LayoutRes type: Int = TYPE_NORMAL) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(type, parent, false)) {
    private val container = itemView.container

    @UiThread
    fun loadAd(ad: AdCallback?) {
        if (ad == null) return
        container.implBannerBrains(ad.getID())
    }

    companion object {
        const val TYPE_NORMAL = R.layout.item_ad
        const val TYPE_FAV = R.layout.item_ad_fav
    }
}