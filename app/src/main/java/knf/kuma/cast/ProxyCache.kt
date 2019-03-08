package knf.kuma.cast

import knf.kuma.App
import knf.kuma.commons.SelfServer
import knf.libs.videocache.HttpProxyCacheServer

object ProxyCache {
    private val cacheServer: HttpProxyCacheServer by lazy { HttpProxyCacheServer(App.context) }

    fun start(url: String): String {
        return if (cacheServer.isCached(url))
            SelfServer.start(cacheServer.getProxyUrl(url), true) ?: ""
        else
            cacheServer.getProxyUrl(url, false)
    }
}