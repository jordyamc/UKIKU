package knf.kuma.iap

import android.os.Bundle
import android.os.RemoteException


interface BillingService {

    @Throws(RemoteException::class)
    fun isBillingSupported(apiVersion: Int, packageName: String, type: String): Int

    @Throws(RemoteException::class)
    fun getSkuDetails(apiVersion: Int, packageName: String, type: String, skusBundle: Bundle): Bundle

    @Throws(RemoteException::class)
    fun getBuyIntent(apiVersion: Int, packageName: String, sku: String, type: String,
                     developerPayload: String): Bundle

    @Throws(RemoteException::class)
    fun getPurchases(apiVersion: Int, packageName: String, type: String, continuationToken: String): Bundle

    @Throws(RemoteException::class)
    fun consumePurchase(apiVersion: Int, packageName: String, purchaseToken: String): Int

    @Throws(RemoteException::class)
    fun getBuyIntentToReplaceSkus(apiVersion: Int, packageName: String, oldSkus: List<String>, newSku: String, type: String, developerPayload: String): Bundle

}