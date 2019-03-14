package knf.kuma.backup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import knf.kuma.App
import knf.kuma.achievements.AchievementManager
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.safeDismiss
import knf.kuma.commons.safeShow
import knf.kuma.commons.showSnackbar
import knf.kuma.database.CacheDB
import knf.kuma.pojos.*
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@SuppressLint("StaticFieldLeak")
object BUUtils {
    const val LOGIN_CODE = 56478
    private var activity: Activity? = null
    private var loginInterface: LoginInterface? = null
    private var DBC: DbxClientV2? = null

    private const val TAG = "Sync"
    private const val keyFavs = "favs"
    private const val keyHistory = "history"
    private const val keyFollowing = "following"
    private const val keySeen = "seen"
    private const val keyAchievements = "achievements"
    const val keyAutoBackup = "autobackup"

    val isLogedIn: Boolean
        get() = DBC != null

    private var dbToken: String?
        get() = PreferenceManager.getDefaultSharedPreferences(activity).getString("db_token", null)
        set(value) = PreferenceManager.getDefaultSharedPreferences(activity).edit().putString("db_token", value).apply()

    var type: BUType
        get() = getType(activity)
        set(type) = PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt("backup_type", type.value).apply()

    fun init(activity: Activity, startclient: Boolean) {
        init(activity, activity as LoginInterface, startclient)
    }

    fun init(activity: Activity, lInterface: LoginInterface, startclient: Boolean) {
        BUUtils.activity = activity
        loginInterface = lInterface
        if (startclient)
            startClient(type, true)
    }

    fun init(context: Context) {
        startClient(context, getType(context))
    }

    fun setDropBoxClient(token: String?) {
        if (token != null) {
            PreferenceManager.getDefaultSharedPreferences(activity).edit().putString("db_token", token).apply()
            val requestConfig = DbxRequestConfig.newBuilder("dropbox_app")
                    .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                    .build()
            DBC = DbxClientV2(requestConfig, token)
        }
        loginInterface?.onLogin()
    }

