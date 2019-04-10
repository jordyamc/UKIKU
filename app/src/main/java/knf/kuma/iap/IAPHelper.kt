package knf.kuma.iap

import android.content.Context
import android.content.Intent
import android.net.Uri


object IAPHelper {
    fun hasWalletInstalled(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.parse("ethereum:")
        intent.data = uri
        return hasHandlerAvailable(intent, context)
    }

    private fun hasHandlerAvailable(intent: Intent, context: Context): Boolean {
        val manager = context.packageManager
        val infos = manager.queryIntentActivities(intent, 0)
        return infos.isNotEmpty()
    }
}