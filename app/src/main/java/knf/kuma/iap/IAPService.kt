package knf.kuma.iap

import android.os.Bundle
import android.os.IBinder
import com.appcoins.billing.AppcoinsBilling

class IAPService(binder: IBinder) : BillingService {

    private val service = AppcoinsBilling.Stub.asInterface(binder)

    override fun isBillingSupported(apiVersion: Int, packageName: String, type: String): Int {
        return service.isBillingSupported(apiVersion, packageName, type)
    }

    override fun getSkuDetails(apiVersion: Int, packageName: String, type: String, skusBundle: Bundle): Bundle {
        return service.getSkuDetails(apiVersion, packageName, type, skusBundle)
    }

    override fun getBuyIntent(apiVersion: Int, packageName: String, sku: String, type: String, developerPayload: String): Bundle {
        return service.getBuyIntent(apiVersion, packageName, sku, type, developerPayload)
    }

    override fun getPurchases(apiVersion: Int, packageName: String, type: String, continuationToken: String): Bundle {
        return service.getPurchases(apiVersion, packageName, type, continuationToken)
    }

    override fun consumePurchase(apiVersion: Int, packageName: String, purchaseToken: String): Int {
        return service.consumePurchase(apiVersion, packageName, purchaseToken)
    }

    override fun getBuyIntentToReplaceSkus(apiVersion: Int, packageName: String, oldSkus: List<String>, newSku: String, type: String, developerPayload: String): Bundle {
        return Bundle()
    }
}