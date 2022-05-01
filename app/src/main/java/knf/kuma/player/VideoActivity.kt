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
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.rubensousa.previewseekbar.PreviewLoader
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.doOnUI
import knf.kuma.commons.noCrash
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.pojos.QueueObject
import kotlinx.android.synthetic.main.exo_playback_youtube_control_view.*
import kotlinx.android.synthetic.main.player_view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.concurrent.Future

/**
 * Allows playback of videos that are in a playlist, using [PlayerHolder] to load the and render
 * it to the [com.google.android.exoplayer2.ui.PlayerView] to render the video output. Supports
 * [MediaSessionCompat] and picture in picture as well.
 */

class VideoActivity : GenericActivity(), PlayerHolder.PlayerCallback, PreviewLoader {
    private val mediaSession: MediaSessionCompat by lazy { createMediaSession() }
    private val mediaSessionConnector: MediaSessionConnector by lazy {
        createMediaSessionConnector()
    }
    private lateinit var playerState: PlayerState
    private lateinit var playerHolder: PlayerHolder
    private var previewFuture: Future<Unit>? = null
    private var requestedFrame = 0L
    var playList: List<QueueObject> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        Log.e("Player", "OnCreate")
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setContentView(R.layout.player_view)
        window.decorView.setBackgroundColor(Color.BLACK)
        volumeControlStream = AudioManager.STREAM_MUSIC
        hideUI()
        player.resizeMode = getResizeMode()
        exit.onClick { onBackPressed() }
        lock.onClick { lock() }
        unlock.onClick { unlock() }
        youtubeOverlay.performListener(object : YouTubeOverlay.PerformListener {
            override fun onAnimationEnd() {
                youtubeOverlay.startAnimation(AnimationUtils.loadAnimation(this@VideoActivity, R.anim.fadeout))
                youtubeOverlay.isVisible = false
                exo_ll_controls.isVisible = true
            }

            override fun onAnimationStart() {
                youtubeOverlay.isVisible = true
                youtubeOverlay.startAnimation(AnimationUtils.loadAnimation(this@VideoActivity, R.anim.fadein))
                exo_ll_controls.isVisible = false
            }
        })
        if (!intent.getBooleanExtra("isPlayList", false)) {
            lay_next.isVisible = false
            lay_prev.isVisible = false
        }
        lifecycleScope.launch(Dispatchers.Main) {
            playList = withContext(Dispatchers.IO) {
                CacheDB.INSTANCE.queueDAO().getAllByAid(intent.getStringExtra("playlist")
                        ?: "empty")
            }
            playerState = withContext(Dispatchers.IO) {
                CacheDB.INSTANCE.playerStateDAO().find(intent.getStringExtra("title") ?: "???")
                        ?: PlayerState()
            }
            if (savedInstanceState != null) {
                playerState.position = savedInstanceState.getLong("position", 0)
                playerState.window = savedInstanceState.getInt("window", 0)
                playerState.whenReady = savedInstanceState.getBoolean("playWhenReady", true)
            }
            createMediaSession()
            createPlayer()
            skip.setOnClickListener { playerHolder.skip() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode)
                player.useController = false
            playerHolder.start()
            activateMediaSession()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::playerHolder.isInitialized) {
            playerHolder.saveState()
            outState.putInt("window", playerState.window)
            outState.putLong("position", playerState.position)
            outState.putBoolean("playWhenReady", playerState.whenReady)
        }
    }

    private fun lock() {
        player.hideController()
        lay_locked.isVisible = true
    }

    private fun unlock() {
        player.showController()
        lay_locked.isVisible = false
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
        Log.e("Player", "OnStart")
        if (::playerHolder.isInitialized) {
            startPlayer()
            activateMediaSession()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e("Player", "OnResume")
        doOnUI { hideUI() }
        resumePlayer()
    }

    override fun onPause() {
        super.onPause()
        Log.e("Player", "OnPause, isfinishing: $isFinishing")
        if (Build.VERSION.SDK_INT >= 24 && isInPictureInPictureMode)
            resumePlayer()
        else {
            if (::playerHolder.isInitialized)
                if (!playerState.isFinishing) {
                    playerHolder.saveState()
                    playerState.title = video_title.text.toString()
                    saveState()
                }
            if (!isFinishing)
                pausePlayer()
            else
                stopPlayer()
        }
    }

    override fun onStop() {
        Log.e("Player", "OnStop")
        deactivateMediaSession()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("Player", "OnDestroy")
        stopPlayer()
        releasePlayer()
        releaseMediaSession()
    }

    private fun saveState() {
        GlobalScope.launch(Dispatchers.IO) {
            CacheDB.INSTANCE.playerStateDAO().set(playerState)
        }
    }

    // MediaSession related functions.
    private fun createMediaSession(): MediaSessionCompat = MediaSessionCompat(this, packageName).apply {
        setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                with(playerHolder.audioFocusPlayer) {
                    playWhenReady = true
                }
            }

            override fun onPause() {
                super.onPause()
                with(playerHolder.audioFocusPlayer) {
                    playWhenReady = false
                }
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                with(playerHolder.audioFocusPlayer) {
                    if (hasNext())
                        next()
                }
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                with(playerHolder.audioFocusPlayer) {
                    if (hasPrevious())
                        previous()
                }
            }
        })
    }

    private fun createMediaSessionConnector(): MediaSessionConnector =
            MediaSessionConnector(mediaSession).apply {
                // If QueueNavigator isn't set, then mediaSessionConnector will not handle following
                // MediaSession actions (and they won't show up in the minimized PIP activity):
                // [ACTION_SKIP_PREVIOUS], [ACTION_SKIP_NEXT], [ACTION_SKIP_TO_QUEUE_ITEM]
                setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
                    override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
                        return MediaCatalog(mutableListOf(), intent, playList)[windowIndex]
                    }
                })
            }


    // MediaSession related functions.
    private fun activateMediaSession() {
        // Note: do not pass a null to the 3rd param below, it will cause a NullPointerException.
        // To pass Kotlin arguments to Java varargs, use the Kotlin spread operator `*`.
        mediaSessionConnector.setPlayer(playerHolder.audioFocusPlayer)
        mediaSession.isActive = true
    }

    private fun deactivateMediaSession() {
        mediaSessionConnector.setPlayer(null)
        mediaSession.isActive = false
    }

    private fun releaseMediaSession() {
        mediaSession.release()
    }

    // ExoPlayer related functions.
    private fun createPlayer() {
        playerHolder = PlayerHolder(this, playerState, player, intent, playList)
        youtubeOverlay.player(playerHolder.audioFocusPlayer)
        if (!intent.getBooleanExtra("isPlayList", false)) {
            exo_next.visibility = View.GONE
            exo_prev.visibility = View.GONE
        }
        //exoPlayer.overlayFrameLayout.setOnTouchListener(BVListener(this))
    }

    private fun startPlayer() {
        if (::playerHolder.isInitialized)
            playerHolder.start()
    }

    private fun stopPlayer() {
        if (::playerHolder.isInitialized)
            playerHolder.stop()
    }

    private fun resumePlayer() {
        if (::playerHolder.isInitialized)
            with(playerHolder.audioFocusPlayer) {
                playWhenReady = true
            }
    }

    private fun pausePlayer() {
        if (::playerHolder.isInitialized)
            with(playerHolder.audioFocusPlayer) {
                playWhenReady = false
            }
    }

    private fun releasePlayer() {
        if (::playerHolder.isInitialized)
            playerHolder.release()
    }

    // Picture in Picture related functions.
    override fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && ::playerHolder.isInitialized && playerHolder.audioFocusPlayer.playWhenReady && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            noCrash {
                enterPictureInPictureMode(
                    with(PictureInPictureParams.Builder()) {
                        //setAspectRatio(Rational(16, 9))
                        build()
                    })
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (::playerHolder.isInitialized)
            playerHolder.saveState()
        player.useController = !isInPictureInPictureMode
    }

    override fun loadPreview(currentPosition: Long, max: Long) {
        previewFuture?.cancel(true)
        previewFuture = doAsync {
            requestedFrame = currentPosition
            val bitmap = playerHolder.retriever.getFrameAtTime(
                currentPosition * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            if (requestedFrame == currentPosition || previewFuture?.isCancelled == false)
                doOnUI(false) { preview.setImageBitmap(bitmap) }
        }
    }

    override fun onChangeTitle(title: String) {
        lifecycleScope.launch(Dispatchers.Main) { video_title.text = title }
    }

    override fun onLoadingChange(loading: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode)
            progress.post { progress.visibility = View.GONE }
        progress.post { progress.visibility = if (loading) View.VISIBLE else View.GONE }
    }

    override fun onPlayerStateChanged(state: Int, playWhenReady: Boolean) {
        if (state == Player.STATE_READY)
            doOnUI { hideUI() }
    }


    override fun onFinish() {
        playerState.apply {
            title = video_title.text.toString()
            position = 0
            isFinishing = true
        }
        saveState()
        unlock()
        //finish()
    }


}