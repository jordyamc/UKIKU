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
import knf.kuma.pojos.AnimeObject
import org.jetbrains.anko.doAsync
import java.io.IOException
import java.util.*

class AnimeLoad : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        doAsync {
            try {
                if (searchIcon == null)
                    searchIcon = IconCompat.createWithBitmap(getBitmap(Objects.requireNonNull<Drawable>(ContextCompat.getDrawable(context, R.drawable.ic_search_black_24dp)) as VectorDrawable))
                if (openIcon == null)
                    openIcon = IconCompat.createWithBitmap(getBitmap(Objects.requireNonNull<Drawable>(ContextCompat.getDrawable(context, R.drawable.ic_open)) as VectorDrawable))
                if (QUERY != "/anime/") {
                    LIST = CacheDB.createAndGet(context).animeDAO().getByName("%" + QUERY.replace("/anime/", "").trim { it <= ' ' } + "%")
                    for (animeObject in LIST) {
                        try {
                            animeObject.icon = IconCompat.createWithBitmap(PicassoSingle[context].load(PatternUtil.getCover(animeObject.aid!!)).get())
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
        var LIST: MutableList<AnimeObject> = ArrayList()
        var openIcon: IconCompat? = null
        var searchIcon: IconCompat? = null

        private fun getBitmap(vectorDrawable: VectorDrawable): Bitmap {
            val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
                    vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
            vectorDrawable.draw(canvas)
            return bitmap
        }
    }
}
