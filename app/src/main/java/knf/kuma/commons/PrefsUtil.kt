package knf.kuma.commons

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.player.CustomExoPlayer
import knf.kuma.player.VideoActivity
import knf.kuma.uagen.randomUA

@SuppressLint("StaticFieldLeak")
object PrefsUtil {
    private var context: Context = App.context

    val layType: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("lay_type", context.getString(R.string.layType))
                ?: "0"

    val themeOption: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("theme_option", "0")
                ?: "0"

    val themeColor: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("theme_color", "0")
                ?: "0"

    var favsOrder: Int
        get() = PreferenceManager.getDefaultSharedPreferences(context).getInt("favs_order", 0)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("favs_order", value).apply()

    var dirOrder: Int
        get() = PreferenceManager.getDefaultSharedPreferences(context).getInt("dir_order", 0)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("dir_order", value).apply()

    var achievementsVersion: Int
        get() = PreferenceManager.getDefaultSharedPreferences(context).getInt("achievements_version", 0)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("achievements_version", value).apply()

    val isChapsAsc: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("asc_chapters", false)

    var isDirectoryFinished: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("directory_finished", false)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("directory_finished", value).apply()

    val isAdsEnabled: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ads_enabled", false) && BuildConfig.DEBUG

    val downloaderType: Int
        get() = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("downloader_type", "1")
                ?: "1")

    var autoBackupTime: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("auto_backup", "0")
                ?: "0"
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("auto_backup", value).apply()

    val showFavIndicator: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_fav_count", true)

    private val useExperimentalPlayer: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("experimental_player", false)

    val collapseDirectoryNotification: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("collapse_dir_nots", true)

    val showRecentImage: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("recent_image", true)

    val useSmoothAnimations: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("smooth_animations", true)

    var emissionBlacklist: MutableSet<String>
        get() = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("emision_blacklist", LinkedHashSet())
                ?: LinkedHashSet()
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("emision_blacklist", value).apply()

    var emissionShowHidden: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_hidden", false)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("show_hidden", value).apply()

    var isAchievementsOmitted: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("achievements_omited", false)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("achievements_omited", value).apply()

    var lastStart: Long
        get() = PreferenceManager.getDefaultSharedPreferences(context).getLong("last_start", System.currentTimeMillis())
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("last_start", value).apply()

    var firstStart: Long
        get() = PreferenceManager.getDefaultSharedPreferences(context).getLong("first_start_new", 0)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("first_start_new", value).apply()

    val saveWithName: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("save_type", "0") == "0"

    var emissionShowFavs: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_favs", true)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("show_favs", value).apply()

    val timeoutTime: Long
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("timeout_time", if (context.resources.getBoolean(R.bool.isTv)) "0" else "10")?.toLong()
                ?: 0

    var rememberServer: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("remember_server", false)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("remember_server", value).apply()

    var lastServer: String?
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("last_server", null)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("last_server", value).apply()

    var lastBackup: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("last_backup", "Desconocido")
                ?: "Desconocido"
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("last_backup", value).apply()

    val isProxyCastEnabled: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("force_local_cast", false)

    val isGroupingEnabled: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("group_notifications", true) && canGroupNotifications

    val useExperimentalOkHttp: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("experimental_okhttp", false)

    var storageType: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("storage_type", "Sin almacenamiento")
                ?: "Sin almacenamiento"
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("storage_type", value).apply()

    var downloadType: String
        get() = defaultDownloadType
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("download_type", value).apply()

    val maxParallelDownloads: Int
        get() = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("max_parallel_downloads", "3")
                ?: "3")

    var userAgent: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("user_agent", randomUA())
                ?: randomUA()
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("user_agent", value).apply()

    var randomLimit: Int
        get() = PreferenceManager.getDefaultSharedPreferences(context).getInt("random_limit", 25)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("random_limit", value).apply()

    val useHome: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("recents_design", "0") == "1"

    val useDefaultUserAgent: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("default_useragent", false)

    fun showProgress(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_progress", true)
    }

    fun showFavSections(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("fav_sections", true)
    }

    fun showImport(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_import", false)
    }

    fun bufferSize(): Int {
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("buffer_size", "32")
                ?: "32")
    }

    fun getLiveEmissionBlackList(): LiveData<Set<String>> {
        return PreferenceManager.getDefaultSharedPreferences(context).stringSetLiveData("emision_blacklist", LinkedHashSet())
    }

    fun getPlayerIntent(): Intent {
        return if (useExperimentalPlayer)
            Intent(context, VideoActivity::class.java)
        else
            Intent(context, CustomExoPlayer::class.java)
    }

    fun getLiveShowFavIndicator(): LiveData<Boolean> {
        return PreferenceManager.getDefaultSharedPreferences(context).booleanLiveData("show_fav_count", true)
    }

    private val defaultDownloadType: String
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                "1"
            else
                PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0")
                        ?: "0"
        }

}
