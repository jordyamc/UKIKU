package knf.kuma.commons

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.preference.PreferenceManager
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.achievements.AchievementManager
import knf.kuma.ads.AdsUtils
import knf.kuma.ads.FullscreenAdLoader
import knf.kuma.ads.getFAdLoaderInterstitial
import knf.kuma.ads.getFAdLoaderRewarded
import knf.kuma.backup.firestore.syncData
import knf.kuma.custom.GenericActivity
import knf.kuma.database.EADB
import knf.kuma.iap.IAPWrapper
import knf.kuma.iap.Inventory
import knf.kuma.iap.PayloadHelper
import knf.kuma.pojos.EAObject
import knf.tools.kprobability.item
import knf.tools.kprobability.probabilityOf
import kotlinx.android.synthetic.main.activity_ea.*
import kotlinx.android.synthetic.main.item_ea_step.view.*
import moe.feng.common.stepperview.IStepperAdapter
import moe.feng.common.stepperview.VerticalStepperItemView
import org.jetbrains.anko.sdk27.coroutines.onClick
import xdroid.toaster.Toaster
import java.util.*

object EAHelper {
    private val CODE1: String by lazy {
        PreferenceManager.getDefaultSharedPreferences(App.context).getString("ea_code1", null)
                ?: generate(App.context, "ea_code1", arrayOf("R", "F", "D", "C"))
    }
    private val CODE2: String by lazy {
        PreferenceManager.getDefaultSharedPreferences(App.context).getString("ea_code2", null)
                ?: generate(App.context, "ea_code2", arrayOf("1", "2", "3", "4", "5", "6", "7"))
    }
    private var CURRENT_1 = ""
    private var CURRENT_2 = ""

    val isPart0Unlocked: Boolean
        get() = EADB.INSTANCE.eaDAO().isUnlocked(0)

    val isPart1Unlocked: Boolean
        get() = EADB.INSTANCE.eaDAO().isUnlocked(1)

    val isPart2Unlocked: Boolean
        get() = EADB.INSTANCE.eaDAO().isUnlocked(2)

    val isPart3Unlocked: Boolean
        get() = EADB.INSTANCE.eaDAO().isUnlocked(3)
    val isAllUnlocked: Boolean
        get() = isPart0Unlocked && isPart1Unlocked && isPart2Unlocked && isPart3Unlocked

    val phase: Int
        get() = when {
            isPart3Unlocked -> 4
            isPart2Unlocked -> 3
            isPart1Unlocked -> 2
            isPart0Unlocked -> 1
            else -> 0
        }

    val eaMessage: String?
        get() = when {
            isPart3Unlocked -> "Disfruta de la recompensa"
            isPart2Unlocked -> "El tesoro esta en Akihabara"
            isPart1Unlocked -> "LMMJVSD \u2192 US \u2192 $CODE2"
            isPart0Unlocked -> CODE1
            else -> "\u26B2 easteregg"
        }

    fun getMessage(phase: Int): String {
        return when (phase) {
            4 -> "Disfruta de la recompensa"
            3 -> "El tesoro esta en Akihabara"
            2 -> "LMMJVSD \u2192 US \u2192 $CODE2"
            1 -> CODE1
            else -> "\u26B2 easteregg"
        }
    }

    fun checkStart(query: String) {
        if (phase == 0 && query == BuildConfig.EASTER_SEARCH) {
            Toaster.toastLong(CODE1)
            setUnlocked(0)
        }
    }

    private fun generate(context: Context, key: String, array: Array<String>): String {
        val builder = StringBuilder()
        for (i in 0..9) {
            builder.append(array[Random().nextInt(array.size)])
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, builder.toString()).apply()
        return builder.toString()
    }

    fun enter1(part: String) {
        if (isPart0Unlocked && phase == 1) {
            CURRENT_1 += part
            if (CURRENT_1 == CODE1) {
                setUnlocked(1)
                Toaster.toastLong("LMMJVSD \u2192 US \u2192 $CODE2")
                clear1()
            } else if (!CODE1.startsWith(CURRENT_1)) {
                clear1()
                CURRENT_1 += part
            }
        }
    }

    fun clear1() {
        CURRENT_1 = ""
    }

