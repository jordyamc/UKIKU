package knf.kuma.backup.framework

import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import knf.kuma.backup.Backups
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.doOnUIGlobal
import knf.kuma.commons.noCrashLet
import knf.kuma.commons.safeContext
import java.io.File

class LocalService : BackupService() {

    private val gson = GsonBuilder()
        .setExclusionStrategies(object : ExclusionStrategy {
            override fun shouldSkipField(f: FieldAttributes): Boolean {
                return f.name.endsWith("\$delegate")
            }
            override fun shouldSkipClass(clazz: Class<*>?): Boolean = false
        }).create()
    private val baseFile by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            safeContext.getExternalFilesDir("backups") ?: File(safeContext.filesDir, "backups")
        } else {
            File(Environment.getExternalStorageDirectory(), "UKIKU/backups")
        }
    }

    override fun start() {
        if (!baseFile.exists())
            baseFile.mkdirs()
    }

    override val isLoggedIn: Boolean
        get() = true

    override fun logIn(token: String?): Boolean {
        Backups.type = Backups.Type.LOCAL
        return true
    }

    override fun logOut() {
    }

    override suspend fun search(id: String, manual: Boolean): BackupObject<*>? {
        val file = File(baseFile, "$id.backup")
        return if (file.exists()) {
            noCrashLet {
                val backup = gson.fromJson(file.readText().checkResponse(id), Backups.getType(id)) as BackupObject<*>
                check(backup.version == BackupObject.BACKUP_VERSION)
                backup
            }
        } else {
            if (manual && id != Backups.keyAutoBackup) {
                doOnUIGlobal { Toast.makeText(safeContext, "El archivo de respaldo necesita estar en ${file.path}", Toast.LENGTH_LONG).show() }
            }
            null
        }
    }

    override suspend fun backup(backupObject: BackupObject<*>, id: String): BackupObject<*>? {
        val file = File(baseFile, "$id.backup")
        return noCrashLet {
            file.writeText(gson.toJson(backupObject, Backups.getType(id)).checkData(id).also { Log.e("Backup", "ID: $id\n$it") })
            backupObject
        }
    }
}