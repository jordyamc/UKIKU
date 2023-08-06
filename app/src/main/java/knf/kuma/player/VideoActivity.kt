package knf.kuma.player

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.SSLSkipper
import knf.kuma.commons.doOnUI
import knf.kuma.commons.noCrash
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.databinding.PlayerViewBinding
import knf.kuma.pojos.QueueObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick

/**
 * Allows playback of videos that are in a playlist, using [PlayerHolder] to load the and render
 * it to the [com.google.android.exoplayer2.ui.PlayerView] to render the video output. Supports
 * [MediaSessionCompat] and picture in picture as well.
 */

class VideoActivity : GenericActivity(), PlayerHolder.PlayerCallback {
    private val mediaSession: MediaSessionCompat by lazy { createMediaSession() }
    private val mediaSessionConnector: MediaSessionConnector by lazy {
        createMediaSessionConnector()
    }
    private val binding by lazy { PlayerViewBinding.inflate(layoutInflater) }
    private lateinit var playerState: PlayerState
    private lateinit var playerHolder: PlayerHolder
    var playList: List<QueueObject> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setContentView(binding.root)
        window.decorView.setBackgroundColor(Color.BLACK)
        volumeControlStream = AudioManager.STREAM_MUSIC
        hideUI()
        SSLSkipper.skip()
        binding.player.resizeMode = getResizeMode()
        find<View>(R.id.exit).onClick { onBackPressed() }
        find<View>(R.id.lock).onClick { lock() }
        find<View>(R.id.skip).setOnClickListener { playerHolder.skip() }
        binding.unlock.onClick { unlock() }
        binding.youtubeOverlay.performListener(object : YouTubeOverlay.PerformListener {
            override fun onAnimationEnd() {
                binding.youtubeOverlay.startAnimation(AnimationUtils.loadAnimation(this@VideoActivity, R.anim.fadeout))
                binding.youtubeOverlay.isVisible = false
                find<View>(R.id.exo_ll_controls).isVisible = true
            }

            override fun onAnimationStart() {
                binding.youtubeOverlay.isVisible = true
                binding.youtubeOverlay.startAnimation(AnimationUtils.loadAnimation(this@VideoActivity, R.anim.fadein))
                find<View>(R.id.exo_ll_controls).isVisible = false
            }
        })
        if (!intent.getBooleanExtra("isPlayList", false)) {
            find<View>(R.id.lay_next).isVisible = false
            find<View>(R.id.lay_prev).isVisible = false
        }
        lifecycleScope.launch(Dispatchers.IO) {
            playList = CacheDB.INSTANCE.queueDAO().getAllByAid(intent.getStringExtra("playlist") ?: "empty")
            playerState = CacheDB.INSTANCE.playerStateDAO().find(intent.getStringExtra("title") ?: "???") ?: PlayerState()
            if (savedInstanceState != null) {
                playerState.position = savedInstanceState.getLong("position", 0)
                playerState.window = savedInstanceState.getInt("window", 0)
                playerState.whenReady = savedInstanceState.getBoolean("playWhenReady", true)
            }
            //createMediaSession()
            withContext(Dispatchers.Main) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode)
                    binding.player.useController = false
                createPlayer()
                playerHolder.start()
                activateMediaSession()
            }
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
        binding.player.hideController()
        binding.layLocked.isVisible = true
    }

    private fun unlock() {
        binding.player.showController()
        binding.layLocked.isVisible = false
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
        if (::playerHolder.isInitialized) {
            startPlayer()
            activateMediaSession()
        }
    }

    override fun onResume() {
        super.onResume()
        doOnUI { hideUI() }
        resumePlayer()
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= 24 && isInPictureInPictureMode)
            resumePlayer()
        else {
            if (::playerHolder.isInitialized)
                if (!playerState.isFinishing) {
                    playerHolder.saveState()
                    playerState.title = find<TextView>(R.id.video_title).text.toString()
                    saveState()
                }
            if (!isFinishing)
                pausePlayer()
            else
                stopPlayer()
        }
    }

    override fun onStop() {
        deactivateMediaSession()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
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
                /*setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
                    override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {

                    }
                })*/
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
        playerHolder = PlayerHolder(this, playerState, binding.player, intent, playList)
        binding.youtubeOverlay.player(playerHolder.audioFocusPlayer)
        if (!intent.getBooleanExtra("isPlayList", false)) {
            find<View>(com.google.android.exoplayer2.R.id.exo_next).visibility = View.GONE
            find<View>(com.google.android.exoplayer2.R.id.exo_prev).visibility = View.GONE
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
        binding.player.useController = !isInPictureInPictureMode
    }

    override fun onChangeTitle(title: String) {
        lifecycleScope.launch(Dispatchers.Main) { find<TextView>(R.id.video_title).text = title }
    }

    override fun onLoadingChange(loading: Boolean) {
        with(find<View>(R.id.progress)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode)
                post { visibility = View.GONE }
            post { visibility = if (loading) View.VISIBLE else View.GONE }
        }
    }

    override fun onPlayerStateChanged(state: Int, playWhenReady: Boolean) {
        if (state == Player.STATE_READY)
            doOnUI { hideUI() }
    }


    override fun onFinish() {
        playerState.apply {
            title = find<TextView>(R.id.video_title).text.toString()
            position = 0
            isFinishing = true
        }
        saveState()
        unlock()
        //finish()
    }


}