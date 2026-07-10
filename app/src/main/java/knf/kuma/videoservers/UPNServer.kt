package knf.kuma.videoservers

import android.content.Context
import android.util.Log
import knf.kuma.videoservers.VideoServer.Names.UPNServer
import kotlinx.coroutines.runBlocking
import java.util.regex.Pattern

class UPNServer(context: Context, baseLink: String) : Server(context, baseLink) {

    override val isValid: Boolean
        get() = baseLink.contains("animeav1.uns.bio")

    override val name: String
        get() = UPNServer

    override val canDownload: Boolean
        get() = false

    override val videoServer: VideoServer?
        get() {
            return try {
                val url = runBlocking {
                    Unpacker.listenResources(context, baseLink, Pattern.compile(".*master.m3u8.*"), 15000, executeOnFinish = "javascript:setInterval(function(){var el=document.getElementById('player-button-container');if(el)el.click();},100);")
                }
                Log.e("UPNServer", "url: $url")
                VideoServer(UPNServer,
                        mutableListOf(
                                Option(name, null, url, Headers {
                                    add(0, "Referer" to "https://animeav1.uns.bio/")
                                }),
                        ))
            } catch (e: Exception) {
                null
            }

        }
}
