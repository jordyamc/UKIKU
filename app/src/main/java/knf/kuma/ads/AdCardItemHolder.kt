package knf.kuma.ads

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import knf.kuma.R
import kotlinx.android.synthetic.main.item_ad.view.*
import xdroid.core.Global.getResources


class AdCardItemHolder(parent: ViewGroup, @LayoutRes type: Int = TYPE_NORMAL) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(type, parent, false)) {
    private val container = itemView.container

    @UiThread
    fun loadAd(ad: AdCallback?) {
        if (ad == null) return
        val dm = getResources().displayMetrics
        val density = (dm.density * 160).toDouble()
        val x = Math.pow(dm.widthPixels / density, 2.0)
        val y = Math.pow(dm.heightPixels / density, 2.0)
        val screenInches = Math.sqrt(x + y)
        val adSize = when {
            screenInches > 8 -> // > 728 X 90
                AdSize.LEADERBOARD
            screenInches > 6 -> // > 468 X 60
                AdSize.MEDIUM_RECTANGLE
            else -> // > 320 X 50
                AdSize.BANNER
        }
        container.removeAllViews()
        val adView = AdView(container.context)
        adView.adSize = adSize
        adView.adUnitId = ad.getID()
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    companion object {
        const val TYPE_NORMAL = R.layout.item_ad
        const val TYPE_FAV = R.layout.item_ad_fav
    }
}