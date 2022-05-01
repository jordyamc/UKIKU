package knf.kuma.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.webkit.URLUtil
import androidx.media.AudioAttributesCompat
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.rxjava3.disposables.Disposable
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.noCrash
import knf.kuma.database.CacheDB
import knf.kuma.pojos.QueueObject
import okhttp3.OkHttpClient
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
    var retriever = MediaMetadataRetriever()
    private val mediaCatalog: MediaCatalog
    private var disposable: Disposable? = null
    private var lastPosition = 0L

    // Create the exoPlayer instance.
    init {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioAttributes = AudioAttributesCompat.Builder()
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                .build()
        playerCallback = context as PlayerCallback
        mediaCatalog = MediaCatalog(mutableListOf(), intent, playList)
        playerCallback.onChangeTitle((mediaCatalog[listPosition].title ?: "").toString())
        mediaCatalog[listPosition].title?.let { playerState.title = it.toString() }
        setUpRetriever(mediaCatalog[listPosition])
        if (mediaCatalog.size == 1) playerCallback.onChangeTitle(mediaCatalog[0].title.toString())
        audioFocusPlayer = AudioFocusWrapper(
                audioAttributes,
                audioManager,
            ExoPlayer.Builder(context).build()
                        .also { player ->
                            playerView.player = player
                        }
        )
        /*disposable = Observable.interval(1, TimeUnit.SECONDS).map { audioFocusPlayer.currentPosition }
                .subscribeOn(AndroidSchedulers.from(audioFocusPlayer.applicationLooper, false))
                .subscribe {
                    if (it > 0)
                        lastPosition = it
                }*/
    }

    private fun setUpRetriever(description: MediaDescriptionCompat) {
        description.mediaUri?.let {
            noCrash {
                if (URLUtil.isFileUrl(it.toString()))
                    retriever.setDataSource(context, it)
                else
                    retriever.setDataSource(it.toString(), mapOf())
            }
        }
    }

    private fun buildMediaSource(): List<MediaItem> {
        return mediaCatalog.map { createExtractorMediaSource(it.mediaUri ?: Uri.EMPTY) }
    }

    private fun createExtractorMediaSource(uri: Uri): MediaItem {
        val item = MediaItem.fromUri(uri)
        if (intent.getBooleanExtra("isFile", false)) return item
        return ProgressiveMediaSource.Factory(
            if (PrefsUtil.useExperimentalOkHttp)
                DefaultDataSource.Factory(
                    context,
                    OkHttpDataSource.Factory(OkHttpClient()).apply {
                        setUserAgent(BypassUtil.userAgent)
                    }
                )
            else
                DefaultHttpDataSource.Factory().apply {
                    setUserAgent(BypassUtil.userAgent)
                }
        ).createMediaSource(item).mediaItem
    }

    // Prepare playback.
    fun start() {
        // Load media.
        audioFocusPlayer.setMediaItems(buildMediaSource())
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
            stop(true)
        }
    }

    fun skip() {
        with(audioFocusPlayer) {
            seekTo(currentWindowIndex, currentPosition + 85000)
        }
    }

    fun saveState() {
        with(audioFocusPlayer) {
            with(playerState) {
                position = currentPosition
                window = currentWindowIndex
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
    private fun attachLogging(exoPlayer: ExoPlayer) {
        // Show toasts on state changes.
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
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
                FirebaseCrashlytics.getInstance().recordException(error)
                playerCallback.onFinish()
            }

            override fun onLoadingChanged(isLoading: Boolean) {
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
                        setUpRetriever(mediaCatalog[listPosition])
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            /*override fun onPositionDiscontinuity(reason: Int) {
                try {
                    val latestPosition = audioFocusPlayer.currentWindowIndex
                    if (latestPosition != listPosition) {
                        playerState.apply {
                            title = playList[listPosition].title()
                            if (reason == 0) {
                                position = 0
                            } else if (reason in 1..2) {
                                position = lastPosition
                            }
                        }
                        doAsync {
                                CacheDB.INSTANCE.playerStateDAO().set(playerState)
                        }
                        listPosition = latestPosition
                        playerCallback.onChangeTitle((mediaCatalog[listPosition].title
                                ?: "").toString())
                        setUpRetriever(mediaCatalog[listPosition])
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }*/
        })
        // Write to log on state changes.
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                //Log.i("Player", "playerStateChanged: ${getStateString(playbackState)}, $playWhenReady")
                playerCallback.onPlayerStateChanged(playbackState, playWhenReady)
            }

            fun getStateString(state: Int): String {
                return when (state) {
                    Player.STATE_BUFFERING -> "STATE_BUFFERING"
                    Player.STATE_ENDED -> "STATE_ENDED"
                    Player.STATE_IDLE -> "STATE_IDLE"
                    Player.STATE_READY -> "STATE_READY"
                    else -> "?"
                }
            }


        })

    }

    interface PlayerCallback {
        fun onChangeTitle(title: String)
        fun onLoadingChange(loading: Boolean)
        fun onPlayerStateChanged(state: Int, playWhenReady: Boolean)
        fun onFinish()
    }

}