package knf.kuma.videoservers

import android.content.Context
import android.util.Log
import android.webkit.WebSettings
import knf.kuma.uagen.UAGenerator
import knf.kuma.videoservers.VideoServer.Names.TRANSFERIT
import kotlinx.coroutines.runBlocking

class TransferIt(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("transfer.it")

    override val name: String
        get() = TRANSFERIT

    override val canDownload: Boolean
        get() = true

    override val videoServer: VideoServer?
        get() {
            return try {
                var ua = WebSettings.getDefaultUserAgent(context)
                val url = runBlocking {
                    ua = UAGenerator.getLatestUserAgent()
                    Unpacker.listenResources(
                        context,
                        baseLink,
                        userAgent = ua,
                        onRequest = { it != null && it.contains("api.mega.co.nz") && it.contains("mp4") },
                        timeout = 15000,
                        executeOnFinish = "javascript:setInterval(function(){var el=document.getElementsByClassName('js-download')[0];if(el)el.click();},5000);"
                    )
                }
                Log.e("UPNServer", "url: $url")
                VideoServer(TRANSFERIT,
                        mutableListOf(
                                Option(name, null, url, Headers {
                                    add("User-Agent" to ua)
                                    add("Referer" to "https://transfer.it/")
                                })
                        ), true)
            } catch (e: Exception) {
                null
            }

        }
}
