package knf.kuma.commons

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.tools.kprobability.item
import knf.tools.kprobability.probabilityOf
import kotlinx.android.synthetic.main.dialog_wallet.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import xdroid.toaster.Toaster

val Int.coins: String get() = "$this loli-coin${if (this == 1) "" else "s"}"

object Economy {

    fun reward(isAdClicked: Boolean = false) {
        doOnUI {
            val reward = probabilityOf<Int> {
                item(1, if (isAdClicked) 80.0 else 90.0)
                item(2, if (isAdClicked) 15.0 else 8.0)
                item(3, if (isAdClicked) 5.0 else 2.0)
            }.random()
            Answers.getInstance().logCustom(CustomEvent("Coins generated").putCustomAttribute("coins", reward))
            PrefsUtil.coins += reward
            AchievementManager.incrementCount(reward, listOf(46, 47, 48, 49, 50, 51))
            Toaster.toast("Has ganado ${reward.coins}!!!!!")
        }
    }

    fun buy(price: Int): Boolean {
        val total = PrefsUtil.coins
        return if (total >= price) {
            PrefsUtil.coins -= price
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
        view.coinsCount.text = PrefsUtil.coins.toString()
        PrefsUtil.coinsLive.observe(activity, Observer {
            doOnUI {
                view.coinsCount.text = if (it.isEmpty()) "0" else it.decrypt(BuildConfig.CIPHER_PWD)
            }
        })
        view.showAdButton.onClick { onShow() }
        AlertDialog.Builder(activity).apply {
            setView(view)
        }.show()
    }
}