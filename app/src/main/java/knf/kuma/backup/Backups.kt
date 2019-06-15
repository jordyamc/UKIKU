package knf.kuma.backup

import android.content.Context
import android.view.View
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import com.google.gson.reflect.TypeToken
import knf.kuma.App
import knf.kuma.achievements.AchievementManager
import knf.kuma.backup.framework.BackupService
import knf.kuma.backup.framework.DropBoxService
import knf.kuma.backup.framework.LocalService
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.*
import knf.kuma.database.CacheDB
import knf.kuma.pojos.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import java.text.SimpleDateFormat
import java.util.*

object Backups {

    private const val keyFavs = "favs"
    private const val keyHistory = "history"
    private const val keyFollowing = "following"
    private const val keySeen = "seen"
    const val keyAchievements = "achievements"
    const val keyAutoBackup = "autobackup"

    var type: Type
        get() = when (PreferenceManager.getDefaultSharedPreferences(App.context).getInt("backup_type", -1)) {
            1 -> Type.DROPBOX
            0 -> Type.LOCAL
            else -> Type.NONE
        }
        set(type) = PreferenceManager.getDefaultSharedPreferences(App.context).edit().putInt("backup_type", type.value).apply()

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
            doOnUI { snackbar?.safeDismiss() }
        }
    }

    fun backupAll() {
        GlobalScope.launch(Dispatchers.IO) {
            val service = createService()
            service?.backup(BackupObject(getList(keyFavs)), keyFavs)
            service?.backup(BackupObject(getList(keyHistory)), keyHistory)
            service?.backup(BackupObject(getList(keyFollowing)), keyFollowing)
            service?.backup(BackupObject(getList(keySeen)), keySeen)
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
            service?.search(keySeen)?.let { restore(null, false, keySeen, it) }
        }
    }

    private fun restore(view: View? = null, replace: Boolean, id: String, backupObject: BackupObject<*>) {
        val snackbar = view?.showSnackbar("Restaurando...", Snackbar.LENGTH_INDEFINITE)
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
                snackbar?.safeDismiss()
                view?.showSnackbar("Restauración completada")
            } catch (e: Exception) {
                e.printStackTrace()
                snackbar?.safeDismiss()
                view?.showSnackbar("Error al restaurar")
            }
        }
    }

    val isAnimeflvInstalled: Boolean
        get() =
            try {
                App.context.packageManager.getPackageInfo("knf.animeflv", 0)
                AchievementManager.unlock(listOf(7))
                true
            } catch (e: Exception) {
                false
            }

    private fun getList(id: String): List<*> {
        return when (id) {
            keyFavs -> CacheDB.INSTANCE.favsDAO().allRaw
            keyHistory -> CacheDB.INSTANCE.recordsDAO().allRaw
            keyFollowing -> CacheDB.INSTANCE.seeingDAO().allRaw
            keySeen -> CacheDB.INSTANCE.chaptersDAO().all
            keyAchievements -> CacheDB.INSTANCE.achievementsDAO().allCompleted
            else -> mutableListOf<RecordObject>()
        }
    }

    fun getType(id: String): java.lang.reflect.Type {
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

    fun saveLastBackup() {
        PrefsUtil.lastBackup = SimpleDateFormat("dd/MM/yyyy kk:mm", Locale.getDefault()).format(Calendar.getInstance().time)
    }

    enum class Type(var value: Int) {
        NONE(-1),
        LOCAL(0),
        DROPBOX(1)
    }

}