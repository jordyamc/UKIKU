package knf.kuma.slices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import knf.kuma.R
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PicassoSingle
import knf.kuma.database.CacheDB
import org.jetbrains.anko.doAsync
import java.io.IOException
import java.util.*

class AnimeLoad : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        doAsync {
            try {
                initIcons(context)
                if (QUERY != "/anime/") {
                    LIST = CacheDB.createAndGet(context).animeDAO().getByName("%" + QUERY.replace("/anime/", "").trim() + "%")
                    for (animeObject in LIST) {
                        try {
                            animeObject.icon = IconCompat.createWithBitmap(PicassoSingle.get().load(PatternUtil.getCover(animeObject.aid)).get())
                        } catch (e: IOException) {
                            animeObject.icon = IconCompat.createWithResource(context, R.mipmap.ic_launcher)
                        }

                    }
                    context.contentResolver.notifyChange(Uri.parse("content://knf.kuma.slices$QUERY"), null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        var QUERY = "/anime/"
        var LIST: MutableList<AnimeSliceObject> = ArrayList()
        lateinit var openIcon: IconCompat
        lateinit var searchIcon: IconCompat

        private fun initIcons(context: Context) {
            if (!::searchIcon.isInitialized)
                searchIcon = IconCompat.createWithBitmap(getBitmap(ContextCompat.getDrawable(context, R.drawable.ic_search_black_24dp)))
            if (!::openIcon.isInitialized)
                openIcon = IconCompat.createWithBitmap(getBitmap(ContextCompat.getDrawable(context, R.drawable.ic_open)))
        }

        private fun getBitmap(vectorDrawable: Drawable?): Bitmap? {
            if (vectorDrawable !is VectorDrawable) return null
            val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
                    vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
            vectorDrawable.draw(canvas)
            return bitmap
        }
    }
}
