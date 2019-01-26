package knf.kuma.iap

import android.os.Bundle
import knf.kuma.App
import knf.kuma.commons.noCrash
import knf.kuma.iap.IAPWrapper.Companion.responseCode
import org.json.JSONObject

class Inventory(service: BillingService) {

    val purchaseList = mutableMapOf<String, Purchase>()
    val skuList = mutableMapOf<String, SkuDetails>()

    init {
        loadPurchases(service)
        loadSkuDetails(service)
    }

    private fun isPurchased(sku: String): Boolean {
        return purchaseList.containsKey(sku)
    }

    fun addPurchase(purchase: Purchase) {
        purchaseList[purchase.sku] = purchase
    }

    private fun loadPurchases(service: BillingService) {
        var continuationToken: String? = ""
        do {
            noCrash {
                val bundle = service.getPurchases(3, App.context.packageName, IAPWrapper.ITEM_TYPE_INAPP, continuationToken
                        ?: "")
                if (bundle.responseCode != IAPWrapper.BILLING_RESPONSE_RESULT_OK)
                    return@noCrash
                val ownedSkus = bundle.getStringArrayList(IAPWrapper.RESPONSE_INAPP_ITEM_LIST)
                val purchaseDatas = bundle.getStringArrayList(IAPWrapper.RESPONSE_INAPP_PURCHASE_DATA_LIST)
                val signatureList = bundle.getStringArrayList(IAPWrapper.RESPONSE_INAPP_SIGNATURE_LIST)
                val idsList = bundle.getStringArrayList(IAPWrapper.RESPONSE_INAPP_PURCHASE_ID_LIST)
                if (purchaseDatas != null && signatureList != null && idsList != null)
                    ownedSkus?.forEachIndexed { index, _ ->
                        val purchase = Purchase(idsList[index], purchaseDatas[index], signatureList[index])
                        purchaseList[purchase.sku] = purchase
                    }
                continuationToken = bundle.getString(IAPWrapper.INAPP_CONTINUATION_TOKEN)
            }
        } while (!continuationToken.isNullOrEmpty())
    }

    private fun loadSkuDetails(service: BillingService) {
        val skuBundle = Bundle().apply {
            //putStringArrayList(IAPWrapper.GET_SKU_DETAILS_ITEM_LIST, toStringList(purchaseList.keys))
            putStringArrayList(IAPWrapper.GET_SKU_DETAILS_ITEM_LIST, arrayListOf("ee_2", "ee_3", "ee_4", "ee_all"))
        }
        val bundle = service.getSkuDetails(3, App.context.packageName, IAPWrapper.ITEM_TYPE_INAPP, skuBundle)
        if (bundle.containsKey(IAPWrapper.RESPONSE_GET_SKU_DETAILS_LIST)) {
            val detailsList = bundle.getStringArrayList(IAPWrapper.RESPONSE_GET_SKU_DETAILS_LIST)
                    ?: arrayListOf()
            detailsList.forEach {
                val skuDetails = SkuDetails(it)
                skuList[skuDetails.sku] = skuDetails
            }
        }
    }

    private fun toStringList(set: Set<String>): ArrayList<String> {
        val list = arrayListOf<String>()
        set.forEach { list.add(it) }
        return list
    }

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

    data class SkuDetails(val data: String) {
        val sku: String
        val type: String
        val price: String
        val priceAmountMicros: Long
        val priceCurrencyCode: String
        val title: String
        val description: String

        init {
            val json = JSONObject(data)
            sku = json.optString("productId")
            type = json.optString("type")
            price = json.optString("price")
            priceAmountMicros = json.optLong("price_amount_micros")
            priceCurrencyCode = json.optString("price_currency_code")
            title = json.optString("title")
            description = json.optString("description")
        }
    }


}