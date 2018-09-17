package knf.kuma.commons

import android.content.Context
import android.preference.PreferenceManager
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.LevelEndEvent
import com.crashlytics.android.answers.LevelStartEvent
import knf.kuma.R
import knf.kuma.database.EADB
import knf.kuma.pojos.EAObject
import xdroid.toaster.Toaster
import java.util.*

object EAHelper {
    private var CODE1: String? = null
    private var CODE2: String? = null
    private var CURRENT_1 = ""
    private var CURRENT_2 = ""

    private val isPart0Unlocked: Boolean
        get() = EADB.INSTANCE!!.eaDAO().isUnlocked(0)!!

    private val isPart1Unlocked: Boolean
        get() = EADB.INSTANCE!!.eaDAO().isUnlocked(1)!!

    private val isPart2Unlocked: Boolean
        get() = EADB.INSTANCE!!.eaDAO().isUnlocked(2)!!

    private val isPart3Unlocked: Boolean
        get() = EADB.INSTANCE!!.eaDAO().isUnlocked(3)!!

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
            isPart1Unlocked -> "LMMJVSD \u2192 US \u2192 " + CODE2!!
            isPart0Unlocked -> CODE1
            else -> "\u26B2 easteregg"
        }

    fun init(context: Context) {
        val manager = PreferenceManager.getDefaultSharedPreferences(context)
        CODE1 = manager.getString("ea_code1", null)
        CODE2 = manager.getString("ea_code2", null)
        if (CODE1 == null)
            CODE1 = generate(context, "ea_code1", arrayOf("R", "F", "D", "C"))
        if (CODE2 == null)
            CODE2 = generate(context, "ea_code2", arrayOf("1", "2", "3", "4", "5", "6", "7"))
    }

    fun checkStart(query: String) {
        if (phase == 0 && query == "easteregg") {
            Toaster.toastLong(CODE1)
            Answers.getInstance().logLevelStart(LevelStartEvent().putLevelName("Easter Egg"))
            Answers.getInstance().logLevelStart(LevelStartEvent().putLevelName("Easter Egg Phase 1"))
            setUnlocked(0)
        }
    }

    private fun generate(context: Context, key: String, array: Array<String>): String {
        val builder = StringBuilder()
        for (i in 0..9) {
            builder.append(array[Random().nextInt(array.size - 1)])
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, builder.toString()).apply()
        return builder.toString()
    }

    fun enter1(part: String) {
        if (isPart0Unlocked && phase == 1) {
            CURRENT_1 += part
            if (CURRENT_1 == CODE1) {
                setUnlocked(1)
                Toaster.toastLong("LMMJVSD \u2192 US \u2192 " + CODE2!!)
                clear1()
                Answers.getInstance().logLevelEnd(LevelEndEvent().putLevelName("Easter Egg Phase 1").putScore(0))
                Answers.getInstance().logLevelStart(LevelStartEvent().putLevelName("Easter Egg Phase 2"))
            } else if (!CODE1!!.startsWith(CURRENT_1)) {
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
                Answers.getInstance().logLevelEnd(LevelEndEvent().putLevelName("Easter Egg Phase 2").putScore(0))
                Answers.getInstance().logLevelStart(LevelStartEvent().putLevelName("Easter Egg Phase 3"))
            } else if (!CODE2!!.startsWith(CURRENT_2)) {
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
            Answers.getInstance().logLevelEnd(LevelEndEvent().putLevelName("Easter Egg Phase 3").putScore(0))
            Answers.getInstance().logLevelEnd(LevelEndEvent().putLevelName("Easter Egg").putScore(0))
        }
    }

    private fun setUnlocked(phase: Int) {
        EADB.INSTANCE!!.eaDAO().unlock(EAObject(phase))
    }

    @StyleRes
    fun getTheme(context: Context?): Int {
        if (context == null || !isPart0Unlocked || !isPart1Unlocked || !isPart2Unlocked or !isPart3Unlocked)
            return R.style.AppTheme
        when (PrefsUtil.themeColor) {
            "0" -> return R.style.AppTheme
            "1" -> return R.style.AppTheme_Pink
            "2" -> return R.style.AppTheme_Purple
            "3" -> return R.style.AppTheme_DeepPurple
            "4" -> return R.style.AppTheme_Indigo
            "5" -> return R.style.AppTheme_Blue
            "6" -> return R.style.AppTheme_LightBlue
            "7" -> return R.style.AppTheme_Cyan
            "8" -> return R.style.AppTheme_Teal
            "9" -> return R.style.AppTheme_Green
            "10" -> return R.style.AppTheme_LightGreen
            "11" -> return R.style.AppTheme_Lime
            "12" -> return R.style.AppTheme_Yellow
            "13" -> return R.style.AppTheme_Amber
            "14" -> return R.style.AppTheme_Orange
            "15" -> return R.style.AppTheme_DeepOrange
            "16" -> return R.style.AppTheme_Brown
            "17" -> return R.style.AppTheme_Gray
            "18" -> return R.style.AppTheme_BlueGray
            else -> return R.style.AppTheme
        }
    }

    @StyleRes
    fun getThemeNA(context: Context?): Int {
        if (context == null || !isPart0Unlocked || !isPart1Unlocked || !isPart2Unlocked or !isPart3Unlocked)
            return R.style.AppTheme_NoActionBar
        when (PrefsUtil.themeColor) {
            "0" -> return R.style.AppTheme_NoActionBar
            "1" -> return R.style.AppTheme_NoActionBar_Pink
            "2" -> return R.style.AppTheme_NoActionBar_Purple
            "3" -> return R.style.AppTheme_NoActionBar_DeepPurple
            "4" -> return R.style.AppTheme_NoActionBar_Indigo
            "5" -> return R.style.AppTheme_NoActionBar_Blue
            "6" -> return R.style.AppTheme_NoActionBar_LightBlue
            "7" -> return R.style.AppTheme_NoActionBar_Cyan
            "8" -> return R.style.AppTheme_NoActionBar_Teal
            "9" -> return R.style.AppTheme_NoActionBar_Green
            "10" -> return R.style.AppTheme_NoActionBar_LightGreen
            "11" -> return R.style.AppTheme_NoActionBar_Lime
            "12" -> return R.style.AppTheme_NoActionBar_Yellow
            "13" -> return R.style.AppTheme_NoActionBar_Amber
            "14" -> return R.style.AppTheme_NoActionBar_Orange
            "15" -> return R.style.AppTheme_NoActionBar_DeepOrange
            "16" -> return R.style.AppTheme_NoActionBar_Brown
            "17" -> return R.style.AppTheme_NoActionBar_Gray
            "18" -> return R.style.AppTheme_NoActionBar_BlueGray
            else -> return R.style.AppTheme_NoActionBar
        }
    }

    @StyleRes
    fun getThemeDialog(context: Context?): Int {
        if (context == null || !isPart0Unlocked || !isPart1Unlocked || !isPart2Unlocked or !isPart3Unlocked)
            return R.style.AppTheme_NoActionBar
        when (PrefsUtil.themeColor) {
            "0" -> return R.style.AppTheme_Dialog_Base
            "1" -> return R.style.AppTheme_Dialog_Pink
            "2" -> return R.style.AppTheme_Dialog_Purple
            "3" -> return R.style.AppTheme_Dialog_DeepPurple
            "4" -> return R.style.AppTheme_Dialog_Indigo
            "5" -> return R.style.AppTheme_Dialog_Blue
            "6" -> return R.style.AppTheme_Dialog_LightBlue
            "7" -> return R.style.AppTheme_Dialog_Cyan
            "8" -> return R.style.AppTheme_Dialog_Teal
            "9" -> return R.style.AppTheme_Dialog_Green
            "10" -> return R.style.AppTheme_Dialog_LightGreen
            "11" -> return R.style.AppTheme_Dialog_Lime
            "12" -> return R.style.AppTheme_Dialog_Yellow
            "13" -> return R.style.AppTheme_Dialog_Amber
            "14" -> return R.style.AppTheme_Dialog_Orange
            "15" -> return R.style.AppTheme_Dialog_DeepOrange
            "16" -> return R.style.AppTheme_Dialog_Brown
            "17" -> return R.style.AppTheme_Dialog_Gray
            "18" -> return R.style.AppTheme_Dialog_BlueGray
            else -> return R.style.AppTheme_Dialog_Base
        }
    }

    @DrawableRes
    fun getThemeImg(context: Context?): Int {
        if (context == null || !isPart0Unlocked || !isPart1Unlocked || !isPart2Unlocked or !isPart3Unlocked)
            return R.drawable.side_nav_bar
        when (PrefsUtil.themeColor) {
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
    fun getThemeColor(context: Context?): Int {
        if (context == null || !isPart0Unlocked || !isPart1Unlocked || !isPart2Unlocked or !isPart3Unlocked)
            return R.color.colorAccent
        when (PrefsUtil.themeColor) {
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
    fun getThemeColorLight(context: Context?): Int {
        if (context == null || !isPart0Unlocked || !isPart1Unlocked || !isPart2Unlocked or !isPart3Unlocked)
            return R.color.colorAccentLight
        when (PrefsUtil.themeColor) {
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