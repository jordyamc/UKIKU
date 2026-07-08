package knf.kuma.cast

import android.net.Uri
import androidx.core.net.toUri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.common.images.WebImage
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.SelfServer
import knf.kuma.pojos.ExplorerObject
import knf.kuma.pojos.av1.Chapter
import knf.kuma.pojos.av1.DirectoryAV1
import knf.kuma.pojos.av1.RecentAV1

data class CastMedia(val url: String, val eid: String, val mediaInfo: MediaInfo) {

    val title: String get() = mediaInfo.metadata?.getString(MediaMetadata.KEY_TITLE)!!
    val subTitle: String get() = mediaInfo.metadata?.getString(MediaMetadata.KEY_SUBTITLE)!!
    val image: String get() = mediaInfo.metadata?.images!![0].url.toString()
    val type: String get() = mediaInfo.contentType!!

    companion object {

        fun create(anime: DirectoryAV1, chapter: Chapter, url: String? = null): CastMedia {
            val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(MediaMetadata.KEY_TITLE, chapter.name)
                putString(MediaMetadata.KEY_SUBTITLE, chapter.name)
                addImage(WebImage(chapter.thumbnail(anime).toUri()))
            }
            val fUrl = when {
                url.isNullOrBlank() -> SelfServer.start(chapter.filePath(anime), true)
                PrefsUtil.isProxyCastEnabled -> ProxyCache.start(url)
                else -> url
            }
            val mediaInfo = MediaInfo.Builder(fUrl!!).apply {
                setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                setContentType("video/mp4")
                setMetadata(metadata)
            }
            return CastMedia(fUrl, chapter.eid.toString(), mediaInfo.build())
        }

        fun create(recent: RecentAV1?, url: String? = null): CastMedia? {
            if (recent == null) return null
            val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(MediaMetadata.KEY_TITLE, recent.name)
                putString(MediaMetadata.KEY_SUBTITLE, recent.chapter)
                addImage(WebImage(recent.episodeImageUrl.toUri()))
            }
            val fUrl = when {
                url.isNullOrBlank() -> SelfServer.start(recent.getFilePath(), true)
                PrefsUtil.isProxyCastEnabled -> ProxyCache.start(url)
                else -> url
            }
            val mediaInfo = MediaInfo.Builder(fUrl!!).apply {
                setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                setContentType("video/mp4")
                setMetadata(metadata)
            }
            return CastMedia(fUrl, recent.eid.toString(), mediaInfo.build())
        }

        fun create(fileDownObj: ExplorerObject.FileDownObj?): CastMedia? {
            if (fileDownObj == null) return null
            val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(MediaMetadata.KEY_TITLE, fileDownObj.title)
                putString(MediaMetadata.KEY_SUBTITLE, "Episodio ${fileDownObj.chapter}")
                addImage(WebImage(Uri.parse(fileDownObj.chapPreviewLink)))
            }
            val url = SelfServer.start(
                fileDownObj.fileName.substring(fileDownObj.fileName.indexOf("$")),
                true
            )
            val mediaInfo = MediaInfo.Builder(url!!).apply {
                setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                setContentType("video/mp4")
                setMetadata(metadata)
            }
            return CastMedia(url, fileDownObj.eid, mediaInfo.build())
        }
    }

}