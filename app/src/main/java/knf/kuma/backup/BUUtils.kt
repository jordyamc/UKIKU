package knf.kuma.backup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.crashlytics.android.Crashlytics
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.drive.*
import com.google.android.gms.drive.query.Filters
import com.google.android.gms.drive.query.Query
import com.google.android.gms.drive.query.SearchableField
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import knf.kuma.achievements.AchievementManager
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.safeDismiss
import knf.kuma.commons.safeShow
import knf.kuma.commons.showSnackbar
import knf.kuma.database.CacheDB
import knf.kuma.pojos.*
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@SuppressLint("StaticFieldLeak")
object BUUtils {
    const val LOGIN_CODE = 56478
    private var activity: Activity? = null
    private var loginInterface: LoginInterface? = null
    private var DRC: DriveResourceClient? = null
    private var DBC: DbxClientV2? = null

    val isLogedIn: Boolean
        get() = DRC != null || DBC != null

    private val dbToken: String?
        get() = PreferenceManager.getDefaultSharedPreferences(activity).getString("db_token", null)

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

    fun setDriveClient() {
        activity?.let {
            val account = GoogleSignIn.getLastSignedInAccount(it)
            if (account != null)
                DRC = Drive.getDriveResourceClient(it, account)
            loginInterface?.onLogin()
        }
    }

    private fun setDriveClient(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null)
            DRC = Drive.getDriveResourceClient(context, account)
        loginInterface?.onLogin()
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

