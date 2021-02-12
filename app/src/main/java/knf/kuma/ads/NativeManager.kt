package knf.kuma.ads

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import knf.kuma.App
import knf.kuma.commons.Network
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object NativeManager {
    private var isLoading = false
    private var internalSize = 0
    private val adsChannel = Channel<NativeAd>(Int.MAX_VALUE)

    init {
        cacheAds()
    }

    suspend fun take(scope: CoroutineScope, size: Int, tryCount: Int = 0, callback: TakeCallback) {
        val operation = suspend {
            scope.launch{
                if (internalSize >= size) {
                    launch(Dispatchers.Main){
                        callback(suspendCoroutine {
                            launch {
                                val pendingList = mutableListOf<NativeAd>()
                                repeat(size) {
                                    internalSize--
                                    pendingList.add(adsChannel.receive())
                                }
                                it.resume(pendingList)
                            }
                        })
                    }
                    if (internalSize <= 5)
                        cacheAds()
                }else {
                    take(scope, size, tryCount + 1, callback)
                }
            }
            Unit
        }
        when {
            withContext(Dispatchers.IO) {Network.isAdsBlocked } -> callback(emptyList())
            internalSize >= size -> operation()
            isLoading -> {
                withContext(Dispatchers.IO) {
                    while (isLoading)
                        delay(500)
                    if (tryCount > 3)
                        callback(emptyList())
                    else
                        take(scope, size, tryCount + 1, callback)
                }
            }
            else -> {
                cacheAds(scope, size, operation)
            }
        }
    }

    private fun cacheAds(scope: CoroutineScope = GlobalScope, pendingSize: Int = 5, pending: PendingCallback = {}) {
        if (isLoading) return
        GlobalScope.launch(Dispatchers.Main) {
            isLoading = true
            var loader: AdLoader? = null
            var isCallbackCalled = false
            loader = AdLoader.Builder(App.context, AdsUtilsMob.LIST_NATIVE)
                    .forNativeAd {
                        GlobalScope.launch {
                            internalSize++
                            adsChannel.send(it)
                            if (internalSize >= pendingSize) {
                                isCallbackCalled = true
                                scope.launch { pending() }
                            }
                            if (loader?.isLoading == false) {
                                isLoading = false
                                if (!isCallbackCalled)
                                    scope.launch { pending() }
                            }
                        }
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(p0: LoadAdError?) {
                            super.onAdFailedToLoad(p0)
                            if (loader?.isLoading == false) {
                                isLoading = false
                                launch {
                                    if (!isCallbackCalled)
                                        scope.launch { pending() }
                                }
                            }
                        }
                    }).build()
            loader?.loadAds(AdsUtilsMob.adRequest, 5)
        }
    }
}

typealias PendingCallback = suspend () -> Unit
typealias TakeCallback = (List<NativeAd>) -> Unit