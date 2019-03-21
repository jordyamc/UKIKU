package knf.kuma.iap

import android.os.Bundle
import android.os.IBinder
import com.appcoins.billing.AppcoinsBilling

class IAPService(binder: IBinder) : BillingService {

    private val service = AppcoinsBilling.Stub.asInterface(binder)

    override fun isBillingSupported(apiVersion: Int, packageName: String, type: String): Int {
        return try {
            service.isBillingSupported(apiVersion, packageName, type)
        }catch (e:Exception){
            6
        }
    }

    override fun getSkuDetails(apiVersion: Int, packageName: String, type: String, skusBundle: Bundle): Bundle {
        return try {
            service.getSkuDetails(apiVersion, packageName, type, skusBundle)
        } catch (e: Exception) {
            Bundle()
        }
    }

    override fun getBuyIntent(apiVersion: Int, packageName: String, sku: String, type: String, developerPayload: String): Bundle {
        return try {
            service.getBuyIntent(apiVersion, packageName, sku, type, developerPayload)
        }catch (e:Exception){
            Bundle()
        }
    }

    override fun getPurchases(apiVersion: Int, packageName: String, type: String, continuationToken: String): Bundle {
        return try {
            service.getPurchases(apiVersion, packageName, type, continuationToken)
        }catch (e:Exception){
            Bundle()
        }
    }

    override fun consumePurchase(apiVersion: Int, packageName: String, purchaseToken: String): Int {
        return try {
            service.consumePurchase(apiVersion, packageName, purchaseToken)
        }catch (e:Exception){
            6
        }
    }

    override fun getBuyIntentToReplaceSkus(apiVersion: Int, packageName: String, oldSkus: List<String>, newSku: String, type: String, developerPayload: String): Bundle {
        return Bundle()
    }
}