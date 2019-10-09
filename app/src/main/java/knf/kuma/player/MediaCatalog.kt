package knf.kuma.player

import android.content.Intent
import android.support.v4.media.MediaDescriptionCompat
import knf.kuma.database.CacheDB

/**
 * Manages a set of media metadata that is used to create a playlist for [VideoActivity].
 */

open class MediaCatalog(private val list: MutableList<MediaDescriptionCompat>, private val intent: Intent) :
        MutableList<MediaDescriptionCompat> by list {

    companion object : MediaCatalog(mutableListOf(), Intent())

    init {
        if (intent.getBooleanExtra("isPlayList", false)) {
            val playList = CacheDB.INSTANCE.queueDAO().getAllByAid(intent.getStringExtra("playlist"))
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