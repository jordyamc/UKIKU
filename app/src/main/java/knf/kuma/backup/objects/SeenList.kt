package knf.kuma.backup.objects

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import knf.kuma.database.CacheDB
import knf.kuma.pojos.SeenObject
import xdroid.toaster.Toaster
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

/**
 * Created by jordy on 01/03/2018.
 */

class SeenList {
    @SerializedName("response")
    internal var response: String? = null
    @SerializedName("vistos")
    internal var vistos: String? = null
    internal var list: MutableList<SeenObj>? = null

    private fun deserialize() {
        list = ArrayList()
        Log.e("Seen", vistos)
        val els = vistos?.replace("E", "")?.split(":::".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
        for (el in els ?: emptyArray()) {
            if (el != "") {
                val spl = el.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                list?.add(SeenObj(spl[0], spl[1]))
            }
        }
        list?.sort()
    }

    internal inner class SeenObj(var aid: String, var num: String) : Comparable<SeenObj> {

        override fun compareTo(other: SeenObj): Int {
            val bname = aid.compareTo(other.aid)
            return if (bname != 0) {
                bname
            } else {
                num.compareTo(other.num)
            }
        }
    }

    companion object {

        fun decode(inputStream: InputStream?): MutableList<SeenObject>? {
            if (inputStream == null) return null
            var errorCount = 0
            val dao = CacheDB.INSTANCE.animeDAO()
            val seenList = Gson().fromJson<SeenList>(InputStreamReader(inputStream), object : TypeToken<SeenList>() {

            }.type)
            seenList.deserialize()
            val totalCount = seenList.list?.size ?: 0
            val chapters = ArrayList<SeenObject>()
            var animeObject: AnimeChapters? = null
            for (obj in seenList.list ?: listOf<SeenObj>()) {
                try {
                    if (animeObject == null || animeObject.aid != obj.aid)
                        animeObject = dao.getChaptersByAid(obj.aid)
                    val chapterList = animeObject.chaptersList()
                    var found = false
                    for (chapter in chapterList) {
                        try {
                            if (chapter.number.endsWith(" " + obj.num)) {
                                chapters.add(SeenObject.fromChapter(chapter))
                                found = true
                                break
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    if (!found)
                        errorCount++
                } catch (e: Exception) {
                    errorCount++
                }
            }
            Toaster.toast("Migrados correctamente " + (totalCount - errorCount) + "/" + totalCount)
            return chapters
        }
    }
}