    fun enter2(part: String) {
        if (isPart1Unlocked && phase == 2) {
            CURRENT_2 += part
            if (CURRENT_2 == CODE2) {
                setUnlocked(2)
                Toaster.toastLong("El tesoro esta en Akihabara")
                clear2()
            } else if (!CODE2.startsWith(CURRENT_2)) {
                clear2()
                CURRENT_2 += part
            }
        }
    }

    fun clear2() {
        CURRENT_2 = ""
    }

    internal fun enter3() {
        if (isPart2Unlocked && phase == 3) {
            setUnlocked(3)
        }
    }

    fun setUnlocked(phase: Int) {
        EADB.INSTANCE.eaDAO().unlock(EAObject(phase))
        AchievementManager.onPhaseUnlocked(phase)
        syncData { ea() }
    }

    @StyleRes
    fun getTheme(): Int {
        return noCrashLet(R.style.AppTheme_NoActionBar) {
            if (!isPart0Unlocked || !isPart1Unlocked || !isPart2Unlocked or !isPart3Unlocked)
                R.style.AppTheme_DayNight
            when (PrefsUtil.themeColor) {
                "0" -> R.style.AppTheme_DayNight
                "1" -> R.style.AppTheme_Pink
                "2" -> R.style.AppTheme_Purple
                "3" -> R.style.AppTheme_DeepPurple
                "4" -> R.style.AppTheme_Indigo
                "5" -> R.style.AppTheme_Blue
                "6" -> R.style.AppTheme_LightBlue
                "7" -> R.style.AppTheme_Cyan
                "8" -> R.style.AppTheme_Teal
                "9" -> R.style.AppTheme_Green
                "10" -> R.style.AppTheme_LightGreen
                "11" -> R.style.AppTheme_Lime
                "12" -> R.style.AppTheme_Yellow
                "13" -> R.style.AppTheme_Amber
                "14" -> R.style.AppTheme_Orange
                "15" -> R.style.AppTheme_DeepOrange
                "16" -> R.style.AppTheme_Brown
                "17" -> R.style.AppTheme_Gray
                "18" -> R.style.AppTheme_BlueGray
                else -> R.style.AppTheme_DayNight
            }
        }
    }

    @StyleRes
    fun getThemeNA(): Int {
        return noCrashLet(R.style.AppTheme_NoActionBar) {
            if (!isPart0Unlocked || !isPart1Unlocked || !isPart2Unlocked or !isPart3Unlocked)
                R.style.AppTheme_NoActionBar
            when (PrefsUtil.themeColor) {
                "0" -> R.style.AppTheme_NoActionBar
                "1" -> R.style.AppTheme_NoActionBar_Pink
                "2" -> R.style.AppTheme_NoActionBar_Purple
                "3" -> R.style.AppTheme_NoActionBar_DeepPurple
                "4" -> R.style.AppTheme_NoActionBar_Indigo
                "5" -> R.style.AppTheme_NoActionBar_Blue
                "6" -> R.style.AppTheme_NoActionBar_LightBlue
                "7" -> R.style.AppTheme_NoActionBar_Cyan
                "8" -> R.style.AppTheme_NoActionBar_Teal
                "9" -> R.style.AppTheme_NoActionBar_Green
                "10" -> R.style.AppTheme_NoActionBar_LightGreen
                "11" -> R.style.AppTheme_NoActionBar_Lime
                "12" -> R.style.AppTheme_NoActionBar_Yellow
                "13" -> R.style.AppTheme_NoActionBar_Amber
                "14" -> R.style.AppTheme_NoActionBar_Orange
                "15" -> R.style.AppTheme_NoActionBar_DeepOrange
                "16" -> R.style.AppTheme_NoActionBar_Brown
                "17" -> R.style.AppTheme_NoActionBar_Gray
                "18" -> R.style.AppTheme_NoActionBar_BlueGray
                else -> R.style.AppTheme_NoActionBar
            }
        }
    }

