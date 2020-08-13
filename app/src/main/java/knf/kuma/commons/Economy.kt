package knf.kuma.commons

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import knf.kuma.App
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.backup.firestore.FirestoreManager
import kotlinx.android.synthetic.main.dialog_wallet.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import xdroid.toaster.Toaster.toast

val Int.coins: String get() = "$this loli-coin${if (this == 1) "" else "s"}"

object Economy {

    private val coinsLiveData = MutableLiveData(PrefsUtil.userCoins)
    val rewardedVideoLiveData = MutableLiveData(PrefsUtil.userRewardedVideoCount)

    fun reward(isAdClicked: Boolean = false, baseReward: Int = 1) {
        doOnUIException(onLog = { FirebaseCrashlytics.getInstance().recordException(it);toast("Error al obtener loli-coins\n${it.message}") }) {
            val reward = diceOf<Int> {
                put(baseReward, if (isAdClicked) 80.0 else 90.0)
                put(baseReward + 1, if (isAdClicked) 15.0 else 8.0)
                put(baseReward + 2, if (isAdClicked) 5.0 else 2.0)
            }
            doAsync { repeat(reward) { FirebaseAnalytics.getInstance(App.context).logEvent("Coins_generated", Bundle()) } }
            PrefsUtil.userCoins = (PrefsUtil.userCoins + reward).also { coinsLiveData.value = it }
            PrefsUtil.userRewardedVideoCount = (PrefsUtil.userRewardedVideoCount + 1).also { rewardedVideoLiveData.value = it }
            FirestoreManager.updateTop()
            AchievementManager.incrementCount(reward, listOf(46, 47, 48, 49, 50, 51))
            toast("Has ganado ${reward.coins}!!!!!")
        }
    }

    fun buy(price: Int): Boolean {
        val total = PrefsUtil.userCoins
        return if (total >= price) {
            PrefsUtil.userCoins = (total - price).also { doOnUI { coinsLiveData.value = it } }
            doAsync { repeat(price) { FirebaseAnalytics.getInstance(App.context).logEvent("Coins_used", Bundle()) } }
            true
        } else false
    }

    fun showWallet(activity: FragmentActivity, themed: Boolean = false, onShow: () -> Unit) {
        doOnUI {
            val view = activity.layoutInflater.inflate(R.layout.dialog_wallet, null)
            if (themed) {
                view.coinsCount.setTextColor(ContextCompat.getColor(activity, EAHelper.getThemeColorLight()))
                view.backgroundTop.setBackgroundColor(ContextCompat.getColor(activity, EAHelper.getThemeColorLight()))
                view.backgroundBottom.setBackgroundColor(ContextCompat.getColor(activity, EAHelper.getThemeColor()))
            }
            view.coinsCount.text = PrefsUtil.userCoins.toString()
            coinsLiveData.observe(activity, Observer {
                doOnUI {
                    view.coinsCount.text = it.toString()
                }
            })
            view.showAdButton.onClick { onShow() }
            AlertDialog.Builder(activity).apply {
                setView(view)
            }.show()
        }
    }
}