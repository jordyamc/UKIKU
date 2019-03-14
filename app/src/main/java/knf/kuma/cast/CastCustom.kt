package knf.kuma.cast

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.ImageView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import es.munix.multidisplaycast.CastManager
import es.munix.multidisplaycast.interfaces.DialogCallback
import knf.kuma.commons.load

class CastCustom : CastManager() {

    override fun onLoadImage(context: Context, image: String, imageView: ImageView) {
        imageView.load(image)
    }

    override fun getPairingDialog(context: Context, title: String, message: String, positiveText: String?, negativeText: String?, dialogCallback: DialogCallback): Dialog {
        return createDialog(context, null, title, message, positiveText, negativeText, dialogCallback)
                ?: super.getPairingDialog(context, title, message, positiveText, negativeText, dialogCallback)
    }

    override fun getDisconnectDialog(context: Context, customView: View, positiveText: String?, dialogCallback: DialogCallback): Dialog {
        return createDialog(context, customView, null, null, positiveText, null, dialogCallback)
                ?: super.getDisconnectDialog(context, customView, positiveText, dialogCallback)
    }

    override fun getPairingCodeDialog(context: Context, view: View, title: String, positiveText: String?, negativeText: String?, callback: DialogCallback): Dialog {
        return createDialog(context, view, title, null, positiveText, negativeText, callback)
                ?: super.getPairingCodeDialog(context, view, title, positiveText, negativeText, callback)
    }

    private fun createDialog(context: Context, view: View?, title: String?, message: String?, positiveText: String?, negativeText: String?, callback: DialogCallback): Dialog? {
        return try {
            MaterialDialog(context).apply {
                if (!title.isNullOrEmpty()) title(text = title)
                if (!title.isNullOrEmpty()) message(text = message)
                if (view != null) customView(view = view)
                if (!positiveText.isNullOrEmpty())
                    positiveButton(text = positiveText) {
                        callback.onPositive()
                    }
                if (!negativeText.isNullOrEmpty())
                    negativeButton(text = negativeText) {
                        callback.onNegative()
                    }
            }
        } catch (e: Exception) {
            null
        }
    }
}