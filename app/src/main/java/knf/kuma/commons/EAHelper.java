package knf.kuma.commons;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StyleRes;
import android.util.Log;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.LevelEndEvent;
import com.crashlytics.android.answers.LevelStartEvent;

import org.jetbrains.annotations.Nullable;

import knf.kuma.R;
import xdroid.toaster.Toaster;

public class EAHelper {
    private static final String CODE1 = "FRRDCRFFR";
    private static final String CODE2 = "146523764";
    private static String CURRENT_1 = "";
    private static String CURRENT_2 = "";

    public static void checkStart(Context context, String query) {
        if (context != null && getPhase(context) == 0 && query.equals("easteregg")) {
            Toaster.toastLong("FRRDCRFFR");
            Answers.getInstance().logLevelStart(new LevelStartEvent().putLevelName("Easter Egg"));
            setUnlocked(context, 0);
        }
    }

    public static void enter1(Context context, String part) {
        if (context != null && isPart0Unlocked(context) && getPhase(context) == 1) {
            CURRENT_1 += part;
            if (CURRENT_1.equals(CODE1)) {
                setUnlocked(context, 1);
                Toaster.toastLong("LMMJVSD \u2192 US \u2192 146523764");
                clear1();
                Answers.getInstance().logLevelEnd(new LevelEndEvent().putLevelName("Easter Egg Phase 1").putScore(0));
            } else if (!CODE1.startsWith(CURRENT_1)) {
                clear1();
                CURRENT_1 += part;
            }
        }
    }

    public static void clear1() {
        CURRENT_1 = "";
    }

    public static void enter2(Context context, String part) {
        if (context != null && isPart1Unlocked(context) && getPhase(context) == 2) {
            CURRENT_2 += part;
            if (CURRENT_2.equals(CODE2)) {
                setUnlocked(context, 2);
                Toaster.toastLong("El tesoro esta en Akihabara");
                clear2();
                Answers.getInstance().logLevelEnd(new LevelEndEvent().putLevelName("Easter Egg Phase 2").putScore(0));
            } else if (!CODE2.startsWith(CURRENT_2)) {
                clear2();
                CURRENT_2 += part;
            }
            Log.e("EA", CURRENT_2);
        }
    }

    public static void clear2() {
        CURRENT_2 = "";
    }

    static void enter3(Context context) {
        if (context != null && isPart2Unlocked(context) && getPhase(context) == 3)
            setUnlocked(context, 3);
    }

    private static boolean isPart0Unlocked(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ea_0_unlock", false);
    }

    private static boolean isPart1Unlocked(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ea_1_unlock", false);
    }

    private static boolean isPart2Unlocked(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ea_2_unlock", false);
    }

    private static boolean isPart3Unlocked(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ea_3_unlock", false);
    }

    public static int getPhase(Context context) {
        if (context == null)
            return 0;
        if (isPart3Unlocked(context))
            return 4;
        else if (isPart2Unlocked(context))
            return 3;
        else if (isPart1Unlocked(context))
            return 2;
        else if (isPart0Unlocked(context))
            return 1;
        else return 0;
    }

    @Nullable
    public static String getEAMessage(Context context) {
        if (context == null)
            return null;
        if (isPart3Unlocked(context))
            return "Disfruta de la recompensa";
        else if (isPart2Unlocked(context))
            return "El tesoro esta en Akihabara";
        else if (isPart1Unlocked(context))
            return "LMMJVSD \u2192 US \u2192 146523764";
        else if (isPart0Unlocked(context))
            return "FRRDCRFFR";
        else
            return null;
    }

    private static void setUnlocked(Context context, int phase) {
        if (context != null)
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("ea_" + phase + "_unlock", true).apply();
    }

    @StyleRes
    public static int getTheme(Context context) {
        if (context == null || !isPart0Unlocked(context) || !isPart1Unlocked(context) || !isPart2Unlocked(context) | !isPart3Unlocked(context))
            return R.style.AppTheme;
        switch (PreferenceManager.getDefaultSharedPreferences(context).getString("theme_color", "0")) {
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
        if (context == null || !isPart0Unlocked(context) || !isPart1Unlocked(context) || !isPart2Unlocked(context) | !isPart3Unlocked(context))
            return R.style.AppTheme_NoActionBar;
        switch (PreferenceManager.getDefaultSharedPreferences(context).getString("theme_color", "0")) {
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

    @DrawableRes
    public static int getThemeImg(Context context) {
        if (context == null || !isPart0Unlocked(context) || !isPart1Unlocked(context) || !isPart2Unlocked(context) | !isPart3Unlocked(context))
            return R.drawable.side_nav_bar;
        switch (PreferenceManager.getDefaultSharedPreferences(context).getString("theme_color", "0")) {
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
        if (context == null || !isPart0Unlocked(context) || !isPart1Unlocked(context) || !isPart2Unlocked(context) | !isPart3Unlocked(context))
            return R.color.colorAccent;
        switch (PreferenceManager.getDefaultSharedPreferences(context).getString("theme_color", "0")) {
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
        if (context == null || !isPart0Unlocked(context) || !isPart1Unlocked(context) || !isPart2Unlocked(context) | !isPart3Unlocked(context))
            return R.color.colorAccentLight;
        switch (PreferenceManager.getDefaultSharedPreferences(context).getString("theme_color", "0")) {
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
