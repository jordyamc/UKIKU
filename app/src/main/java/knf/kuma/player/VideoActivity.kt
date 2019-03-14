package knf.kuma.player

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.rubensousa.previewseekbar.PreviewLoader
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.doOnUI
import knf.kuma.commons.noCrash
import kotlinx.android.synthetic.main.exo_playback_control_view.*
import kotlinx.android.synthetic.main.player_view.*
import org.jetbrains.anko.doAsync
import java.util.concurrent.Future

/**
 * Allows playback of videos that are in a playlist, using [PlayerHolder] to load the and render
 * it to the [com.google.android.exoplayer2.ui.PlayerView] to render the video output. Supports
 * [MediaSessionCompat] and picture in picture as well.
 */

class VideoActivity : AppCompatActivity(), PlayerHolder.PlayerCallback, PreviewLoader {
    private val mediaSession: MediaSessionCompat by lazy { createMediaSession() }
    private val mediaSessionConnector: MediaSessionConnector by lazy {
        createMediaSessionConnector()
    }
    private val playerState by lazy { PlayerState() }
    private lateinit var playerHolder: PlayerHolder
    private var previewFuture: Future<Unit>? = null
    private var requestedFrame = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setContentView(R.layout.player_view)
        window.decorView.setBackgroundColor(Color.BLACK)
        volumeControlStream = AudioManager.STREAM_MUSIC
        if (savedInstanceState != null) {
            playerState.position = savedInstanceState.getLong("position", 0)
            playerState.window = savedInstanceState.getInt("window", 0)
        }
        hideUI()
        player.setResizeMode(getResizeMode())
        createMediaSession()
        createPlayer()
        skip.setOnClickListener { playerHolder.skip() }
        exo_progress.attachPreviewFrameLayout(previewFrameLayout)
        exo_progress.setPreviewLoader(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode)
            player.useController = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("window", playerState.window)
        outState.putLong("position", playerState.position)
    }

    private fun hideUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        window.addFlags(View.KEEP_SCREEN_ON)
    }

    private fun getResizeMode(): Int {
        return when (PreferenceManager.getDefaultSharedPreferences(this).getString("player_resize", "0")) {
            "0" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            "1" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            "2" -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            "3" -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i("Player", "OnStart")
        startPlayer()
        activateMediaSession()
    }

    override fun onResume() {
        super.onResume()
        doOnUI { hideUI() }
    }

    override fun onStop() {
        Log.i("Player", "OnStop")
        playerHolder.saveState()
        stopPlayer()
        deactivateMediaSession()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("Player", "OnDestroy")
        releasePlayer()
        releaseMediaSession()
    }

    // MediaSession related functions.
    private fun createMediaSession(): MediaSessionCompat = MediaSessionCompat(this, packageName)

    private fun createMediaSessionConnector(): MediaSessionConnector =
            MediaSessionConnector(mediaSession).apply {
                // If QueueNavigator isn't set, then mediaSessionConnector will not handle following
                // MediaSession actions (and they won't show up in the minimized PIP activity):
                // [ACTION_SKIP_PREVIOUS], [ACTION_SKIP_NEXT], [ACTION_SKIP_TO_QUEUE_ITEM]
                setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
                    override fun getMediaDescription(windowIndex: Int): MediaDescriptionCompat {
                        return MediaCatalog(mutableListOf(), intent)[windowIndex]
                    }
                })
            }


    // MediaSession related functions.
    private fun activateMediaSession() {
        // Note: do not pass a null to the 3rd param below, it will cause a NullPointerException.
        // To pass Kotlin arguments to Java varargs, use the Kotlin spread operator `*`.
        mediaSessionConnector.setPlayer(playerHolder.audioFocusPlayer, null)
        mediaSession.isActive = true
    }

    private fun deactivateMediaSession() {
        mediaSessionConnector.setPlayer(null, null)
        mediaSession.isActive = false
    }

    private fun releaseMediaSession() {
        mediaSession.release()
    }

    // ExoPlayer related functions.
    private fun createPlayer() {
        playerHolder = PlayerHolder(this, playerState, player, intent)
        if (!intent.getBooleanExtra("isPlayList", false)) {
            exo_next.visibility = View.GONE
            exo_prev.visibility = View.GONE
        }
        //exoPlayer.overlayFrameLayout.setOnTouchListener(BVListener(this))
    }

    private fun startPlayer() {
        playerHolder.start()
    }

    private fun stopPlayer() {
        playerHolder.stop()
    }

    private fun releasePlayer() {
        playerHolder.release()
    }

    // Picture in Picture related functions.
    override fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && playerHolder.audioFocusPlayer.playWhenReady && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            noCrash {
                enterPictureInPictureMode(
                        with(PictureInPictureParams.Builder()) {
                            //setAspectRatio(Rational(16, 9))
                            build()
                        })
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean,
                                               newConfig: Configuration?) {
        playerHolder.saveState()
        player.useController = !isInPictureInPictureMode
    }

    override fun loadPreview(currentPosition: Long, max: Long) {
        previewFuture?.cancel(true)
        previewFuture = doAsync {
            requestedFrame = currentPosition
            val bitmap = playerHolder.retriever.getFrameAtTime(currentPosition * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (requestedFrame == currentPosition || previewFuture?.isCancelled == false)
                doOnUI(false) { preview.setImageBitmap(bitmap) }
        }
    }

    override fun onChangeTitle(title: String) {
        video_title.text = title
    }

    override fun onLoadingChange(loading: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode)
            progress.post { progress.visibility = View.GONE }
        progress.post { progress.visibility = if (loading) View.VISIBLE else View.GONE }
    }

    override fun onPlayerStateChanged(state: Int, playWhenReady: Boolean) {
        if (state == Player.STATE_READY)
            doOnUI { hideUI() }
        if (state == Player.STATE_READY && playWhenReady)
            exo_progress.hidePreview()
    }

    override fun onFinish() {
        finish()
    }


}