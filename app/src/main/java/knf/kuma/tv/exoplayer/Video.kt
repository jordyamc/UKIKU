package knf.kuma.tv.exoplayer

import android.net.Uri
import android.os.Bundle

class Video(bundle: Bundle?) {
    internal var uri: Uri = Uri.parse(bundle?.getString("url"))
    internal var title: String? = null
    internal var chapter: String? = null
    internal var cookies: String? = null

    init {
        this.title = bundle?.getString("title")
        this.chapter = bundle?.getString("chapter")
        this.cookies = bundle?.getString("cookies")
    }
}
