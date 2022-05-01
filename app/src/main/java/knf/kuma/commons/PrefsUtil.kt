package knf.kuma.commons

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager
import com.securepreferences.SecurePreferences
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.ads.AdsUtils
import knf.kuma.player.CustomExoPlayer
import knf.kuma.player.VideoActivity
import knf.kuma.uagen.randomUA
import knh.kuma.commons.cloudflarebypass.util.ConvertUtil
import java.net.HttpCookie
import java.util.*

@SuppressLint("StaticFieldLeak")
object PrefsUtil {
    private var context: Context = App.context

    val layType: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("lay_type", context.getString(R.string.layType))
                ?: "0"

    val themeOption: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("theme_option", context.getString(R.string.theme_default))
                ?: context.getString(R.string.theme_default)

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
        get() = (!BuildConfig.DEBUG && !isSubscriptionEnabled && AdsUtils.remoteConfigs.getBoolean("ads_forced")) ||
                PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ads_enabled_new", true)

    val downloaderType: Int
        get() = Integer.parseInt(
            PreferenceManager.getDefaultSharedPreferences(context)
                .getString("downloader_type", null)
                ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isMIUI(safeContext)) "0" else "1"
        )

    var autoBackupTime: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("auto_backup", "0")
                ?: "0"
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("auto_backup", value).apply()

    val showFavIndicator: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("show_fav_count", true)

    var spProtectionEnabled: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("security_blocking_firestore", true)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean("security_blocking_firestore", value).apply()

    var tvRecentsChannelCreated: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("tv_channel_recents_created", false)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean("tv_channel_recents_created", value).apply()

    var tvRecentsPreFilled: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("tv_channel_recents_prefilled", false)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean("tv_channel_recents_prefilled", value).apply()

    var tvRecentsChannelId: Long
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getLong("tv_channel_recents_id", -1)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putLong("tv_channel_recents_id", value).apply()

    var tvRecentsChannelLastEid: String?
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getString("tv_channel_recents_last_eid", null)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString("tv_channel_recents_last_eid", value).apply()

    var tvRecentsChannelIds: Set<String>?
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getStringSet("tv_channel_recents_ids", null)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putStringSet("tv_channel_recents_ids", value).apply()

    var spErrorType: String?
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getString("sp_error_type", null)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString("sp_error_type", value).apply()

    private val useExperimentalPlayer: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("experimental_player", false)

    val collapseDirectoryNotification: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("collapse_dir_nots", true)

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

    var isSecurityUpdated: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("securityUpdated", false)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("securityUpdated", value).apply()

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

    var timeoutTime: Long
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("timeout_time", if (context.resources.getBoolean(R.bool.isTv)) "0" else "10")?.toLong()
                ?: 0
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("timeout_time", value.toString()).apply()

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
        get() =
            if (alwaysGenerateUA && mayUseRandomUA)
                randomUA()
            else
                PreferenceManager.getDefaultSharedPreferences(context).getString("user_agent", randomUA())
                        ?: randomUA()
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("user_agent", value).apply()

    val mayUseRandomUA: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("may_use_random_useragent_1", false)

    var alwaysGenerateUA: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("alwaysGenerateUA", true)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("alwaysGenerateUA", value).apply()

    var userAgentDir: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("user_agent_dir", randomUA())
                ?: randomUA()
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("user_agent_dir", value).apply()

    var randomLimit: Int
        get() = PreferenceManager.getDefaultSharedPreferences(context).getInt("random_limit", 25)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("random_limit", value).apply()

    val useHome: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("recents_design", "0") == "1"

    var useDefaultUserAgent: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_device_useragent", false)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("use_device_useragent", value).apply()

    val usePlaceholders: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("list_placeholder", false)

    var instanceUuid: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("instance_uuid", null)
                ?: UUID.randomUUID().toString().also { instanceUuid = it }
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("instance_uuid", value).apply()

    var instanceName: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("instance_name", "Anónimo")
                ?: "Anónimo"
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("instance_name", value).apply()

    var recentLastHiddenNew: Int
        get() = PreferenceManager.getDefaultSharedPreferences(context).getInt("recent_last_hidden_new", 0)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("recent_last_hidden_new", value).apply()

    private val rewardedVideoCount: Int
        get() = PreferenceManager.getDefaultSharedPreferences(context).getInt("rewarded_videos_seen", 0)

    var userRewardedVideoCount: Int
        get() = SecurePreferences(context).getInt("user_rewarded_videos_seen", rewardedVideoCount)
        set(value) = SecurePreferences(context).edit().putInt("user_rewarded_videos_seen", value).apply()

    private val coins: Int
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("coinsNum", null)?.decrypt()?.toInt()
                ?: 0

    var userCoins: Int
        get() = noCrashLet(0) {
            SecurePreferences(context).getInt("userCoins", try {
                coins
            } catch (e: Exception) {
                0
            })
        }
        set(value) = SecurePreferences(context).edit().putInt("userCoins", value).apply()

    var lsAchievements: Long
        get() = PreferenceManager.getDefaultSharedPreferences(context).getLong("ls_achievements", -1)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("ls_achievements", value).apply()

    var lsEa: Long
        get() = PreferenceManager.getDefaultSharedPreferences(context).getLong("ls_ea", -1)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("ls_ea", value).apply()

    var lsFavs: Long
        get() = PreferenceManager.getDefaultSharedPreferences(context).getLong("ls_favs", -1)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("ls_favs", value).apply()

    var lsGenres: Long
        get() = PreferenceManager.getDefaultSharedPreferences(context).getLong("ls_genres", -1)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("ls_genres", value).apply()

    var lsHistory: Long
        get() = PreferenceManager.getDefaultSharedPreferences(context).getLong("ls_history", -1)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("ls_history", value).apply()

    var lsQueue: Long
        get() = PreferenceManager.getDefaultSharedPreferences(context).getLong("ls_queue", -1)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("ls_queue", value).apply()

    var lsSeeing: Long
        get() = PreferenceManager.getDefaultSharedPreferences(context).getLong("ls_seeing", -1)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("ls_seeing", value).apply()

    var lsSeen: Long
        get() = PreferenceManager.getDefaultSharedPreferences(context).getLong("ls_seen", -1)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("ls_seen", value).apply()

    var isFamilyFriendly: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("family_friendly_enabled", false)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("family_friendly_enabled", value).apply()

    var ffPass: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("ff_pass_cbc", "")
                ?: ""
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("ff_pass_cbc", value).apply()

    var topCount: Int
        get() = PreferenceManager.getDefaultSharedPreferences(context).getInt("top_count", 25)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("top_count", value).apply()

    var subscriptionToken: String?
        get() = SecurePreferences(context).getString("subscription_token", null)
        set(value) = SecurePreferences(context).edit().putString("subscription_token", value).apply()

    var isPSWarned: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("isPSWarned1", false)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("isPSWarned1", value).apply()

    var isNativeAdsEnabled: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("isNativeAdsEnabled", true)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("isNativeAdsEnabled", value).apply()

    var isFullAdsEnabled: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("isFullAdsEnabled", true)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("isFullAdsEnabled", value).apply()

    var fullAdsProbability: Float
        get() = PreferenceManager.getDefaultSharedPreferences(context).getFloat("fullAdsProbability", 70.0f)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putFloat("fullAdsProbability", value).apply()

    var fullAdsExtraProbability: Float
        get() = PreferenceManager.getDefaultSharedPreferences(context).getFloat("fullAdsExtraProbability", 50.0f)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putFloat("fullAdsExtraProbability", value).apply()

    val designStyle: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("designStyleType", "0")
                ?: "0"

    val recentActionType: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("recentActionType", "0")
                ?: "0"

    var dirCookies: List<HttpCookie>
        get() = ConvertUtil.String2List(PreferenceManager.getDefaultSharedPreferences(context).getString("dirCookies", "")
                ?: "")
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putString("dirCookies", ConvertUtil.listToString(value)).apply()

    var isForbiddenTipShown: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("isForbiddenTipShown", false)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("isForbiddenTipShown", value).apply()

    var isBypassWarningShown: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("isBypassWarningShown", false)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("isBypassWarningShown", value).apply()

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

    fun getLiveDesignType(): LiveData<String> {
        return PreferenceManager.getDefaultSharedPreferences(context).stringLiveData("designStyleType", "0").distinct
    }

    fun getLiveEmissionVisibility(): LiveData<Boolean> =
            PreferenceManager.getDefaultSharedPreferences(context).booleanLiveData("show_hidden", false)

    val isSubscriptionEnabled: Boolean get() = subscriptionToken != null

    private val defaultDownloadType: String
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                "1"
            else
                PreferenceManager.getDefaultSharedPreferences(context).getString("download_type", "0")
                        ?: "0"
        }

}
