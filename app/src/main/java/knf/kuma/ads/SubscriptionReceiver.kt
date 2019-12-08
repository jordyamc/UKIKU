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
            val purchaseTime: Long = 0L
    )

    fun check(intent: Intent) {
        GlobalScope.launch(Dispatchers.IO) {
            if (intent.hasExtra("token"))
                check(intent.getStringExtra("token"))
            else
                check(PrefsUtil.subscriptionToken)
        }
    }

    private suspend fun check(token: String?) {
        if (token == null || !Network.isConnected) return
        val status = checkStatus(token)
        if (status.isVerified) {
            PrefsUtil.subscriptionToken = token
        } else {
            PrefsUtil.subscriptionToken = null
            PrefsUtil.subscriptionOrderId = null
            if (!PrefsUtil.isAdsEnabled)
                FirestoreManager.doSignOut(App.context)
        }
    }

    suspend fun checkStatus(token: String): VerifyStatus = withContext(Dispatchers.IO) {
        val json = JSONObject(URL("https://nuclient-verification.herokuapp.com/subscriptions.php?token=${token}").readText())
        VerifyStatus(json.getBoolean("isVerified"), json.getBoolean("isActive"))
    }
}