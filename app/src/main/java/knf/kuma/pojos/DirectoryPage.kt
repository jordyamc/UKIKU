package knf.kuma.pojos

import android.util.Log
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.isFullMode
import knf.kuma.commons.jsoupCookies
import knf.kuma.database.CacheDB
import knf.kuma.database.dao.AnimeDAO
import pl.droidsonroids.jspoon.Jspoon
import pl.droidsonroids.jspoon.annotation.Selector

class DirectoryPage {
    @Selector(value = "article.Anime.alt.B a.Button.Vrnmlk", attr = "href")
    var links: List<String> = listOf()

    fun getAnimes(animeDAO: AnimeDAO, jspoon: Jspoon, updateInterface: UpdateInterface, isCloudflareActive: Boolean): List<AnimeObject> {
        val animeObjects = ArrayList<AnimeObject>()
        for (link in links) {
            if (Network.isConnected) {
                if (!animeDAO.existLink("%animeflv.net$link")) {
                    try {
                        val response = jsoupCookies("https://animeflv.net$link", true).execute()
                        val body = response.body()
                        if (response.statusCode() == 200 && body != null) {
                            val webInfo =
                                jspoon.adapter(AnimeObject.WebInfo::class.java).fromHtml(body)
                            if (isFullMode && !PrefsUtil.isFamilyFriendly) {
                                animeObjects.add(AnimeObject("https://animeflv.net$link", webInfo))
                                Log.e("Directory Getter", "Added: https://animeflv.net$link")
                            } else {
                                if (webInfo.genres.contains("Ecchi"))
                                    Log.e("Directory Getter", "Skip: https://animeflv.net$link")
                                else {
                                    animeObjects.add(
                                        AnimeObject(
                                            "https://animeflv.net$link",
                                            webInfo
                                        )
                                    )
                                    Log.e("Directory Getter", "Added: https://animeflv.net$link")
                                }
                            }
                            updateInterface.onAdd()
                        } else if (response.statusCode() == 404) {
                            CacheDB.INSTANCE.animeDAO().allLinksInEmission
                        } else check(response.statusCode() < 400) { "Response code: ${response.statusCode()}" }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("Directory Getter", "Error adding: https://animeflv.net" + link + "\nCause: " + e.message)
                        updateInterface.onError()
                    }
                    if (isCloudflareActive)
                        Thread.sleep(5000)
                }
            } else {
                Log.e("Directory Getter", "Abort: No internet")
                break
            }
        }
        return animeObjects
    }

    fun getAnimesRecreate(jspoon: Jspoon, updateInterface: UpdateInterface, isCloudflareActive: Boolean): List<AnimeObject> {
        val animeObjects = ArrayList<AnimeObject>()
        for (link in links) {
            if (Network.isConnected) {
                try {
                    val webInfo = jspoon.adapter(AnimeObject.WebInfo::class.java).fromHtml(jsoupCookies("https://animeflv.net$link").get().outerHtml())
                    if (isFullMode && !PrefsUtil.isFamilyFriendly) {
                        animeObjects.add(AnimeObject("https://animeflv.net$link", webInfo))
                        Log.e("Directory Getter", "Replaced: https://animeflv.net$link")
                    } else {
                        if (webInfo.genres.contains("Ecchi"))
                            Log.e("Directory Getter", "Skip: https://animeflv.net$link")
                        else {
                            animeObjects.add(AnimeObject("https://animeflv.net$link", webInfo))
                            Log.e("Directory Getter", "Replaced: https://animeflv.net$link")
                        }
                    }
                    updateInterface.onAdd()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("Directory Getter", "Error replacing: https://animeflv.net" + link + "\nCause: " + e.message)
                    updateInterface.onError()
                }
                if (isCloudflareActive)
                    Thread.sleep(5000)
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