    @StyleRes
    fun getThemeDialog(): Int {
        return noCrashLet(R.style.AppTheme_Dialog_Base) {
            if (!isPart0Unlocked || !isPart1Unlocked || !isPart2Unlocked or !isPart3Unlocked)
                R.style.AppTheme_Dialog_Base
            when (PrefsUtil.themeColor) {
                "0" -> R.style.AppTheme_Dialog_Base
                "1" -> R.style.AppTheme_Dialog_Pink
                "2" -> R.style.AppTheme_Dialog_Purple
                "3" -> R.style.AppTheme_Dialog_DeepPurple
                "4" -> R.style.AppTheme_Dialog_Indigo
                "5" -> R.style.AppTheme_Dialog_Blue
                "6" -> R.style.AppTheme_Dialog_LightBlue
                "7" -> R.style.AppTheme_Dialog_Cyan
                "8" -> R.style.AppTheme_Dialog_Teal
                "9" -> R.style.AppTheme_Dialog_Green
                "10" -> R.style.AppTheme_Dialog_LightGreen
                "11" -> R.style.AppTheme_Dialog_Lime
                "12" -> R.style.AppTheme_Dialog_Yellow
                "13" -> R.style.AppTheme_Dialog_Amber
                "14" -> R.style.AppTheme_Dialog_Orange
                "15" -> R.style.AppTheme_Dialog_DeepOrange
                "16" -> R.style.AppTheme_Dialog_Brown
                "17" -> R.style.AppTheme_Dialog_Gray
                "18" -> R.style.AppTheme_Dialog_BlueGray
                else -> R.style.AppTheme_Dialog_Base
            }
        }
    }

    @DrawableRes
    fun getThemeImg(): Int {
        return noCrashLet(getThemeImg("0")) {
            if (!isPart0Unlocked || !isPart1Unlocked || !isPart2Unlocked or !isPart3Unlocked)
                getThemeImg("0")
            getThemeImg(PrefsUtil.themeColor)
        }
    }

    @DrawableRes
    fun getThemeImg(value: String): Int {
        when (value) {
            "0" -> return R.drawable.side_nav_bar
            "1" -> return R.drawable.side_nav_bar_pink
            "2" -> return R.drawable.side_nav_bar_purple
            "3" -> return R.drawable.side_nav_bar_deep_purple
            "4" -> return R.drawable.side_nav_bar_indigo
            "5" -> return R.drawable.side_nav_bar_blue
            "6" -> return R.drawable.side_nav_bar_light_blue
            "7" -> return R.drawable.side_nav_bar_cyan
            "8" -> return R.drawable.side_nav_bar_teal
            "9" -> return R.drawable.side_nav_bar_green
            "10" -> return R.drawable.side_nav_bar_light_green
            "11" -> return R.drawable.side_nav_bar_lime
            "12" -> return R.drawable.side_nav_bar_yellow
            "13" -> return R.drawable.side_nav_bar_amber
            "14" -> return R.drawable.side_nav_bar_orange
            "15" -> return R.drawable.side_nav_bar_deep_orange
            "16" -> return R.drawable.side_nav_bar_brown
            "17" -> return R.drawable.side_nav_bar_gray
            "18" -> return R.drawable.side_nav_bar_blue_gray
            else -> return R.drawable.side_nav_bar
        }
    }

    @ColorRes
    fun getThemeColor(): Int {
        return noCrashLet(getThemeColor("0")) {
            if (!isPart0Unlocked || !isPart1Unlocked || !isPart2Unlocked or !isPart3Unlocked)
                getThemeColor("0")
            getThemeColor(PrefsUtil.themeColor)
        }
    }

    @ColorRes
    fun getThemeColor(value: String): Int {
        when (value) {
            "0" -> return R.color.colorAccent
            "1" -> return R.color.colorAccentPink
            "2" -> return R.color.colorAccentPurple
            "3" -> return R.color.colorAccentDeepPurple
            "4" -> return R.color.colorAccentIndigo
            "5" -> return R.color.colorAccentBlue
            "6" -> return R.color.colorAccentLightBlue
            "7" -> return R.color.colorAccentCyan
            "8" -> return R.color.colorAccentTeal
            "9" -> return R.color.colorAccentGreen
            "10" -> return R.color.colorAccentLightGreen
            "11" -> return R.color.colorAccentLime
            "12" -> return R.color.colorAccentYellow
            "13" -> return R.color.colorAccentAmber
            "14" -> return R.color.colorAccentOrange
            "15" -> return R.color.colorAccentDeepOrange
            "16" -> return R.color.colorAccentBrown
            "17" -> return R.color.colorAccentGray
            "18" -> return R.color.colorAccentBlueGrey
            else -> return R.color.colorAccent
        }
    }

