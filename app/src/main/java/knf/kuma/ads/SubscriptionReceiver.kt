package knf.kuma.ads

import android.content.Intent
import knf.kuma.App
import knf.kuma.backup.firestore.FirestoreManager
import knf.kuma.commons.Network
import knf.kuma.commons.PrefsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object SubscriptionReceiver {

    data class VerifyStatus(val isVerified: Boolean = false, val isActive: Boolean = false)

    data class SubscriptionInfo(
            val token: String = "",
            val orderId: String = "",
            val purchaseTime: Long = 0L
    )

    fun check(intent: Intent) {
        GlobalScope.launch(Dispatchers.IO) {
            if (intent.hasExtra("orderId") && intent.hasExtra("token"))
                check(intent.getStringExtra("token"), intent.getStringExtra("orderId"))
            else
                check(PrefsUtil.subscriptionToken, PrefsUtil.subscriptionOrderId)
        }
    }

    private suspend fun check(token: String?, orderId: String?) {
        if (token == null || orderId == null || !Network.isConnected) return
        val status = checkStatus(token, orderId)
        if (status.isVerified) {
            PrefsUtil.subscriptionToken = token
            PrefsUtil.subscriptionOrderId = orderId
        } else {
            PrefsUtil.subscriptionToken = null
            PrefsUtil.subscriptionOrderId = null
            if (!PrefsUtil.isAdsEnabled)
                FirestoreManager.doSignOut(App.context)
        }
    }

    suspend fun checkStatus(token: String, orderId: String): VerifyStatus = withContext(Dispatchers.IO) {
        val json = JSONObject(URL(String.format("https://nuclient-verification.herokuapp.com/subscriptions.php?token=%s&orderid=%s", token, orderId)).readText())
        VerifyStatus(json.getBoolean("isVerified"), json.getBoolean("isActive"))
    }
}