package knf.kuma.cast

import android.content.Context
import android.graphics.Bitmap
import es.munix.multidisplaycast.helpers.NotificationsHelper
import knf.kuma.commons.PicassoSingle
import java.io.ByteArrayOutputStream

class CastNotificationHelper : NotificationsHelper() {

    override fun getBitmap(context: Context?, link: String): ByteArray {
        return try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            PicassoSingle.get().load(link).get().compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            byteArrayOutputStream.toByteArray()
        }catch (e:Exception){
            super.getBitmap(context, link)
        }
    }
}