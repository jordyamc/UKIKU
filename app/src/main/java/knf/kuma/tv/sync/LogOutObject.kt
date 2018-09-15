package knf.kuma.tv.sync

import knf.kuma.R

class LogOutObject : SyncObject() {

    override val image: Int
        get() = R.drawable.banner_signout

    override val title: String
        get() = "Cerrar sesión"
}
