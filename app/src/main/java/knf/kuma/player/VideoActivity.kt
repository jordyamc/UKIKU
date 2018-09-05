package knf.kuma.player

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.util.Rational
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import knf.kuma.R
import knf.kuma.commons.EAHelper
import kotlinx.android.synthetic.main.exo_playback_control_view.*
import kotlinx.android.synthetic.main.player_view.*

/**
 * Allows playback of videos that are in a playlist, using [PlayerHolder] to load the and render
 * it to the [com.google.android.exoplayer2.ui.PlayerView] to render the video output. Supports
 * [MediaSessionCompat] and picture in picture as well.
 */

class VideoActivity : AppCompatActivity(), PlayerHolder.PlayerCallback {
    private val mediaSession: MediaSessionCompat by lazy { createMediaSession() }
    private val mediaSessionConnector: MediaSessionConnector by lazy {
        createMediaSessionConnector()
    }
    private val playerState by lazy { PlayerState() }
    private lateinit var playerHolder: PlayerHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.player_view)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode)
            player.useController = false
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState!!.putInt("window", playerState.window)
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

    /*override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                and View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                and View.SYSTEM_UI_FLAG_FULLSCREEN
                and View.SYSTEM_UI_FLAG_IMMERSIVE)
        Log.i("Player", "OnResume")
        startPlayer()
        activateMediaSession()
    }*/

    /*override fun onPause() {
        super.onPause()
        Log.i("Player", "OnPause")
        stopPlayer()
        deactivateMediaSession()
    }*/

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
        //player.overlayFrameLayout.setOnTouchListener(BVListener(this))
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && playerHolder.audioFocusPlayer.playWhenReady) {
            enterPictureInPictureMode(
                    with(PictureInPictureParams.Builder()) {
                        setAspectRatio(Rational(16, 9))
                        build()
                    })
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean,
                                               newConfig: Configuration?) {
        playerHolder.saveState()
        player.useController = !isInPictureInPictureMode
    }

    override fun onChangeTitle(title: String) {
        findViewById<TextView>(R.id.title).text = title
    }

    override fun onLoadingChange(loading: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode)
            progress.post { progress.visibility = View.GONE }
        progress.post { progress.visibility = if (loading) View.VISIBLE else View.GONE }
    }

    override fun onFinish() {
        finish()
    }
}