package knf.kuma.commons

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import androidx.lifecycle.LiveData
import knf.kuma.player.CustomExoPlayer
import knf.kuma.player.VideoActivity
import java.util.*

@SuppressLint("StaticFieldLeak")
object PrefsUtil {
    private var context: Context? = null

    val layType: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("lay_type", "0")!!

    val themeOption: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("theme_option", "0")!!

    val themeColor: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString("theme_color", "0")!!

    var favsOrder: Int
        get() = PreferenceManager.getDefaultSharedPreferences(context).getInt("favs_order", 0)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("favs_order", value).apply()

    var dirOrder: Int
        get() = PreferenceManager.getDefaultSharedPreferences(context).getInt("dir_order", 0)
        set(value) = PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("dir_order", value).apply()

    val isChapsAsc: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("asc_chapters", false)

    val isDirectoryFinished: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("directory_finished", false)

    val downloaderType: Int
        get() = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("downloader_type", "1")!!)

    val showFavIndicator: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_fav_count", true)

    private val useExperimentalPlayer: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("experimental_player", false)

    val collapseDirectoryNotification: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("collapse_dir_nots", true)

    val showRecentImage: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("recent_image", true)

    fun init(context: Context) {
        PrefsUtil.context = context
    }

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
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("buffer_size", "32")!!)
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

}
