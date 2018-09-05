package knf.kuma.commons;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.LevelEndEvent;
import com.crashlytics.android.answers.LevelStartEvent;

import org.jetbrains.annotations.Nullable;

import java.util.Random;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;
import knf.kuma.R;
import knf.kuma.database.EADB;
import knf.kuma.pojos.EAObject;
import xdroid.toaster.Toaster;

public class EAHelper {
    private static String CODE1;
    private static String CODE2;
    private static String CURRENT_1 = "";
    private static String CURRENT_2 = "";

    public static void init(Context context) {
        SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(context);
        CODE1 = manager.getString("ea_code1", null);
        CODE2 = manager.getString("ea_code2", null);
        if (CODE1 == null)
            CODE1 = generate(context, "ea_code1", new String[]{"R", "F", "D", "C"});
        if (CODE2 == null)
            CODE2 = generate(context, "ea_code2", new String[]{"1", "2", "3", "4", "5", "6", "7"});
    }

    public static void checkStart(String query) {
        if (getPhase() == 0 && query.equals("easteregg")) {
            Toaster.toastLong(CODE1);
            Answers.getInstance().logLevelStart(new LevelStartEvent().putLevelName("Easter Egg"));
            Answers.getInstance().logLevelStart(new LevelStartEvent().putLevelName("Easter Egg Phase 1"));
            setUnlocked(0);
        }
    }

