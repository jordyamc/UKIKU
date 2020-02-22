package knf.kuma.tv.exoplayer

import android.net.Uri
import android.os.Bundle

class Video(bundle: Bundle?) {
    internal var uri: Uri = Uri.parse(bundle?.getString("url"))
    internal var title: String? = null
    internal var chapter: String? = null
    internal var headers: HashMap<String, String>? = null

    init {
        this.title = bundle?.getString("title")
        this.chapter = bundle?.getString("chapter")
        this.headers = bundle?.getSerializable("headers") as? HashMap<String, String>
    }
}
