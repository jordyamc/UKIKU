package knf.kuma.tv.sync

import androidx.annotation.DrawableRes
import knf.kuma.R

open class SyncObject {
    var isDropbox = false

    open val image: Int
        @DrawableRes
        get() = if (isDropbox) R.drawable.banner_dropbox else R.drawable.banner_drive

    open val title: String
        get() = if (isDropbox) "DropBox" else "Google Drive"

    internal constructor()

    constructor(isDropbox: Boolean) {
        this.isDropbox = isDropbox
    }
}
