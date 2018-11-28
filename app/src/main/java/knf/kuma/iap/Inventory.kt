package knf.kuma.iap

import android.os.Bundle
import knf.kuma.App
import knf.kuma.commons.noCrash
import org.json.JSONObject

class Inventory(service: BillingService) {

    val purchaseList = mutableListOf<Purchase>()

    init {
        loadPurchases(service)
    }

    private fun loadPurchases(service: BillingService) {
        var continuationToken: String? = ""
        do {
            noCrash {
                val bundle = service.getPurchases(3, App.context.packageName, IAPWrapper.ITEM_TYPE_INAPP, continuationToken!!)
                if (bundle.responseCode != IAPWrapper.BILLING_RESPONSE_RESULT_OK)
                    return@noCrash
                val ownedSkus = bundle.getStringArrayList(IAPWrapper.RESPONSE_INAPP_ITEM_LIST)
                val purchaseDatas = bundle.getStringArrayList(IAPWrapper.RESPONSE_INAPP_PURCHASE_DATA_LIST)
                val signatureList = bundle.getStringArrayList(IAPWrapper.RESPONSE_INAPP_SIGNATURE_LIST)
                val idsList = bundle.getStringArrayList(IAPWrapper.RESPONSE_INAPP_PURCHASE_ID_LIST)
                ownedSkus?.forEachIndexed { index, _ ->
                    purchaseList.add(Purchase(idsList!![index], purchaseDatas!![index], signatureList!![index]))
                }
                continuationToken = bundle.getString(IAPWrapper.INAPP_CONTINUATION_TOKEN)
            }
        } while (!continuationToken.isNullOrEmpty())
    }

    private val Bundle.responseCode: Int
        get() = get(IAPWrapper.RESPONSE_CODE)?.toString()?.toInt() ?: -1

    data class Purchase(val id: String, val data: String, val signature: String) {
        val orderID: String
        val packageName: String
        val sku: String
        val purchaseTime: Long
        val purchaseState: Int
        val developerPayload: String
        val token: String

        init {
            val json = JSONObject(data)
            orderID = json.optString("orderId")
            packageName = json.optString("packageName")
            sku = json.optString("productId")
            purchaseTime = json.optLong("purchaseTime")
            purchaseState = json.optInt("purchaseState")
            developerPayload = json.optString("developerPayload")
            token = json.optString("token", json.optString("purchaseToken"))
            if (!Security.verifyPurchase(data, signature))
                throw IllegalStateException("Error verifying purchase")
        }
    }


}