    private static String generate(Context context, String key, String[] array) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            builder.append(array[new Random().nextInt(array.length - 1)]);
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, builder.toString()).apply();
        return builder.toString();
    }

    public static void enter1(String part) {
        if (isPart0Unlocked() && getPhase() == 1) {
            CURRENT_1 += part;
            if (CURRENT_1.equals(CODE1)) {
                setUnlocked(1);
                Toaster.toastLong("LMMJVSD \u2192 US \u2192 " + CODE2);
                clear1();
                Answers.getInstance().logLevelEnd(new LevelEndEvent().putLevelName("Easter Egg Phase 1").putScore(0));
                Answers.getInstance().logLevelStart(new LevelStartEvent().putLevelName("Easter Egg Phase 2"));
            } else if (!CODE1.startsWith(CURRENT_1)) {
                clear1();
                CURRENT_1 += part;
            }
        }
    }

    public static void clear1() {
        CURRENT_1 = "";
    }

    public static void enter2(String part) {
        if (isPart1Unlocked() && getPhase() == 2) {
            CURRENT_2 += part;
            if (CURRENT_2.equals(CODE2)) {
                setUnlocked(2);
                Toaster.toastLong("El tesoro esta en Akihabara");
                clear2();
                Answers.getInstance().logLevelEnd(new LevelEndEvent().putLevelName("Easter Egg Phase 2").putScore(0));
                Answers.getInstance().logLevelStart(new LevelStartEvent().putLevelName("Easter Egg Phase 3"));
            } else if (!CODE2.startsWith(CURRENT_2)) {
                clear2();
                CURRENT_2 += part;
            }
        }
    }

    public static void clear2() {
        CURRENT_2 = "";
    }

    static void enter3() {
        if (isPart2Unlocked() && getPhase() == 3) {
            setUnlocked(3);
            Answers.getInstance().logLevelEnd(new LevelEndEvent().putLevelName("Easter Egg Phase 3").putScore(0));
            Answers.getInstance().logLevelEnd(new LevelEndEvent().putLevelName("Easter Egg").putScore(0));
        }
    }

    private static boolean isPart0Unlocked() {
        return EADB.INSTANCE.eaDAO().isUnlocked(0);
    }

    private static boolean isPart1Unlocked() {
        return EADB.INSTANCE.eaDAO().isUnlocked(1);
    }

    private static boolean isPart2Unlocked() {
        return EADB.INSTANCE.eaDAO().isUnlocked(2);
    }

    private static boolean isPart3Unlocked() {
        return EADB.INSTANCE.eaDAO().isUnlocked(3);
    }

    public static int getPhase() {
        if (isPart3Unlocked())
            return 4;
        else if (isPart2Unlocked())
            return 3;
        else if (isPart1Unlocked())
            return 2;
        else if (isPart0Unlocked())
            return 1;
        else return 0;
    }

    @Nullable
    public static String getEAMessage() {
        if (isPart3Unlocked())
            return "Disfruta de la recompensa";
        else if (isPart2Unlocked())
            return "El tesoro esta en Akihabara";
        else if (isPart1Unlocked())
            return "LMMJVSD \u2192 US \u2192 " + CODE2;
        else if (isPart0Unlocked())
            return CODE1;
        else
            return "\u26B2 easteregg";
    }

    private static void setUnlocked(int phase) {
        EADB.INSTANCE.eaDAO().unlock(new EAObject(phase));
    }

    @StyleRes
    public static int getTheme(Context context) {
        if (context == null || !isPart0Unlocked() || !isPart1Unlocked() || !isPart2Unlocked() | !isPart3Unlocked())
            return R.style.AppTheme;
        switch (PrefsUtil.INSTANCE.getThemeColor()) {
            default:
            case "0":
                return R.style.AppTheme;
            case "1":
                return R.style.AppTheme_Pink;
            case "2":
                return R.style.AppTheme_Purple;
            case "3":
                return R.style.AppTheme_DeepPurple;
            case "4":
                return R.style.AppTheme_Indigo;
            case "5":
                return R.style.AppTheme_Blue;
            case "6":
                return R.style.AppTheme_LightBlue;
            case "7":
                return R.style.AppTheme_Cyan;
            case "8":
                return R.style.AppTheme_Teal;
            case "9":
                return R.style.AppTheme_Green;
            case "10":
                return R.style.AppTheme_LightGreen;
            case "11":
                return R.style.AppTheme_Lime;
            case "12":
                return R.style.AppTheme_Yellow;
            case "13":
                return R.style.AppTheme_Amber;
            case "14":
                return R.style.AppTheme_Orange;
            case "15":
                return R.style.AppTheme_DeepOrange;
            case "16":
                return R.style.AppTheme_Brown;
            case "17":
                return R.style.AppTheme_Gray;
            case "18":
                return R.style.AppTheme_BlueGray;
        }
    }

    @StyleRes
    public static int getThemeNA(Context context) {
        if (context == null || !isPart0Unlocked() || !isPart1Unlocked() || !isPart2Unlocked() | !isPart3Unlocked())
            return R.style.AppTheme_NoActionBar;
        switch (PrefsUtil.INSTANCE.getThemeColor()) {
            default:
            case "0":
                return R.style.AppTheme_NoActionBar;
            case "1":
                return R.style.AppTheme_NoActionBar_Pink;
            case "2":
                return R.style.AppTheme_NoActionBar_Purple;
            case "3":
                return R.style.AppTheme_NoActionBar_DeepPurple;
            case "4":
                return R.style.AppTheme_NoActionBar_Indigo;
            case "5":
                return R.style.AppTheme_NoActionBar_Blue;
            case "6":
                return R.style.AppTheme_NoActionBar_LightBlue;
            case "7":
                return R.style.AppTheme_NoActionBar_Cyan;
            case "8":
                return R.style.AppTheme_NoActionBar_Teal;
            case "9":
                return R.style.AppTheme_NoActionBar_Green;
            case "10":
                return R.style.AppTheme_NoActionBar_LightGreen;
            case "11":
                return R.style.AppTheme_NoActionBar_Lime;
            case "12":
                return R.style.AppTheme_NoActionBar_Yellow;
            case "13":
                return R.style.AppTheme_NoActionBar_Amber;
            case "14":
                return R.style.AppTheme_NoActionBar_Orange;
            case "15":
                return R.style.AppTheme_NoActionBar_DeepOrange;
            case "16":
                return R.style.AppTheme_NoActionBar_Brown;
            case "17":
                return R.style.AppTheme_NoActionBar_Gray;
            case "18":
                return R.style.AppTheme_NoActionBar_BlueGray;
        }
    }

    @StyleRes
    public static int getThemeDialog(Context context) {
        if (context == null || !isPart0Unlocked() || !isPart1Unlocked() || !isPart2Unlocked() | !isPart3Unlocked())
            return R.style.AppTheme_NoActionBar;
        switch (PrefsUtil.INSTANCE.getThemeColor()) {
            default:
            case "0":
                return R.style.AppTheme_Dialog_Base;
            case "1":
                return R.style.AppTheme_Dialog_Pink;
            case "2":
                return R.style.AppTheme_Dialog_Purple;
            case "3":
                return R.style.AppTheme_Dialog_DeepPurple;
            case "4":
                return R.style.AppTheme_Dialog_Indigo;
            case "5":
                return R.style.AppTheme_Dialog_Blue;
            case "6":
                return R.style.AppTheme_Dialog_LightBlue;
            case "7":
                return R.style.AppTheme_Dialog_Cyan;
            case "8":
                return R.style.AppTheme_Dialog_Teal;
            case "9":
                return R.style.AppTheme_Dialog_Green;
            case "10":
                return R.style.AppTheme_Dialog_LightGreen;
            case "11":
                return R.style.AppTheme_Dialog_Lime;
            case "12":
                return R.style.AppTheme_Dialog_Yellow;
            case "13":
                return R.style.AppTheme_Dialog_Amber;
            case "14":
                return R.style.AppTheme_Dialog_Orange;
            case "15":
                return R.style.AppTheme_Dialog_DeepOrange;
            case "16":
                return R.style.AppTheme_Dialog_Brown;
            case "17":
                return R.style.AppTheme_Dialog_Gray;
            case "18":
                return R.style.AppTheme_Dialog_BlueGray;
        }
    }

    @DrawableRes
    public static int getThemeImg(Context context) {
        if (context == null || !isPart0Unlocked() || !isPart1Unlocked() || !isPart2Unlocked() | !isPart3Unlocked())
            return R.drawable.side_nav_bar;
        switch (PrefsUtil.INSTANCE.getThemeColor()) {
            default:
            case "0":
                return R.drawable.side_nav_bar;
            case "1":
                return R.drawable.side_nav_bar_pink;
            case "2":
                return R.drawable.side_nav_bar_purple;
            case "3":
                return R.drawable.side_nav_bar_deep_purple;
            case "4":
                return R.drawable.side_nav_bar_indigo;
            case "5":
                return R.drawable.side_nav_bar_blue;
            case "6":
                return R.drawable.side_nav_bar_light_blue;
            case "7":
                return R.drawable.side_nav_bar_cyan;
            case "8":
                return R.drawable.side_nav_bar_teal;
            case "9":
                return R.drawable.side_nav_bar_green;
            case "10":
                return R.drawable.side_nav_bar_light_green;
            case "11":
                return R.drawable.side_nav_bar_lime;
            case "12":
                return R.drawable.side_nav_bar_yellow;
            case "13":
                return R.drawable.side_nav_bar_amber;
            case "14":
                return R.drawable.side_nav_bar_orange;
            case "15":
                return R.drawable.side_nav_bar_deep_orange;
            case "16":
                return R.drawable.side_nav_bar_brown;
            case "17":
                return R.drawable.side_nav_bar_gray;
            case "18":
                return R.drawable.side_nav_bar_blue_gray;
        }
    }

    @ColorRes
    public static int getThemeColor(Context context) {
        if (context == null || !isPart0Unlocked() || !isPart1Unlocked() || !isPart2Unlocked() | !isPart3Unlocked())
            return R.color.colorAccent;
        switch (PrefsUtil.INSTANCE.getThemeColor()) {
            default:
            case "0":
                return R.color.colorAccent;
            case "1":
                return R.color.colorAccentPink;
            case "2":
                return R.color.colorAccentPurple;
            case "3":
                return R.color.colorAccentDeepPurple;
            case "4":
                return R.color.colorAccentIndigo;
            case "5":
                return R.color.colorAccentBlue;
            case "6":
                return R.color.colorAccentLightBlue;
            case "7":
                return R.color.colorAccentCyan;
            case "8":
                return R.color.colorAccentTeal;
            case "9":
                return R.color.colorAccentGreen;
            case "10":
                return R.color.colorAccentLightGreen;
            case "11":
                return R.color.colorAccentLime;
            case "12":
                return R.color.colorAccentYellow;
            case "13":
                return R.color.colorAccentAmber;
            case "14":
                return R.color.colorAccentOrange;
            case "15":
                return R.color.colorAccentDeepOrange;
            case "16":
                return R.color.colorAccentBrown;
            case "17":
                return R.color.colorAccentGray;
            case "18":
                return R.color.colorAccentBlueGrey;
        }
    }

    @ColorRes
    public static int getThemeColorLight(Context context) {
        if (context == null || !isPart0Unlocked() || !isPart1Unlocked() || !isPart2Unlocked() | !isPart3Unlocked())
            return R.color.colorAccentLight;
        switch (PrefsUtil.INSTANCE.getThemeColor()) {
            default:
            case "0":
                return R.color.colorAccentLight;
            case "1":
                return R.color.colorAccentPinkLight;
            case "2":
                return R.color.colorAccentPurpleLight;
            case "3":
                return R.color.colorAccentDeepPurpleLight;
            case "4":
                return R.color.colorAccentIndigoLight;
            case "5":
                return R.color.colorAccentBlueLight;
            case "6":
                return R.color.colorAccentLightBlueLight;
            case "7":
                return R.color.colorAccentCyanLight;
            case "8":
                return R.color.colorAccentTealLight;
            case "9":
                return R.color.colorAccentGreenLight;
            case "10":
                return R.color.colorAccentLightGreenLight;
            case "11":
                return R.color.colorAccentLimeLight;
            case "12":
                return R.color.colorAccentYellowLight;
            case "13":
                return R.color.colorAccentAmberLight;
            case "14":
                return R.color.colorAccentOrangeLight;
            case "15":
                return R.color.colorAccentDeepOrangeLight;
            case "16":
                return R.color.colorAccentBrownLight;
            case "17":
                return R.color.colorAccentGrayLight;
            case "18":
                return R.color.colorAccentBlueGreyLight;
        }
    }
}