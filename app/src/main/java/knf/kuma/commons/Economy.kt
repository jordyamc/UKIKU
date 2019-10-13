package knf.kuma.commons

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.tools.kprobability.item
import knf.tools.kprobability.probabilityOf
import kotlinx.android.synthetic.main.dialog_wallet.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import xdroid.toaster.Toaster.toast

val Int.coins: String get() = "$this loli-coin${if (this == 1) "" else "s"}"

object Economy {

    private val coinsLiveData = MutableLiveData(PrefsUtil.userCoins)

    fun reward(isAdClicked: Boolean = false) {
        doOnUIException(onLog = { Crashlytics.logException(it);toast("Error al obtener loli-coins\n${it.message}") }) {
            val reward = probabilityOf<Int> {
                item(1, if (isAdClicked) 80.0 else 90.0)
                item(2, if (isAdClicked) 15.0 else 8.0)
                item(3, if (isAdClicked) 5.0 else 2.0)
            }.random()
            doAsync { repeat(reward) { Answers.getInstance().logCustom(CustomEvent("Coins generated")) } }
            PrefsUtil.userCoins = (PrefsUtil.userCoins + reward).also { coinsLiveData.value = it }
            PrefsUtil.rewardedVideoCount += 1
            AchievementManager.incrementCount(reward, listOf(46, 47, 48, 49, 50, 51))
            toast("Has ganado ${reward.coins}!!!!!")
        }
    }

    fun buy(price: Int): Boolean {
        val total = PrefsUtil.userCoins
        return if (total >= price) {
            PrefsUtil.userCoins -= price
            doAsync { repeat(price) { Answers.getInstance().logCustom(CustomEvent("Coins used")) } }
            true
        } else false
    }

    fun showWallet(activity: AppCompatActivity, themed: Boolean = false, onShow: () -> Unit) {
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