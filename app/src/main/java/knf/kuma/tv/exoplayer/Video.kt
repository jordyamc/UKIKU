package knf.kuma.tv.exoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle

class Video(intent: Intent) {
    internal var uri: Uri = Uri.parse(intent.dataString)
    internal var title: String? = null
    internal var chapter: String? = null
    internal var headers: Map<String, String>? = null

    init {
        this.title = intent.getStringExtra("title")
        this.chapter = intent.getStringExtra("chapter")
        this.headers = intent.getStringArrayListExtra("headers")?.chunked(2)?.associate { Pair(it[0], it[1]) }?.ifEmpty { null }
    }
}
