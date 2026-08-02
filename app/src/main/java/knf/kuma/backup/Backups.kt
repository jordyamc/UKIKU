package knf.kuma.backup

import android.content.Context
import android.view.View
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import com.google.gson.reflect.TypeToken
import knf.kuma.App
import knf.kuma.backup.framework.BackupService
import knf.kuma.backup.framework.DropBoxService
import knf.kuma.backup.framework.LocalService
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUIGlobal
import knf.kuma.commons.safeDismiss
import knf.kuma.commons.safeShow
import knf.kuma.commons.showSnackbar
import knf.kuma.database.CacheDB
import knf.kuma.pojos.Achievement
import knf.kuma.pojos.AutoBackupObject
import knf.kuma.pojos.RecordObject
import knf.kuma.pojos.av1.FavoriteAV1
import knf.kuma.pojos.av1.Organizer
import knf.kuma.pojos.av1.Record
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Backups {

    private const val keyFavs = "favs"
    private const val keyHistory = "history"
    private const val keyFollowing = "following"
    const val keyAchievements = "achievements"
    const val keyAutoBackup = "autobackup"

    var type: Type
        get() = when (PreferenceManager.getDefaultSharedPreferences(App.context).getInt("backup_type", -1)) {
            2 -> Type.FIRESTORE
            1 -> Type.DROPBOX
            0 -> Type.LOCAL
            else -> Type.NONE
        }
        set(type) = PreferenceManager.getDefaultSharedPreferences(App.context).edit {
            putInt(
                "backup_type",
                type.value
            )
        }

    fun createService(): BackupService? =
            when (type) {
                Type.DROPBOX -> DropBoxService()
                Type.LOCAL -> LocalService()
                else -> null
            }?.also { it.start() }

    fun search(backupService: BackupService? = null, id: String, onFound: (backupObject: BackupObject<*>?) -> Unit = {}) {
        GlobalScope.launch(Dispatchers.IO) {
            val service = backupService ?: createService()
            service?.search(id)?.let { onFound(it) } ?: onFound(null)
        }
    }

    fun backup(view: View? = null, backupService: BackupService? = null, id: String, onBackup: (backupObject: BackupObject<*>?) -> Unit = {}) {
        GlobalScope.launch(Dispatchers.IO) {
            val snackbar = view?.showSnackbar("Respaldando...", Snackbar.LENGTH_INDEFINITE)
            val service = backupService ?: createService()
            service?.backup(BackupObject(getList(id)), id)?.let { onBackup(it) } ?: onBackup(null)
            doOnUIGlobal { snackbar?.safeDismiss() }
        }
    }

    fun backupAll() {
        GlobalScope.launch(Dispatchers.IO) {
            val service = createService()
            service?.backup(BackupObject(getList(keyFavs)), keyFavs)
            service?.backup(BackupObject(getList(keyHistory)), keyHistory)
            service?.backup(BackupObject(getList(keyFollowing)), keyFollowing)
        }
    }

    fun restoreDialog(context: Context?, view: View, id: String, backupObject: BackupObject<*>?) {
        if (backupObject != null)
            context?.let {
                MaterialDialog(it).safeShow {
                    message(text = "¿Como desea restaurar?")
                    positiveButton(text = "mezclar") { restore(view, false, id, backupObject) }
                    negativeButton(text = "reemplazar") { restore(view, true, id, backupObject) }
                }
            }
    }

    fun restoreAll() {
        GlobalScope.launch(Dispatchers.IO) {
            val service = createService()
            service?.search(keyFavs)?.let { restore(null, false, keyFavs, it) }
            service?.search(keyHistory)?.let { restore(null, false, keyHistory, it) }
            service?.search(keyFollowing)?.let { restore(null, false, keyFollowing, it) }
        }
    }

    private fun restore(view: View? = null, replace: Boolean, id: String, backupObject: BackupObject<*>) {
        val snackbar = view?.showSnackbar("Restaurando...", Snackbar.LENGTH_INDEFINITE)
        doAsync {
            try {
                when (id) {
                    keyFavs -> {
                        if (replace)
                            CacheDB.INSTANCE.favoriteAV1DAO().clear()
                        (backupObject.data?.filterIsInstance<FavoriteAV1>() as? MutableList<FavoriteAV1>)?.let { CacheDB.INSTANCE.favoriteAV1DAO().addAll(it) }
                    }
                    keyHistory -> {
                        if (replace)
                            CacheDB.INSTANCE.recordAV1DAO().clear()
                        (backupObject.data?.filterIsInstance<Record>() as? MutableList<Record>)?.let { CacheDB.INSTANCE.recordAV1DAO().addAll(it) }
                    }
                    keyFollowing -> {
                        if (replace)
                            CacheDB.INSTANCE.organizerDAO().clear()
                        (backupObject.data?.filterIsInstance<Organizer>() as? MutableList<Organizer>)?.let { CacheDB.INSTANCE.organizerDAO().addAll(it) }
                    }
                }
                snackbar?.safeDismiss()
                view?.showSnackbar("Restauración completada")
            } catch (e: Exception) {
                e.printStackTrace()
                snackbar?.safeDismiss()
                view?.showSnackbar("Error al restaurar")
            }
        }
    }

    val isKeyInstalled: Boolean
        get() =
            try {
                App.context.packageManager.getPackageInfo("knf.kuma.key", 0)
                true
            } catch (e: Exception) {
                false
            }

    private fun getList(id: String): List<*> {
        return when (id) {
            keyFavs -> CacheDB.INSTANCE.favoriteAV1DAO().allRaw
            keyHistory -> CacheDB.INSTANCE.recordAV1DAO().all
            keyFollowing -> CacheDB.INSTANCE.organizerDAO().allRaw
            keyAchievements -> CacheDB.INSTANCE.achievementsDAO().all
            else -> mutableListOf<RecordObject>()
        }
    }

    fun getType(id: String): java.lang.reflect.Type {
        return when (id) {
            keyFavs -> object : TypeToken<BackupObject<FavoriteAV1>>() {

            }.type
            keyHistory -> object : TypeToken<BackupObject<Record>>() {

            }.type
            keyFollowing -> object : TypeToken<BackupObject<Organizer>>() {

            }.type
            keyAchievements -> object : TypeToken<BackupObject<Achievement>>() {

            }.type
            keyAutoBackup -> object : TypeToken<AutoBackupObject>() {

            }.type
            else -> object : TypeToken<BackupObject<*>>() {

            }.type
        }
    }

    fun saveLastBackup() {
        PrefsUtil.lastBackup = SimpleDateFormat("dd/MM/yyyy kk:mm", Locale.getDefault()).format(Calendar.getInstance().time)
    }

    enum class Type(var value: Int) {
        NONE(-1),
        LOCAL(0),
        DROPBOX(1),
        FIRESTORE(2)
    }

}