    @ColorRes
    fun getThemeColorLight(): Int {
        return noCrashLet(getThemeColorLight("0")) {
            if (!isPart0Unlocked || !isPart1Unlocked || !isPart2Unlocked or !isPart3Unlocked)
                getThemeColorLight("0")
            getThemeColorLight(PrefsUtil.themeColor)
        }
    }

    @ColorRes
    fun getThemeColorLight(value: String): Int {
        when (value) {
            "0" -> return R.color.colorAccentLight
            "1" -> return R.color.colorAccentPinkLight
            "2" -> return R.color.colorAccentPurpleLight
            "3" -> return R.color.colorAccentDeepPurpleLight
            "4" -> return R.color.colorAccentIndigoLight
            "5" -> return R.color.colorAccentBlueLight
            "6" -> return R.color.colorAccentLightBlueLight
            "7" -> return R.color.colorAccentCyanLight
            "8" -> return R.color.colorAccentTealLight
            "9" -> return R.color.colorAccentGreenLight
            "10" -> return R.color.colorAccentLightGreenLight
            "11" -> return R.color.colorAccentLimeLight
            "12" -> return R.color.colorAccentYellowLight
            "13" -> return R.color.colorAccentAmberLight
            "14" -> return R.color.colorAccentOrangeLight
            "15" -> return R.color.colorAccentDeepOrangeLight
            "16" -> return R.color.colorAccentBrownLight
            "17" -> return R.color.colorAccentGrayLight
            "18" -> return R.color.colorAccentBlueGreyLight
            else -> return R.color.colorAccentLight
        }
    }
}

class EAUnlockActivity : GenericActivity(), IStepperAdapter {

