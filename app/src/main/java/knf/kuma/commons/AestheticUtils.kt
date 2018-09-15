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
            if (Aesthetic.isFirstTime)
                Aesthetic.config {
                    //activityTheme(if (isDarkMode) R.style.Theme_AppCompat_NoActionBar else R.style.Theme_AppCompat_NoActionBar)
                    isDark(isDarkMode(context))
                    colorPrimary(primaryColor(context))
                    colorStatusBarAuto()
                    colorNavigationBarAuto()
                    colorAccentRes(accentColor(context))
                    navigationViewMode(NavigationViewMode.SELECTED_ACCENT)
                    bottomNavigationBackgroundMode(BottomNavBgMode.PRIMARY)
                    bottomNavigationIconTextMode(BottomNavIconTextMode.SELECTED_ACCENT)
                    swipeRefreshLayoutColors(EAHelper.getThemeColor(context), EAHelper.getThemeColorLight(context), primaryColor(context))
                }
        }

        fun setDefaultsTranslucent(context: Context) {
            if (Aesthetic.isFirstTime)
                Aesthetic.config {
                    activityTheme(if (isDarkMode(context)) R.style.Theme_AppCompat_Light_Dialog else R.style.Theme_AppCompat_Dialog)
                    isDark(isDarkMode(context))
                    colorStatusBar(Color.TRANSPARENT)
                    colorNavigationBar(Color.TRANSPARENT)
                    colorWindowBackground(Color.TRANSPARENT)
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
        private fun accentColor(context: Context): Int {
            return EAHelper.getThemeColor(context)
        }

        private fun preferences(context: Context): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(context)
        }
    }
}