    private fun setDropBoxClient(context: Context, token: String?) {
        if (token != null) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString("db_token", token).apply()
            val requestConfig = DbxRequestConfig.newBuilder("dropbox_app")
                    .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                    .build()
            DBC = DbxClientV2(requestConfig, token)
        }
        loginInterface?.onLogin()
    }

    private fun getDBToken(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("db_token", null)
    }

    private fun clearDBToken() {
        PreferenceManager.getDefaultSharedPreferences(activity).edit().putString("db_token", null).apply()
    }

    fun startClient(type: BUType, fromInit: Boolean) {
        when (type) {
            BUUtils.BUType.DROPBOX -> if (fromInit && dbToken != null) {
                setDropBoxClient(dbToken)
            } else {
                activity?.let { Auth.startOAuth2Authentication(it, "qtjow4hsk06vt19") }
            }
            else -> {
            }
        }
    }

    private fun startClient(context: Context, type: BUType) {
        when (type) {
            BUUtils.BUType.DROPBOX -> if (getDBToken(context) != null)
                setDropBoxClient(context, getDBToken(context))
            else -> {
            }
        }
    }

    fun logOut() {
        DBC = null
        when (getType(activity)) {
            BUUtils.BUType.DROPBOX -> clearDBToken()
            else -> {
            }
        }
        type = BUType.LOCAL
    }

    fun getType(context: Context?): BUType {
        return when (PreferenceManager.getDefaultSharedPreferences(context).getInt("backup_type", -1)) {
            1 -> BUType.DROPBOX
            else -> BUType.LOCAL
        }
    }

    fun isConnected(context: Context): Boolean {
        return getType(context) != BUType.LOCAL
    }

    fun search(id: String, searchInterface: SearchInterface) {
        when (type) {
            BUUtils.BUType.DROPBOX -> searchDropbox(id, searchInterface)
            else -> {
            }
        }
    }

    fun search(context: Context, id: String, searchInterface: SearchInterface) {
        when (getType(context)) {
            BUUtils.BUType.DROPBOX -> searchDropbox(id, searchInterface)
            else -> {
            }
        }
    }

    fun backup(view: View, id: String, backupInterface: BackupInterface) {
        when (type) {
            BUUtils.BUType.DROPBOX -> backupDropbox(view, id, backupInterface)
            else -> {
            }
        }
    }

    fun backupNUI(context: Context, id: String, backupInterface: BackupInterface) {
        when (getType(context)) {
            BUUtils.BUType.DROPBOX -> backupDropboxNUI(id, backupInterface)
            else -> {
            }
        }
    }

    fun backupAllNUI(context: Context) {
        val i = AtomicInteger(0)
        val backupInterface: BackupInterface = object : BackupInterface {
            override fun onResponse(backupObject: BackupObject<*>?) {
                i.getAndIncrement()
            }
        }
        backupNUI(context, keyFavs, backupInterface)
        backupNUI(context, keyHistory, backupInterface)
        backupNUI(context, keyFollowing, backupInterface)
        backupNUI(context, keySeen, backupInterface)
        while (i.get() != 4) {
            //
        }
    }

    fun waitAutoBackup(context: Context): AutoBackupObject? {
        val tBackupObject = AtomicReference<AutoBackupObject>(FakeAutoBackup())
        search(context, keyAutoBackup, object : SearchInterface {
            override fun onResponse(backupObject: BackupObject<*>?) {
                try {
                    tBackupObject.set(backupObject as AutoBackupObject)
                } catch (e: Exception) {
                    tBackupObject.set(null)
                }
            }
        })
        while (tBackupObject.get() is FakeAutoBackup) {
            //
        }
        return tBackupObject.get()
    }

    fun backup(backupObject: AutoBackupObject, backupInterface: AutoBackupInterface) {
        loginInterface = null
        BUUtils.init(App.context)
        when (type) {
            BUUtils.BUType.DROPBOX -> backupDropbox(backupObject, backupInterface)
            else -> {
            }
        }
    }

    private fun searchDropbox(id: String, searchInterface: SearchInterface) {
        doAsync {
            try {
                val list = DBC?.files()?.search("", id)?.matches ?: arrayListOf()
                if (list.size > 0) {
                    val downloader = DBC?.files()?.download("/$id")
                    val reader = InputStreamReader(downloader?.inputStream)
                    searchInterface.onResponse(Gson().fromJson<Any>(reader.readText(), getType(id)) as BackupObject<*>)
                    reader.close()
                    downloader?.close()
                } else {
                    searchInterface.onResponse(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                searchInterface.onResponse(null)
            }
        }
    }

    private fun backupDropbox(view: View, id: String, backupInterface: BackupInterface) {
        val snackbar = view.showSnackbar("Respaldando...", Snackbar.LENGTH_INDEFINITE)
        doAsync {
            try {
                val backupObject = BackupObject(getList(id))
                DBC?.files()?.uploadBuilder("/$id")
                        ?.withMute(true)
                        ?.withMode(WriteMode.OVERWRITE)
                        ?.uploadAndFinish(ByteArrayInputStream(Gson().toJson(backupObject, getType(id)).toByteArray(StandardCharsets.UTF_8)))
                backupInterface.onResponse(backupObject)
                saveLastBackup()
            } catch (e: Exception) {
                e.printStackTrace()
                backupInterface.onResponse(null)
            }
            snackbar.safeDismiss()
        }
    }

    private fun backupDropboxNUI(id: String, backupInterface: BackupInterface) {
        doAsync {
            try {
                val backupObject = BackupObject(getList(id))
                DBC?.files()?.uploadBuilder("/$id")
                        ?.withMute(true)
                        ?.withMode(WriteMode.OVERWRITE)
                        ?.uploadAndFinish(ByteArrayInputStream(Gson().toJson(backupObject, getType(id)).toByteArray(StandardCharsets.UTF_8)))
                backupInterface.onResponse(backupObject)
                saveLastBackup()
            } catch (e: Exception) {
                e.printStackTrace()
                backupInterface.onResponse(null)
            }
        }
    }

    private fun backupDropbox(backupObject: AutoBackupObject, backupInterface: AutoBackupInterface) {
        doAsync {
            try {
                DBC?.files()?.uploadBuilder("/$keyAutoBackup")
                        ?.withMute(true)
                        ?.withMode(WriteMode.OVERWRITE)
                        ?.uploadAndFinish(ByteArrayInputStream(Gson().toJson(backupObject, getType(keyAutoBackup)).toByteArray(StandardCharsets.UTF_8)))
                backupInterface.onResponse(backupObject)
                saveLastBackup()
            } catch (e: Exception) {
                e.printStackTrace()
                backupInterface.onResponse(null)
            }
        }
    }

    fun restoreDialog(view: View, id: String, backupObject: BackupObject<*>?) {
        if (backupObject != null)
            activity?.let {
                MaterialDialog(it).safeShow {
                    message(text = "¿Como desea restaurar?")
                    positiveButton(text = "mezclar") { restore(view, false, id, backupObject) }
                    negativeButton(text = "reemplazar") { restore(view, true, id, backupObject) }
                }
            }
    }

    private fun restore(view: View, replace: Boolean, id: String, backupObject: BackupObject<*>) {
        val snackbar = view.showSnackbar("Restaurando...", Snackbar.LENGTH_INDEFINITE)
        doAsync {
            try {
                when (id) {
                    keyFavs -> {
                        if (replace)
                            CacheDB.INSTANCE.favsDAO().clear()
                        (backupObject.data?.filterIsInstance<FavoriteObject>() as? MutableList<FavoriteObject>)?.let { CacheDB.INSTANCE.favsDAO().addAll(it) }
                    }
                    keyHistory -> {
                        if (replace)
                            CacheDB.INSTANCE.recordsDAO().clear()
                        (backupObject.data?.filterIsInstance<RecordObject>() as? MutableList<RecordObject>)?.let { CacheDB.INSTANCE.recordsDAO().addAll(it) }
                    }
                    keyFollowing -> {
                        if (replace)
                            CacheDB.INSTANCE.seeingDAO().clear()
                        (backupObject.data?.filterIsInstance<SeeingObject>() as? MutableList<SeeingObject>)?.let { CacheDB.INSTANCE.seeingDAO().addAll(it) }
                    }
                    keySeen -> {
                        if (replace)
                            CacheDB.INSTANCE.chaptersDAO().clear()
                        (backupObject.data?.filterIsInstance<AnimeObject.WebInfo.AnimeChapter>() as? MutableList<AnimeObject.WebInfo.AnimeChapter>)?.let { CacheDB.INSTANCE.chaptersDAO().addAll(it) }
                    }
                }
                Toaster.toast("Restauración completada")
            } catch (e: Exception) {
                e.printStackTrace()
                Toaster.toast("Error al restaurar")
            } finally {
                snackbar.safeDismiss()
            }
        }
    }

    fun silentRestoreAll() {
        if (isLogedIn) {
            search(keyFavs, object : SearchInterface {
                override fun onResponse(backupObject: BackupObject<*>?) {
                    if (backupObject != null)
                        doAsync {
                            backupObject.data?.filterIsInstance<FavoriteObject>()?.let { CacheDB.INSTANCE.favsDAO().addAll(it) }
                            Log.e(TAG, "Favs sync")
                        }
                }
            })
            search(keySeen, object : SearchInterface {
                override fun onResponse(backupObject: BackupObject<*>?) {
                    if (backupObject != null)
                        doAsync {
                            backupObject.data?.filterIsInstance<AnimeObject.WebInfo.AnimeChapter>()?.let { CacheDB.INSTANCE.chaptersDAO().addAll(it) }
                            Log.e(TAG, "Seen sync")
                        }
                }
            })
            search(keyFollowing, object : SearchInterface {
                override fun onResponse(backupObject: BackupObject<*>?) {
                    if (backupObject != null)
                        doAsync {
                            backupObject.data?.filterIsInstance<SeeingObject>()?.let { CacheDB.INSTANCE.seeingDAO().addAll(it) }
                            Log.e(TAG, "Seen sync")
                        }
                }
            })
            search(keyHistory, object : SearchInterface {
                override fun onResponse(backupObject: BackupObject<*>?) {
                    if (backupObject != null)
                        doAsync {
                            backupObject.data?.filterIsInstance<RecordObject>()?.let { CacheDB.INSTANCE.recordsDAO().addAll(it) }
                            Log.e(TAG, "History sync")
                        }
                }
            })
        }
    }

    private fun getList(id: String): List<*> {
        return when (id) {
            keyFavs -> CacheDB.INSTANCE.favsDAO().allRaw
            keyHistory -> CacheDB.INSTANCE.recordsDAO().allRaw
            keyFollowing -> CacheDB.INSTANCE.seeingDAO().allRaw
            keySeen -> CacheDB.INSTANCE.chaptersDAO().all
            keyAchievements -> CacheDB.INSTANCE.achievementsDAO().all
            else -> mutableListOf<RecordObject>()
        }
    }

    private fun getType(id: String): Type {
        return when (id) {
            keyFavs -> object : TypeToken<BackupObject<FavoriteObject>>() {

            }.type
            keyHistory -> object : TypeToken<BackupObject<RecordObject>>() {

            }.type
            keyFollowing -> object : TypeToken<BackupObject<SeeingObject>>() {

            }.type
            keySeen -> object : TypeToken<BackupObject<AnimeObject.WebInfo.AnimeChapter>>() {

            }.type
            keyAchievements -> object : TypeToken<BackupObject<Achievement>>() {

            }.type
            keyAutoBackup -> object : TypeToken<AutoBackupObject>() {

            }.type
            else -> object : TypeToken<BackupObject<*>>() {

            }.type
        }
    }

    fun isAnimeflvInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("knf.animeflv", 0)
            AchievementManager.unlock(listOf(7))
            true
        } catch (e: Exception) {
            false
        }

    }

    private fun saveLastBackup() {
        PrefsUtil.lastBackup = SimpleDateFormat("dd/MM/yyyy kk:mm", Locale.getDefault()).format(Calendar.getInstance().time)
    }

    enum class BUType(var value: Int) {
        LOCAL(-1),
        DROPBOX(1)
    }

    interface LoginInterface {
        fun onLogin()
    }

    interface SearchInterface {
        fun onResponse(backupObject: BackupObject<*>?)
    }

    interface BackupInterface {
        fun onResponse(backupObject: BackupObject<*>?)
    }

    interface AutoBackupInterface {
        fun onResponse(backupObject: AutoBackupObject?)
    }

}
