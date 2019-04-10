package knf.kuma.commons

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.aesthetic.BottomNavBgMode
import com.afollestad.aesthetic.BottomNavIconTextMode
import com.afollestad.aesthetic.NavigationViewMode
import knf.kuma.R

class AestheticUtils {

    //TODO: Wait for success implementation

    companion object {
        fun setDefaults(context: Context) {
            noCrash {
                if (Aesthetic.isFirstTime)
                    Aesthetic.config {
                        activityTheme(if (isDarkMode(context)) R.style.Theme_AppCompat_NoActionBar else R.style.Theme_AppCompat_Light_NoActionBar)
                        isDark(isDarkMode(context))
                        colorPrimary(primaryColor(context))
                        colorStatusBarAuto()
                        colorNavigationBarAuto()
                        colorAccentRes(accentColor(context))
                        navigationViewMode(NavigationViewMode.SELECTED_ACCENT)
                        bottomNavigationBackgroundMode(BottomNavBgMode.PRIMARY)
                        bottomNavigationIconTextMode(BottomNavIconTextMode.SELECTED_ACCENT)
                        swipeRefreshLayoutColorsRes(EAHelper.getThemeColor(), EAHelper.getThemeColorLight(), primaryColorRes(context))
                    }
            }
        }

        fun setDefaultsTranslucent(context: Context) {
            noCrash {
                if (Aesthetic.isFirstTime)
                    Aesthetic.config {
                        activityTheme(if (isDarkMode(context)) R.style.Theme_AppCompat_Light_Dialog else R.style.Theme_AppCompat_Dialog)
                        isDark(isDarkMode(context))
                        colorStatusBar(Color.TRANSPARENT)
                        colorNavigationBar(Color.TRANSPARENT)
                        colorWindowBackground(Color.TRANSPARENT)
                    }
            }
        }

        fun updateIsDarkMode(context: Context, isDark: Boolean) {
            if (isDark != isDarkMode(context)) {
                preferences(context).edit().putBoolean("theme_isDark", isDark).apply()
                Aesthetic.config {
                    activityTheme(if (isDarkMode(context)) R.style.Theme_AppCompat_NoActionBar else R.style.Theme_AppCompat_Light_NoActionBar)
                    isDark(isDark)
                }
            }
        }

        fun updateAccentColor(context: Context, value: String) {
            Aesthetic.config {
                colorAccentRes(EAHelper.getThemeColor(value))
                swipeRefreshLayoutColorsRes(EAHelper.getThemeColor(value), EAHelper.getThemeColorLight(value), primaryColorRes(context))
            }
        }

        private fun isDarkMode(context: Context): Boolean {
            return preferences(context).getBoolean("theme_isDark", false)
        }

        private fun isCustomMode(context: Context): Boolean {
            return preferences(context).getBoolean("theme_isCustom", false)
        }

        @ColorInt
        private fun primaryColor(context: Context): Int {
            return if (isCustomMode(context)) preferences(context).getInt("theme_primary", ContextCompat.getColor(context, R.color.colorPrimary))
            else ContextCompat.getColor(context, R.color.colorPrimary)
        }

        @ColorRes
        private fun primaryColorRes(context: Context): Int {
            return if (isCustomMode(context)) preferences(context).getInt("theme_primary", R.color.colorPrimary)
            else R.color.colorPrimary
        }

        @ColorRes
        private fun accentColor(context: Context): Int {
            return EAHelper.getThemeColor()
        }

        private fun preferences(context: Context): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(context)
        }
    }
}