package knf.kuma.pojos

import android.util.Log
import knf.kuma.commons.Network
import knf.kuma.commons.execute
import knf.kuma.commons.jsoupCookies
import knf.kuma.commons.okHttpCookies
import knf.kuma.database.dao.AnimeDAO
import pl.droidsonroids.jspoon.Jspoon
import pl.droidsonroids.jspoon.annotation.Selector
import java.util.*

class DirectoryPage {
    @Selector(value = "article.Anime.alt.B a.Button.Vrnmlk", attr = "href")
    var links: List<String> = listOf()

    fun getAnimes(animeDAO: AnimeDAO, jspoon: Jspoon, updateInterface: UpdateInterface): List<AnimeObject> {
        val animeObjects = ArrayList<AnimeObject>()
        for (link in links) {
            if (Network.isConnected) {
                if (!animeDAO.existLink("https://animeflv.net$link"))
                    try {
                        val response = okHttpCookies("https://animeflv.net$link").execute(followRedirects = false)
                        val body = response.body()?.string()
                        if (response.code() == 200 && body != null) {
                            val webInfo = jspoon.adapter(AnimeObject.WebInfo::class.java).fromHtml(body)
                            animeObjects.add(AnimeObject("https://animeflv.net$link", webInfo))
                            Log.e("Directory Getter", "Added: https://animeflv.net$link")
                            updateInterface.onAdd()
                        } else check(response.code() < 400) { "Response code: ${response.code()}" }
                    } catch (e: Exception) {
                        Log.e("Directory Getter", "Error adding: https://animeflv.net" + link + "\nCause: " + e.message)
                        updateInterface.onError()
                    }

            } else {
                Log.e("Directory Getter", "Abort: No internet")
                break
            }
        }
        return animeObjects
    }

    fun getAnimesRecreate(jspoon: Jspoon, updateInterface: UpdateInterface): List<AnimeObject> {
        val animeObjects = ArrayList<AnimeObject>()
        for (link in links) {
            if (Network.isConnected) {
                try {
                    val webInfo = jspoon.adapter(AnimeObject.WebInfo::class.java).fromHtml(jsoupCookies("https://animeflv.net$link").get().outerHtml())
                    animeObjects.add(AnimeObject("https://animeflv.net$link", webInfo))
                    Log.e("Directory Getter", "Replaced: https://animeflv.net$link")
                    updateInterface.onAdd()
                } catch (e: Exception) {
                    Log.e("Directory Getter", "Error replacing: https://animeflv.net" + link + "\nCause: " + e.message)
                    updateInterface.onError()
                }

            } else {
                Log.e("Directory Getter", "Abort: No internet")
                break
            }
        }
        return animeObjects
    }

    interface UpdateInterface {
        fun onAdd()

        fun onError()
    }
}
