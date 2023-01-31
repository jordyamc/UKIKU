package knf.kuma.directory

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUIGlobal
import knf.kuma.commons.noCrash
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import org.json.JSONObject
import java.net.URL

object DirManager {

    fun checkPreDir(forced: Boolean = false) {
        if (forced || CacheDB.INSTANCE.animeDAO().count < 3200) {
            doOnUIGlobal {
                DirectoryService.setStatus(DirectoryService.STATE_CACHED)
            }
            noCrash {
                val info = JSONObject(URL("https://ukiku.app/dirs/directoryInfo.json").readText())
                for (index in 0..6) {
                    val json = info.getJSONObject(index.toString())
                    if (!CacheDB.INSTANCE.animeDAO().hasRange(json.getString("idF"), json.getString("idL"))) {
                        val sliceJson = URL("https://ukiku.app/dirs/directory$index.json").readText()
                        val list: List<AnimeObject> = Gson().fromJson(sliceJson, object : TypeToken<List<AnimeObject>>() {}.type)
                        CacheDB.INSTANCE.animeDAO().insertAll(list)
                    }
                }
                PrefsUtil.isDirectoryFinished = true
            }
        }
    }

}