package knf.kuma.player

import android.content.Intent
import android.support.v4.media.MediaDescriptionCompat
import knf.kuma.pojos.QueueObject

/**
 * Manages a set of media metadata that is used to create a playlist for [VideoActivity].
 */

open class MediaCatalog(private val list: MutableList<MediaDescriptionCompat>, private val intent: Intent, playList: List<QueueObject>) :
        MutableList<MediaDescriptionCompat> by list {

    companion object : MediaCatalog(mutableListOf(), Intent(), emptyList())

    init {
        if (intent.getBooleanExtra("isPlayList", false)) {
            var count = 1
            playList.forEach {
                list.add(
                        with(MediaDescriptionCompat.Builder()) {
                            setTitle(it.title())
                            setMediaId(count.toString())
                            setMediaUri(it.createUri())
                            build()
                        })
                count++
            }
        } else
            list.add(
                    with(MediaDescriptionCompat.Builder()) {
                        setTitle(intent.getStringExtra("title"))
                        setMediaId("1")
                        setMediaUri(intent.data)
                        build()
                    })
    }
}