    private fun clearGoogleAccount() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(Drive.SCOPE_APPFOLDER)
                .build()
        val client = activity?.let { GoogleSignIn.getClient(it, signInOptions) }
        client?.signOut()
    }

    fun startClient(type: BUType, fromInit: Boolean) {
        when (type) {
            BUUtils.BUType.DRIVE -> if (!fromInit) {
                val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_APPFOLDER)
                        .build()
                activity?.startActivityForResult(activity?.let { GoogleSignIn.getClient(it, signInOptions).signInIntent }
                        ?: Intent(), LOGIN_CODE)
            } else {
                setDriveClient()
            }
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
            BUUtils.BUType.DRIVE -> setDriveClient(context)
            BUUtils.BUType.DROPBOX -> if (getDBToken(context) != null)
                setDropBoxClient(context, getDBToken(context))
            else -> {
            }
        }
    }

    fun logOut() {
        DRC = null
        DBC = null
        when (getType(activity)) {
            BUUtils.BUType.DROPBOX -> clearDBToken()
            BUUtils.BUType.DRIVE -> clearGoogleAccount()
            else -> {
            }
        }
        type = BUType.LOCAL
    }

    fun getType(context: Context?): BUType {
        return when (PreferenceManager.getDefaultSharedPreferences(context).getInt("backup_type", -1)) {
            -1 -> BUType.LOCAL
            0 -> BUType.DRIVE
            1 -> BUType.DROPBOX
            else -> BUType.LOCAL
        }
    }

    fun isConnected(context: Context): Boolean {
        return getType(context) != BUType.LOCAL
    }

    fun search(id: String, searchInterface: SearchInterface) {
        when (type) {
            BUUtils.BUType.DRIVE -> searchDrive(id, searchInterface)
            BUUtils.BUType.DROPBOX -> searchDropbox(id, searchInterface)
            else -> {
            }
        }
    }

    fun search(context: Context, id: String, searchInterface: SearchInterface) {
        when (getType(context)) {
            BUUtils.BUType.DRIVE -> searchDriveNC(id, searchInterface)
            BUUtils.BUType.DROPBOX -> searchDropbox(id, searchInterface)
            else -> {
            }
        }
    }

    fun backup(view: View, id: String, backupInterface: BackupInterface) {
        when (type) {
            BUUtils.BUType.DRIVE -> backupDrive(view, id, backupInterface)
            BUUtils.BUType.DROPBOX -> backupDropbox(view, id, backupInterface)
            else -> {
            }
        }
    }

    fun backupNUI(context: Context, id: String, backupInterface: BackupInterface) {
        when (getType(context)) {
            BUUtils.BUType.DRIVE -> backupDriveNUI(id, backupInterface)
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
        backupNUI(context, "favs", backupInterface)
        backupNUI(context, "history", backupInterface)
        backupNUI(context, "following", backupInterface)
        backupNUI(context, "seen", backupInterface)
        while (i.get() != 4) {
            //
        }
    }

    fun waitAutoBackup(context: Context): AutoBackupObject? {
        val tBackupObject = AtomicReference<AutoBackupObject>(FakeAutoBackup())
        search(context, "autobackup", object : SearchInterface {
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
        when (type) {
            BUUtils.BUType.DRIVE -> backupDrive(backupObject, backupInterface)
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
                    searchInterface.onResponse(Gson().fromJson<Any>(InputStreamReader(downloader?.inputStream), getType(id)) as? BackupObject<*>)
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

    private fun searchDrive(id: String, searchInterface: SearchInterface) {
        doAsync {
            try {
                val appFolderTask = DRC?.appFolder
                appFolderTask?.continueWithTask {
                    val appfolder = appFolderTask.result
                    val query = Query.Builder()
                            .addFilter(Filters.contains(SearchableField.TITLE, id))
                            .build()
                    appfolder?.let { DRC?.queryChildren(it, query) }
                }?.continueWithTask<DriveContents> { task ->
                    val metadata = task.result
                    if (metadata?.count ?: 0 > 0) {
                        val driveFile = metadata?.get(0)?.driveId?.asDriveFile()
                        metadata?.release()
                        driveFile?.let { DRC?.openFile(it, DriveFile.MODE_READ_ONLY) }
                    } else {
                        metadata?.release()
                        null
                    }
                }?.apply {
                    activity?.let {
                        addOnSuccessListener(it) { driveContents ->
                            try {
                                searchInterface.onResponse(Gson().fromJson<BackupObject<*>>(InputStreamReader(driveContents.inputStream), getType(id)))
                            } catch (e: Exception) {
                                e.printStackTrace()
                                searchInterface.onResponse(null)
                            }
                        }
                        addOnFailureListener(it) { e ->
                            e.printStackTrace()
                            searchInterface.onResponse(null)
                        }
                    }
                }
            } catch (e: Exception) {
                Crashlytics.logException(e)
                searchInterface.onResponse(null)
            }
        }
    }

    private fun searchDriveNC(id: String, searchInterface: SearchInterface) {
        doAsync {
            try {
                val appFolderTask = DRC?.appFolder
                appFolderTask?.continueWithTask {
                    val appfolder = appFolderTask.result
                    val query = Query.Builder()
                            .addFilter(Filters.contains(SearchableField.TITLE, id))
                            .build()
                    appfolder?.let { DRC?.queryChildren(it, query) }
                }?.continueWithTask<DriveContents> { task ->
                    val metadata = task.result
                    if (metadata?.count ?: 0 > 0) {
                        val driveFile = metadata?.get(0)?.driveId?.asDriveFile()
                        metadata?.release()
                        driveFile?.let { DRC?.openFile(it, DriveFile.MODE_READ_ONLY) }
                    } else {
                        metadata?.release()
                        null
                    }
                }?.apply {
                    addOnSuccessListener { driveContents ->
                        try {
                            searchInterface.onResponse(Gson().fromJson<BackupObject<*>>(InputStreamReader(driveContents.inputStream), getType(id)))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            searchInterface.onResponse(null)
                        }
                    }
                    addOnFailureListener { e ->
                        e.printStackTrace()
                        searchInterface.onResponse(null)
                    }
                }
            } catch (e: Exception) {
                Crashlytics.logException(e)
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
            } catch (e: Exception) {
                e.printStackTrace()
                backupInterface.onResponse(null)
            }
        }
    }

    private fun backupDropbox(backupObject: AutoBackupObject, backupInterface: AutoBackupInterface) {
        doAsync {
            try {
                DBC?.files()?.uploadBuilder("/autobackup")
                        ?.withMute(true)
                        ?.withMode(WriteMode.OVERWRITE)
                        ?.uploadAndFinish(ByteArrayInputStream(Gson().toJson(backupObject, getType("autobackup")).toByteArray(StandardCharsets.UTF_8)))
                backupInterface.onResponse(backupObject)
            } catch (e: Exception) {
                e.printStackTrace()
                backupInterface.onResponse(null)
            }
        }
    }

    private fun backupDrive(view: View, id: String, backupInterface: BackupInterface) {
        val snackbar = view.showSnackbar("Respaldando...", Snackbar.LENGTH_INDEFINITE)
        doAsync {
            val appFolderTask = DRC?.appFolder
            val driveContents = DRC?.createContents()
            val backupObject = BackupObject(getList(id))
            val result = Tasks.whenAll(appFolderTask, driveContents)
                    .continueWithTask {
                        val query = Query.Builder()
                                .addFilter(Filters.contains(SearchableField.TITLE, id))
                                .build()
                        appFolderTask?.result?.let { DRC?.queryChildren(it, query) }
                    }.continueWithTask { task ->
                        val metadata = task.result
                        if (metadata != null && metadata.count > 0)
                            metadata.get(0)?.driveId?.asDriveResource()?.let { DRC?.delete(it) }
                        metadata?.release()
                        val contents = driveContents?.result
                        val outputStream = contents?.outputStream
                        OutputStreamWriter(outputStream).use { writer -> writer.write(Gson().toJson(backupObject, getType(id))) }
                        val changeSet = MetadataChangeSet.Builder()
                                .setTitle(id)
                                .setMimeType("application/json")
                                .setStarred(true)
                                .build()

                        appFolderTask?.result?.let { DRC?.createFile(it, changeSet, contents) }
                    }.apply {
                        activity?.let {
                            addOnSuccessListener(it) {
                                snackbar.safeDismiss()
                                backupInterface.onResponse(backupObject)
                            }
                            addOnFailureListener(it) {
                                snackbar.safeDismiss()
                                backupInterface.onResponse(null)
                            }
                        }
                    }
        }
    }

    private fun backupDriveNUI(id: String, backupInterface: BackupInterface) {
        doAsync {
            val appFolderTask = DRC?.appFolder
            val driveContents = DRC?.createContents()
            val backupObject = BackupObject(getList(id))
            val result = Tasks.whenAll(appFolderTask, driveContents)
                    .continueWithTask {
                        val query = Query.Builder()
                                .addFilter(Filters.contains(SearchableField.TITLE, id))
                                .build()
                        appFolderTask?.result?.let { DRC?.queryChildren(it, query) }
                    }.continueWithTask { task ->
                        val metadata = task.result
                        if (metadata != null && metadata.count > 0)
                            DRC?.delete(metadata.get(0).driveId.asDriveResource())
                        metadata?.release()
                        val contents = driveContents?.result
                        val outputStream = contents?.outputStream
                        OutputStreamWriter(outputStream).use { writer -> writer.write(Gson().toJson(backupObject, getType(id))) }
                        val changeSet = MetadataChangeSet.Builder()
                                .setTitle(id)
                                .setMimeType("application/json")
                                .setStarred(true)
                                .build()

                        appFolderTask?.result?.let { DRC?.createFile(it, changeSet, contents) }
                    }
                    .addOnSuccessListener { backupInterface.onResponse(backupObject) }
                    .addOnFailureListener { backupInterface.onResponse(null) }
        }
    }

    private fun backupDrive(backupObject: AutoBackupObject, backupInterface: AutoBackupInterface) {
        doAsync {
            val appFolderTask = DRC?.appFolder
            val driveContents = DRC?.createContents()
            val result = Tasks.whenAll(appFolderTask, driveContents)
                    .continueWithTask {
                        val query = Query.Builder()
                                .addFilter(Filters.contains(SearchableField.TITLE, "autobackup"))
                                .build()
                        appFolderTask?.result?.let { DRC?.queryChildren(it, query) }
                    }.continueWithTask { task ->
                        val metadata = task.result
                        if (metadata != null && metadata.count > 0)
                            DRC?.delete(metadata.get(0).driveId.asDriveResource())
                        metadata?.release()
                        val contents = driveContents?.result
                        val outputStream = contents?.outputStream

                        OutputStreamWriter(outputStream).use { writer -> writer.write(Gson().toJson(backupObject, getType("autobackup"))) }
                        val changeSet = MetadataChangeSet.Builder()
                                .setTitle("autobackup")
                                .setMimeType("application/json")
                                .setStarred(true)
                                .build()

                        appFolderTask?.result?.let { DRC?.createFile(it, changeSet, contents) }
                    }.apply {
                        activity?.let {
                            addOnSuccessListener(it) { backupInterface.onResponse(backupObject) }
                            addOnFailureListener(it) { backupInterface.onResponse(null) }
                        }
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
                    "favs" -> {
                        if (replace)
                            CacheDB.INSTANCE.favsDAO().clear()
                        (backupObject.data?.filterIsInstance<FavoriteObject>() as? MutableList<FavoriteObject>)?.let { CacheDB.INSTANCE.favsDAO().addAll(it) }
                    }
                    "history" -> {
                        if (replace)
                            CacheDB.INSTANCE.recordsDAO().clear()
                        (backupObject.data?.filterIsInstance<RecordObject>() as? MutableList<RecordObject>)?.let { CacheDB.INSTANCE.recordsDAO().addAll(it) }
                    }
                    "following" -> {
                        if (replace)
                            CacheDB.INSTANCE.seeingDAO().clear()
                        (backupObject.data?.filterIsInstance<SeeingObject>() as? MutableList<SeeingObject>)?.let { CacheDB.INSTANCE.seeingDAO().addAll(it) }
                    }
                    "seen" -> {
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
            search("favs", object : SearchInterface {
                override fun onResponse(backupObject: BackupObject<*>?) {
                    if (backupObject != null)
                        doAsync {
                            backupObject.data?.filterIsInstance<FavoriteObject>()?.let { CacheDB.INSTANCE.favsDAO().addAll(it) }
                            Log.e("Sync", "Favs sync")
                        }
                }
            })
            search("seen", object : SearchInterface {
                override fun onResponse(backupObject: BackupObject<*>?) {
                    if (backupObject != null)
                        doAsync {
                            backupObject.data?.filterIsInstance<AnimeObject.WebInfo.AnimeChapter>()?.let { CacheDB.INSTANCE.chaptersDAO().addAll(it) }
                            Log.e("Sync", "Seen sync")
                        }
                }
            })
            search("following", object : SearchInterface {
                override fun onResponse(backupObject: BackupObject<*>?) {
                    if (backupObject != null)
                        doAsync {
                            backupObject.data?.filterIsInstance<SeeingObject>()?.let { CacheDB.INSTANCE.seeingDAO().addAll(it) }
                            Log.e("Sync", "Seen sync")
                        }
                }
            })
            search("history", object : SearchInterface {
                override fun onResponse(backupObject: BackupObject<*>?) {
                    if (backupObject != null)
                        doAsync {
                            backupObject.data?.filterIsInstance<RecordObject>()?.let { CacheDB.INSTANCE.recordsDAO().addAll(it) }
                            Log.e("Sync", "History sync")
                        }
                }
            })
        }
    }

    private fun getList(id: String): List<*> {
        return when (id) {
            "favs" -> CacheDB.INSTANCE.favsDAO().allRaw
            "history" -> CacheDB.INSTANCE.recordsDAO().allRaw
            "following" -> CacheDB.INSTANCE.seeingDAO().allRaw
            "seen" -> CacheDB.INSTANCE.chaptersDAO().all
            "achievements" -> CacheDB.INSTANCE.achievementsDAO().all
            else -> mutableListOf<RecordObject>()
        }
    }

    private fun getType(id: String): Type {
        return when (id) {
            "favs" -> object : TypeToken<BackupObject<FavoriteObject>>() {

            }.type
            "history" -> object : TypeToken<BackupObject<RecordObject>>() {

            }.type
            "following" -> object : TypeToken<BackupObject<SeeingObject>>() {

            }.type
            "seen" -> object : TypeToken<BackupObject<AnimeObject.WebInfo.AnimeChapter>>() {

            }.type
            "achievements" -> object : TypeToken<BackupObject<Achievement>>() {

            }.type
            "autobackup" -> object : TypeToken<AutoBackupObject>() {

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

    enum class BUType(var value: Int) {
        LOCAL(-1),
        DRIVE(0),
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
