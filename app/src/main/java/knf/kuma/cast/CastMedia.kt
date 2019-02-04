package knf.kuma.cast

import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.common.images.WebImage
import knf.kuma.commons.SelfServer
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.ExplorerObject
import knf.kuma.pojos.RecentObject

data class CastMedia(val url: String, val eid: String, val mediaInfo: MediaInfo) {

    val title: String get() = mediaInfo.metadata.getString(MediaMetadata.KEY_TITLE)
    val subTitle: String get() = mediaInfo.metadata.getString(MediaMetadata.KEY_SUBTITLE)
    val image: String get() = mediaInfo.metadata.images[0].url.toString()
    val type: String get() = mediaInfo.contentType

    companion object {
        fun create(chapter: AnimeObject.WebInfo.AnimeChapter?, url: String? = null): CastMedia? {
            if (chapter == null) return null
            val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(MediaMetadata.KEY_TITLE, chapter.name)
                putString(MediaMetadata.KEY_SUBTITLE, chapter.number)
                addImage(WebImage(Uri.parse(if (chapter.img.isNullOrBlank()) "https://animeflv.net/uploads/animes/thumbs/${chapter.aid}.jpg" else chapter.img)))
            }
            val fUrl = if (url.isNullOrBlank()) SelfServer.start(chapter.fileName, true) else url
            val mediaInfo = MediaInfo.Builder(fUrl).apply {
                setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                setContentType("video/mp4")
                setMetadata(metadata)
            }
            return CastMedia(fUrl ?: "", chapter.eid, mediaInfo.build())
        }

        fun create(recent: RecentObject?, url: String? = null): CastMedia? {
            if (recent == null) return null
            val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(MediaMetadata.KEY_TITLE, recent.name)
                putString(MediaMetadata.KEY_SUBTITLE, recent.chapter)
                addImage(WebImage(Uri.parse("https://animeflv.net/uploads/animes/thumbs/${recent.aid}.jpg")))
            }
            val fUrl = if (url.isNullOrBlank()) SelfServer.start(recent.fileName, true) else url
            val mediaInfo = MediaInfo.Builder(fUrl).apply {
                setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                setContentType("video/mp4")
                setMetadata(metadata)
            }
            return CastMedia(fUrl ?: "", recent.eid, mediaInfo.build())
        }

        fun create(fileDownObj: ExplorerObject.FileDownObj?): CastMedia? {
            if (fileDownObj == null) return null
            val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(MediaMetadata.KEY_TITLE, fileDownObj.title)
                putString(MediaMetadata.KEY_SUBTITLE, "Episodio ${fileDownObj.chapter}")
                addImage(WebImage(Uri.parse(fileDownObj.chapPreviewLink)))
            }
            val url = SelfServer.start(fileDownObj.fileName, true)
            val mediaInfo = MediaInfo.Builder(url).apply {
                setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                setContentType("video/mp4")
                setMetadata(metadata)
            }
            return CastMedia(url ?: "", fileDownObj.eid, mediaInfo.build())
        }
    }

}