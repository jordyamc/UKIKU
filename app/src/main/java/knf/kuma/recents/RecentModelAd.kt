package knf.kuma.recents

import com.google.android.gms.ads.nativead.NativeAd

data class RecentModelAd(val id: Int, val unifiedNativeAd: NativeAd) : RecentModel()