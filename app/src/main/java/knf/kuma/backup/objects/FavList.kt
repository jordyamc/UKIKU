package knf.kuma.backup.objects

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import knf.kuma.database.CacheDB
import knf.kuma.pojos.FavoriteObject
import xdroid.toaster.Toaster
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

class FavList {
    @SerializedName("response")
    internal var response: String? = null
    @SerializedName("favs")
    internal var favs: MutableList<FavSection>? = null

    internal class FavSection {
        @SerializedName("name")
        var name: String? = null
        @SerializedName("list")
        var list: MutableList<FavEntry>? = null
    }

    internal class FavEntry {
        @SerializedName("title")
        var title: String? = null
        @SerializedName("aid")
        var aid: String? = null
        @SerializedName("section")
        var section: String? = null
        @SerializedName("order")
        var order: Int = 0
    }

    companion object {

        fun decode(inputStream: InputStream): MutableList<FavoriteObject> {
            var totalCount = 0
            var errorCount = 0
            val dao = CacheDB.INSTANCE.animeDAO()
            val favList = Gson().fromJson<FavList>(InputStreamReader(inputStream), object : TypeToken<FavList>() {

            }.type)
            val favs = ArrayList<FavoriteObject>()
            for (section in favList.favs!!) {
                totalCount += section.list!!.size
                for (favEntry in section.list!!) {
                    val animeObject = dao.getByAid(favEntry.aid!!)
                    if (animeObject != null) {
                        val fav = FavoriteObject(animeObject)
                        fav.category = favEntry.section
                        favs.add(fav)
                    } else
                        errorCount++
                }
            }
            Toaster.toast("Migrados correctamente " + (totalCount - errorCount) + "/" + totalCount)
            return favs
        }
    }
}
