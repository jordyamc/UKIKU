package knf.kuma.custom

import android.os.Bundle
import android.widget.RelativeLayout
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import es.munix.multidisplaycast.CastControlsActivity
import knf.kuma.App
import knf.kuma.ads.AdsUtils
import knf.kuma.ads.implBanner
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil

class ThemedControlsActivity : CastControlsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
    }

    override fun createImgUrl(url: String): GlideUrl {
        return GlideUrl(url, LazyHeaders.Builder().apply {
            addHeader("Cookie", BypassUtil.getStringCookie(App.context))
            addHeader("User-Agent", BypassUtil.userAgent)
        }.build())
    }

    override fun setUpAd(placeholder: RelativeLayout) {
        if (PrefsUtil.isAdsEnabled)
            placeholder.implBanner(AdsUtils.CAST_BANNER)
    }
}