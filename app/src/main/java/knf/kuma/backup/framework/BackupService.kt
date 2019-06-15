package knf.kuma.backup.framework

import knf.kuma.BuildConfig
import knf.kuma.backup.Backups
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.decrypt
import knf.kuma.commons.encrypt

abstract class BackupService {

    abstract fun start()

    abstract val isLoggedIn: Boolean

    abstract fun logIn(token: String? = null): Boolean

    abstract fun logOut()

    abstract suspend fun search(id: String): BackupObject<*>?

    abstract suspend fun backup(backupObject: BackupObject<*>, id: String): BackupObject<*>?

    fun String.checkResponse(id: String): String =
            if (id == Backups.keyAchievements)
                this.decrypt(BuildConfig.CIPHER_PWD)
            else this

    fun String.checkData(id: String): String =
            if (id == Backups.keyAchievements)
                this.encrypt(BuildConfig.CIPHER_PWD)
            else this

}