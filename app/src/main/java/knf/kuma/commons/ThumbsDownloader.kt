package knf.kuma.commons

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

object ThumbsDownloader {
    fun start(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            val thumbs = context.getExternalFilesDir("thumbs")
            for (id in 1..3500) {
                val result = try {
                    val thumb = File(thumbs, "$id.jpg")
                    if (!thumb.exists()) {
                        val bitmap = PicassoSingle.get()
                            .load("https://www3.animeflv.net/uploads/animes/thumbs/$id.jpg").get()
                        thumb.createNewFile()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(thumb))
                    }
                    delay(100)
                    true
                } catch (e: Exception) {
                    false
                }
                Log.e("Thumb", "Download id $id, success: $result")
            }
        }
    }
}