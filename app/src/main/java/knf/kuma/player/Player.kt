package knf.kuma.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import androidx.annotation.OptIn
import androidx.media.AudioAttributesCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.afollestad.materialdialogs.MaterialDialog
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.crashlytics
import io.reactivex.rxjava3.disposables.Disposable
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.noCrashLetNullable
import knf.kuma.database.CacheDB
import knf.kuma.pojos.QueueObject
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster

@Entity
data class PlayerState(
        @PrimaryKey
        var title: String = "",
        var position: Long = 0,
        @Ignore
        var window: Int = 0,
        @Ignore
        var whenReady: Boolean = true,
        @Ignore
        var isFinishing: Boolean = false)

class PlayerHolder(
    private val context: Context,
    private val playerState: PlayerState,
    private val playerView: PlayerView,
    private val intent: Intent,
    private val playList: List<QueueObject>
) {
    val audioFocusPlayer: ExoPlayer
    val playerCallback: PlayerCallback
    private var listPosition = 0
    private val mediaCatalog: MediaCatalog
    private var disposable: Disposable? = null
    private var lastPosition = 0L

    // Create the exoPlayer instance.
    init {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioAttributes = AudioAttributesCompat.Builder()
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE)
                .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                .build()
        playerCallback = context as PlayerCallback
        mediaCatalog = MediaCatalog(mutableListOf(), intent, playList)
        playerCallback.onChangeTitle((mediaCatalog[listPosition].title ?: "").toString())
        mediaCatalog[listPosition].title?.let { playerState.title = it.toString() }
        if (mediaCatalog.size == 1) playerCallback.onChangeTitle(mediaCatalog[0].title.toString())
        audioFocusPlayer = AudioFocusWrapper(
                audioAttributes,
                audioManager,
            ExoPlayer.Builder(context).build()
                        .also { player ->
                            playerView.player = player
                        }
        )
    }

    private fun buildMediaSource(): List<MediaData> {
        return mediaCatalog.mapNotNull { noCrashLetNullable { createExtractorMediaSource(it) } }
    }

    @OptIn(UnstableApi::class)
    private fun createExtractorMediaSource(descriptor: MediaDescriptionCompat): MediaData {
        val item = MediaItem.fromUri(descriptor.mediaUri?: Uri.EMPTY).buildUpon()
            .setMediaMetadata(MediaMetadata.Builder()
                .setTitle(descriptor.title)
                .build()
            ).build()
        if (intent.getBooleanExtra("isFile", false)) return MediaData(item)
        val httpFactory = DefaultHttpDataSource.Factory().apply {
            descriptor.extras?.getStringArray("headers")?.let { headerArray ->
                val headers = headerArray.toList().chunked(2).associate { Pair(it[0], it[1]) }
                setDefaultRequestProperties(headers)
                if (headers.contains("User-Agent")) {
                    setUserAgent(headers["User-Agent"])
                } else {
                    setUserAgent(BypassUtil.userAgent)
                }
            } ?: setUserAgent(BypassUtil.userAgent)
        }
        val url = descriptor.mediaUri?.toString()
        val factory = if (url?.contains("m3u8") == true || url?.contains("master.") == true) {
            HlsMediaSource.Factory(httpFactory)
        } else {
            ProgressiveMediaSource.Factory(httpFactory)
        }
        return MediaData(factory.createMediaSource(item))
    }

    class MediaData {
        constructor(media: MediaSource) {
            mediaSource = media
        }

        constructor(media: MediaItem) {
            mediaItem = media
        }
        private lateinit var mediaSource: MediaSource
        private lateinit var mediaItem: MediaItem

        @OptIn(UnstableApi::class)
        fun addMedia(exoPlayer: ExoPlayer) {
            if (::mediaSource.isInitialized) {
                exoPlayer.addMediaSource(mediaSource)
            }
            if (::mediaItem.isInitialized) {
                exoPlayer.addMediaItem(mediaItem)
            }
        }
    }

    // Prepare playback.
    fun start() {
        // Load media.
        buildMediaSource().forEach {
            it.addMedia(audioFocusPlayer)
        }
        //audioFocusPlayer.setMediaItems(buildMediaSource())
        audioFocusPlayer.prepare()
        // Restore state (after onResume()/onStart())
        with(playerState) {
            // Start playback when media has buffered enough
            // (whenReady is true by default).
            audioFocusPlayer.seekTo(window, position)
            audioFocusPlayer.playWhenReady = whenReady
            // Add logging.
            attachLogging(audioFocusPlayer)
        }
    }

    // Stop playback and release resources, but re-use the exoPlayer instance.
    fun stop() {
        with(audioFocusPlayer) {
            // Save state
            saveState()
            // Stop the exoPlayer (and release it's resources). The exoPlayer instance can be reused.
            stop()
            clearMediaItems()
        }
    }

    fun skip() {
        with(audioFocusPlayer) {
            seekTo(currentMediaItemIndex, currentPosition + 85000)
        }
    }

    fun saveState() {
        with(audioFocusPlayer) {
            with(playerState) {
                position = currentPosition
                window = currentMediaItemIndex
                whenReady = playWhenReady
            }
        }
    }

    // Destroy the exoPlayer instance.
    fun release() {
        audioFocusPlayer.release() // exoPlayer instance can't be used again.
        disposable?.dispose()
    }

    /**
     * For more info on ExoPlayer logging, please review this
     * [codelab](https://codelabs.developers.google.com/codelabs/exoplayer-intro/#5).
     */
    @OptIn(UnstableApi::class)
    private fun attachLogging(exoPlayer: ExoPlayer) {
        // Show toasts on state changes.
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        playerCallback.onFinish()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Toaster.toast("Error al reproducir: " + error.message?.replace("%", "%%"))
                MaterialDialog(this@PlayerHolder.context).show {
                    message(text = error.stackTraceToString().also {
                        (this@PlayerHolder.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(
                            ClipData.newPlainText("stack", it)
                        )
                    })
                    positiveButton(text = "OK")
                }
                Firebase.crashlytics.recordException(error, CustomKeysAndValues.Builder().putString("link", intent.dataString?:"Empty").putString("extras", intent.extras.toString()).build())
                playerCallback.onFinish()
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                playerCallback.onLoadingChange(isLoading)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                try {
                    val latestPosition = newPosition.mediaItemIndex
                    if (latestPosition != oldPosition.mediaItemIndex) {
                        playerState.apply {
                            title = playList[listPosition].title()
                            if (reason == 0) {
                                position = 0
                            } else if (reason in 1..2) {
                                position = oldPosition.positionMs
                            }
                        }
                        doAsync {
                            CacheDB.INSTANCE.playerStateDAO().set(playerState)
                        }
                        listPosition = latestPosition
                        playerCallback.onChangeTitle(
                            (mediaCatalog[listPosition].title
                                ?: "").toString()
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
        // Write to log on state changes.
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                playerCallback.onPlayerStateChanged(playbackState)
            }
        })

    }

    interface PlayerCallback {
        fun onChangeTitle(title: String)
        fun onLoadingChange(loading: Boolean)
        fun onPlayerStateChanged(state: Int)
        fun onFinish()
    }

}