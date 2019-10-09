package knf.kuma.tv.sync

import androidx.annotation.DrawableRes
import knf.kuma.R
import knf.kuma.backup.Backups

open class SyncObject {
    var type = Backups.Type.NONE

    open val image: Int
        @DrawableRes
        get() = when (type) {
            Backups.Type.DROPBOX -> R.drawable.banner_dropbox
            Backups.Type.FIRESTORE -> R.drawable.banner_firestore
            else -> R.drawable.banner_drive
        }

    open val title: String
        get() = when (type) {
            Backups.Type.DROPBOX -> "Dropbox"
            Backups.Type.FIRESTORE -> "Firestore"
            else -> "Google Drive"
        }

    internal constructor()

    constructor(type: Backups.Type) {
        this.type = type
    }
}
