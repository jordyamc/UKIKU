package knf.kuma.ads

import com.appodeal.ads.NativeAd
import com.appodeal.ads.NativeCallbacks

abstract class AbstractNativeCallbacks : NativeCallbacks {
    override fun onNativeLoaded() {
    }

    override fun onNativeClicked(p0: NativeAd?) {
    }

    override fun onNativeFailedToLoad() {
    }

    override fun onNativeShown(p0: NativeAd?) {
    }

    override fun onNativeShowFailed(p0: NativeAd?) {
    }

    override fun onNativeExpired() {
    }
}