package knf.kuma.retrofit

import knf.kuma.commons.JsExtractor
import knf.kuma.commons.Network
import knf.kuma.database.CacheDB
import knf.kuma.pojos.av1.DirectoryAV1
import knf.kuma.pojos.av1.RecentAV1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object Repository {
    fun reloadRecents() {
        if (Network.isConnected) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val array = JsExtractor.processLink("https://animeav1.com/")
                    val recents = mutableListOf<RecentAV1>()
                    for (i in 0 until array!!.length()) {
                        recents.add(RecentAV1.fromJson(i, array.getJSONObject(i)))
                    }
                    CacheDB.INSTANCE.recentAV1DAO().setCache(recents)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getDirectory(slug: String) : DirectoryAV1? {
        if (!Network.isConnected) return  null
        return try {
            runBlocking {
                val data = JsExtractor.processLink("https://animeav1.com/media/$slug")!!.getJSONObject(0)
                val dir = DirectoryAV1.fromJson(data)
                CacheDB.INSTANCE.directoryDAO().add(dir)
                dir
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
