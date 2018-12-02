package knf.kuma.iap

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.NonNull
import com.afollestad.materialdialogs.MaterialDialog
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.commons.EAHelper
import knf.kuma.commons.safeShow
import xdroid.toaster.Toaster


class IAPWrapper(private val context: Context) : ServiceConnection {
    val isEnabled: Boolean = IAPHelper.hasWalletInstalled(context)
    private var iapService: IAPService? = null
    var inventory: Inventory? = null
    private var onConnectedListener: ((success: Boolean) -> Unit)? = null

    val isAvailable get() = iapService != null && inventory != null

    fun setUp(onConnect: (success: Boolean) -> Unit) {
        onConnectedListener = onConnect
        if (isEnabled && EAHelper.phase < 4)
            context.bindService(Intent(BuildConfig.IAB_BIND_ACTION).setPackage(BuildConfig.IAB_BIND_PACKAGE), this, Context.BIND_AUTO_CREATE)
        else onConnect.invoke(false)
    }

    fun onDestroy() {
        if (isEnabled && isAvailable)
            try {
                context.unbindService(this)
            } finally {
                inventory = null
            }
    }

    fun showInstallDialog() {
        MaterialDialog(context).safeShow {
            message(text = "Para hacer compras en UKIKU, se necesita la cartera AppCoin DBS")
            positiveButton(text = "Instalar") {
                goToStore(context)
            }
            negativeButton(text = "Cancelar")
        }
    }

    @NonNull
    private fun goToStore(activity: Context) {
        val appPackageName = "com.appcoins.wallet"
        try {
            activity.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
        } catch (anfe: ActivityNotFoundException) {
            activity.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
        }

    }

    override fun onServiceDisconnected(name: ComponentName?) {
        iapService = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        iapService = IAPService(service!!).also {
            inventory = Inventory(it)
            if (it.isBillingSupported(3, context.packageName, ITEM_TYPE_INAPP) == BILLING_RESPONSE_RESULT_OK)
                onConnectedListener?.invoke(true)
            else
                onConnectedListener?.invoke(false)
        }
    }

    fun launchPurchaseFlow(activity: Activity, sku: String, extraData: String): Boolean {
        try {
            val intentBundle = iapService?.getBuyIntent(3, App.context.packageName, sku, ITEM_TYPE_INAPP, extraData)
            if (intentBundle?.responseCode == BILLING_RESPONSE_RESULT_OK) {
                val pendingIntent = intentBundle.getParcelable<PendingIntent>(RESPONSE_BUY_INTENT)
                        ?: return false
                activity.startIntentSenderForResult(pendingIntent.intentSender, PURCHASE_CODE, Intent(), 0, 0, 0)
                return true
            } else
                Toaster.toast("Error al iniciar compra")
            return false
        } catch (e: Exception) {
            Toaster.toast("Error al iniciar compra")
            return false
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?, callback: (success: Boolean, sku: String?) -> Unit) {
        if (requestCode != PURCHASE_CODE) {
            callback.invoke(false, null)
            return
        }
        if (data == null) {
            Log.e("Purchase", "Null data")
            callback.invoke(false, null)
            return
        }
        val purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA)
        val dataSignature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE)
        val id = data.getStringExtra(RESPONSE_INAPP_PURCHASE_ID)
        if (resultCode == Activity.RESULT_OK && data.extras?.responseCode == BILLING_RESPONSE_RESULT_OK) {
            if (purchaseData == null || dataSignature == null) {
                Log.e("Purchase", "Data or signature null")
                callback.invoke(false, null)
                return
            }
            val purchase = Inventory.Purchase(id, purchaseData, dataSignature)
            if (!Security.verifyPurchase(purchaseData, dataSignature)) {
                Toaster.toast("Verificacion de firma fallida")
                Log.e("Purchase", "Signature verification failed")
                callback.invoke(false, null)
                return
            }
            inventory?.addPurchase(purchase)
            callback.invoke(false, purchase.sku)
        }
    }

    companion object {
        const val BILLING_RESPONSE_RESULT_OK = 0
        const val BILLING_RESPONSE_RESULT_USER_CANCELED = 1
        const val BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE = 2
        const val BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3
        const val BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4
        const val BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5
        const val BILLING_RESPONSE_RESULT_ERROR = 6
        const val BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7
        const val BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8

        // IAB Helper error codes
        const val IABHELPER_ERROR_BASE = -1000
        const val IABHELPER_REMOTE_EXCEPTION = -1001
        const val IABHELPER_BAD_RESPONSE = -1002
        const val IABHELPER_VERIFICATION_FAILED = -1003
        const val IABHELPER_SEND_INTENT_FAILED = -1004
        const val IABHELPER_USER_CANCELLED = -1005
        const val IABHELPER_UNKNOWN_PURCHASE_RESPONSE = -1006
        const val IABHELPER_MISSING_TOKEN = -1007
        const val IABHELPER_UNKNOWN_ERROR = -1008
        const val IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE = -1009
        const val IABHELPER_INVALID_CONSUMPTION = -1010
        const val IABHELPER_SUBSCRIPTION_UPDATE_NOT_AVAILABLE = -1011

        // Keys for the responses from InAppBillingService
        const val RESPONSE_CODE = "RESPONSE_CODE"
        const val RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST"
        const val RESPONSE_BUY_INTENT = "BUY_INTENT"
        const val RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA"
        const val RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE"
        const val RESPONSE_INAPP_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST"
        const val RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST"
        const val RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST"
        const val INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN"

        const val RESPONSE_INAPP_PURCHASE_ID_LIST = "INAPP_PURCHASE_ID_LIST"
        const val RESPONSE_INAPP_PURCHASE_ID = "INAPP_PURCHASE_ID"

        // Item types
        const val ITEM_TYPE_INAPP = "inapp"
        const val ITEM_TYPE_SUBS = "subs"

        // some fields on the getSkuDetails response bundle
        const val GET_SKU_DETAILS_ITEM_LIST = "ITEM_ID_LIST"
        const val GET_SKU_DETAILS_ITEM_TYPE_LIST = "ITEM_TYPE_LIST"

        const val PURCHASE_CODE = 6745

        val Bundle.responseCode: Int
            get() = get(RESPONSE_CODE)?.toString()?.toInt() ?: -1
    }
}