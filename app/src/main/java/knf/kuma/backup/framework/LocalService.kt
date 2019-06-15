package knf.kuma.backup.framework

import android.os.Environment
import com.google.gson.Gson
import knf.kuma.backup.Backups
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.noCrashLet
import java.io.File

class LocalService : BackupService() {

    private val baseFile = File(Environment.getExternalStorageDirectory(), "UKIKU/backups")

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

    override suspend fun search(id: String): BackupObject<*>? {
        val file = File(baseFile, "$id.backup")
        return if (file.exists()) {
            noCrashLet { Gson().fromJson(file.readText().checkResponse(id), Backups.getType(id)) as BackupObject<*> }
        } else null
    }

    override suspend fun backup(backupObject: BackupObject<*>, id: String): BackupObject<*>? {
        val file = File(baseFile, "$id.backup")
        return noCrashLet {
            file.writeText(Gson().toJson(backupObject, Backups.getType(id)).checkData(id))
            backupObject
        }
    }
}