package knf.kuma.tv.sync

import knf.kuma.R

class BypassObject : SyncObject() {

    override val image: Int
        get() = R.drawable.banner_cloudflare

    override val title: String
        get() = "Recrear bypass"
}