    private val iapWrapper: IAPWrapper by lazy { IAPWrapper(this) }
    private val rewardedAd: FullscreenAdLoader by lazy { getFAdLoaderRewarded(this) }
    private var interstitial: FullscreenAdLoader = getFAdLoaderInterstitial(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ea)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.title = "Easter egg"
        toolbar.setNavigationOnClickListener { onBackPressed() }
        rewardedAd.load()
        interstitial.load()
    }

    override fun onResume() {
        super.onResume()
        iapWrapper.setUp {
            if (it)
                invalidateOptionsMenu()
            doOnUI {
                progress.visibility = View.GONE
                vertical_stepper_view.stepperAdapter = this@EAUnlockActivity
                vertical_stepper_view.currentStep = checkPurchases()
            }
        }
    }

    private fun showAd() {
        probabilityOf<() -> Unit> {
            item({ rewardedAd.show() }, AdsUtils.remoteConfigs.getDouble("rewarded_percent"))
            item({ interstitial.show() }, AdsUtils.remoteConfigs.getDouble("interstitial_percent"))
        }.random()()
    }

    private fun checkPurchases(): Int {
        return if (!iapWrapper.isEnabled)
            EAHelper.phase
        else {
            val inventory = iapWrapper.inventory
            if (inventory.isPurchased(getSkuCode(0)) || inventory.isPurchased(getSkuCode(3))) {
                for (i in 0..3) EAHelper.setUnlocked(i)
                4
            } else if (inventory.isPurchased(getSkuCode(2))) {
                for (i in 0..2) EAHelper.setUnlocked(i)
                3
            } else if (inventory.isPurchased(getSkuCode(1))) {
                for (i in 0..1) EAHelper.setUnlocked(i)
                2
            } else
                EAHelper.phase
        }
    }

    private fun Inventory?.isPurchased(sku: String): Boolean {
        return this?.purchaseList?.containsKey(sku) ?: false
    }

    override fun onDestroy() {
        iapWrapper.onDestroy()
        super.onDestroy()
    }

    override fun getTitle(index: Int): CharSequence {
        return "Paso ${index + 1}"
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateCustomView(index: Int, context: Context?, view: VerticalStepperItemView?): View {
        val inflateView = LayoutInflater.from(context).inflate(R.layout.item_ea_step, view, false)
        val hint = inflateView.hint
        hint.text = EAHelper.getMessage(index)
        val unlockButton = inflateView.unlock
        val unlockCoinsButton = inflateView.unlockCoins
        if (index == 0 || index == 4) {
            inflateView.layBuy.visibility = View.GONE
        } else {
            if (iapWrapper.isEnabled) {
                val details = iapWrapper.inventory?.skuList?.let { it[getSkuCode(index)] }
                unlockButton.text = details?.let { "${details.price} ${details.priceCurrencyCode}" }
                        ?: getPurchaseInfo(index)
                if (!iapWrapper.isAvailable)
                    unlockButton.isEnabled = false
            } else
                unlockButton.text = getPurchaseInfo(index)
            unlockCoinsButton.text = getPurchaseCost(index).toString()
            unlockButton.onClick {
                if (!iapWrapper.isEnabled)
                    iapWrapper.showInstallDialog()
                else
                    if (iapWrapper.launchPurchaseFlow(this@EAUnlockActivity,
                                    getSkuCode(index), PayloadHelper.buildIntentPayload(BuildConfig.APPCOINS_ADDRESS, null)))
                        block_view.visibility = View.VISIBLE
            }
            unlockCoinsButton.onClick {
                doOnUI {
                    if (Economy.buy(getPurchaseCost(index))) {
                        unlock(index)
                        Toaster.toast("Compra realizada")
                    } else
                        Toaster.toast("No tienes suficientes loli-coins!!")
                }
            }
        }
        return inflateView
    }

    private fun getSkuCode(index: Int): String {
        return when (index) {
            1 -> "ee_2"
            2 -> "ee_3"
            3 -> "ee_4"
            else -> "ee_all"
        }
    }

    private fun getPurchaseInfo(index: Int): String {
        return when (index) {
            1 -> "$ 7 usd"
            2 -> "$ 13 usd"
            3 -> "$ 5 usd"
            else -> "..."
        }
    }

    private fun getPurchaseCost(index: Int): Int {
        return when (index) {
            1 -> 700
            2 -> 1300
            3 -> 500
            else -> 9999
        }
    }

    private fun unlock(index: Int) {
        unlock(getSkuCode(index))
    }

    private fun unlock(sku: String?) {
        when (sku) {
            "ee_2" -> {
                EAHelper.setUnlocked(1)
                vertical_stepper_view.nextStep()
            }
            "ee_3" -> {
                EAHelper.setUnlocked(2)
                vertical_stepper_view.nextStep()
            }
            "ee_4" -> {
                EAHelper.setUnlocked(3)
                vertical_stepper_view.nextStep()
            }
            "ee_all" -> {
                EAHelper.setUnlocked(1)
                EAHelper.setUnlocked(2)
                EAHelper.setUnlocked(3)
                vertical_stepper_view.currentStep = 4
                invalidateOptionsMenu()
            }
        }
    }

    override fun getSummary(index: Int): CharSequence? {
        return null
    }

    override fun size(): Int {
        return 5
    }

    override fun onShow(index: Int) {

    }

    override fun onHide(index: Int) {

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (EAHelper.phase != 4)
            menuInflater.inflate(R.menu.menu_ea, menu)
        if (iapWrapper.isEnabled && !iapWrapper.isAvailable)
            menu?.findItem(R.id.unlock)?.isEnabled = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.unlock -> {
                if (!iapWrapper.isEnabled)
                    iapWrapper.showInstallDialog()
                else
                    if (iapWrapper.launchPurchaseFlow(this@EAUnlockActivity,
                                    getSkuCode(0), PayloadHelper.buildIntentPayload(BuildConfig.APPCOINS_ADDRESS, null)))
                        block_view.visibility = View.VISIBLE
            }
            R.id.coins -> {
                Economy.showWallet(this) {
                    showAd()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        block_view.visibility = View.GONE
        iapWrapper.handleActivityResult(requestCode, resultCode, data) { success, sku ->
            if (success)
                unlock(sku)
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, EAUnlockActivity::class.java))
        }
    }
}