package knf.kuma.iap

import android.content.Context


object IAPHelper {
    fun hasWalletInstalled(context: Context): Boolean {
        /*val intent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.parse("ethereum:")
        intent.data = uri
        return hasHandlerAvailable(intent, context)*/
        return false
    }
}