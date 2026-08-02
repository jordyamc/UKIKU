package knf.kuma.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.preference.PreferenceManager
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.crashlytics
import knf.kuma.R
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.EAHelper
import knf.kuma.commons.SSLSkipper
import knf.kuma.commons.doOnUI
import knf.kuma.commons.noCrash
import knf.kuma.database.CacheDB
import knf.kuma.databinding.ExoPlayerBinding
import knf.kuma.pojos.QueueObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import xdroid.toaster.Toaster

@UnstableApi
class BasicExoplayer : AppCompatActivity(), Player.Listener {
    private var exoPlayer: ExoPlayer? = null
    private var playerState: PlayerState = PlayerState()
    private var isEnding = false
    private var playList: List<QueueObject> = ArrayList()
    private val binding by lazy { ExoPlayerBinding.inflate(layoutInflater) }

    private val resizeMode: Int
        @OptIn(UnstableApi::class)
        get() {
            return when (PreferenceManager.getDefaultSharedPreferences(this).getString("player_resize", "0")) {
                "0" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                "1" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                "2" -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                "3" -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setContentView(binding.root)
        window.decorView.setBackgroundColor(Color.BLACK)
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE))
            find<View>(R.id.pip).visibility = View.VISIBLE
        find<View>(R.id.pip).setOnClickListener { onPip() }
        find<View>(R.id.skip).setOnClickListener { onSkip() }
        hideUI()
        binding.player.resizeMode = resizeMode
        binding.player.requestFocus()
        lifecycleScope.launch(Dispatchers.Main) {
            playerState = withContext(Dispatchers.IO) {
                CacheDB.INSTANCE.playerStateDAO().find(intent.getStringExtra("title") ?: "???")
                    ?: PlayerState()
            }
            if (savedInstanceState != null) {
                playerState.position = savedInstanceState.getLong("position", C.TIME_UNSET)
                playerState.window = savedInstanceState.getInt("listPosition", 0)
            }
            checkPlaylist(intent)
            initPlayer(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("position", playerState.position)
        if (playerState.window != 0)
            outState.putInt("listPosition", playerState.window)
    }

    private fun hideUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, binding.root)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun initPlayer(intent: Intent) {
        if (exoPlayer == null) {
            SSLSkipper.skip()
            lifecycleScope.launch(Dispatchers.Main) {
                find<TextView>(R.id.video_title).text = intent.getStringExtra("title")
                exoPlayer = ExoPlayer.Builder(this@BasicExoplayer).build()
                binding.player.player = exoPlayer
                exoPlayer?.addListener(this@BasicExoplayer)
                addMedia(exoPlayer, intent)
                exoPlayer?.prepare()
                val canResume = playerState.window >= 0 && playerState.position >= 0
                if (canResume)
                    exoPlayer?.seekTo(playerState.window, playerState.position)
                exoPlayer?.playWhenReady = true
            }
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun addMedia(player: ExoPlayer?, intent: Intent) {
        player?: return
        if (intent.getBooleanExtra("isPlayList", false)) {
            val sourceList = ArrayList<MediaSource>()
            playList = withContext(Dispatchers.IO) {
                CacheDB.INSTANCE.queueDAO()
                    .getAllByAid(intent.getStringExtra("playlist") ?: "empty")
            }
            noCrash { find<TextView>(R.id.video_title).text = playList[0].title() }
            DefaultHttpDataSource.Factory()
            for (queueObject in playList) {
                sourceList.add(
                    ProgressiveMediaSource.Factory(
                        DefaultHttpDataSource.Factory().apply {
                            setUserAgent(BypassUtil.userAgent)
                        }
                    ).createMediaSource(MediaItem.fromUri(queueObject.createUri()))
                )
            }
            player.addMediaSources(sourceList)
        } else {
            if (intent.getBooleanExtra("isFile", false)) {
                player.addMediaItem(MediaItem.fromUri(intent.data ?: "".toUri()))
            } else {
                val httpFactory = DefaultHttpDataSource.Factory().apply {
                    setAllowCrossProtocolRedirects(true)
                    intent.getStringArrayExtra("headers") ?.let { headerArray ->
                        val slices = headerArray.toList().chunked(2)
                        val headers = mutableMapOf<String, String>()
                        slices.forEach {
                            headers[it[0]] = it[1]
                        }
                        setDefaultRequestProperties(headers)
                        if (headers.contains("User-Agent")) {
                            setUserAgent(headers["User-Agent"])
                        } else {
                            setUserAgent(BypassUtil.userAgent)
                        }
                    }?: setUserAgent(BypassUtil.userAgent)
                }
                val url = intent.data?.toString()
                val factory = if (url?.contains("m3u8") == true || url?.contains("master.") == true) {
                    HlsMediaSource.Factory(httpFactory)
                } else {
                    ProgressiveMediaSource.Factory(httpFactory)
                }
                player.addMediaSource(
                    factory.createMediaSource(
                        MediaItem.fromUri(
                            intent.data ?: "".toUri()
                        )
                    )
                )
            }
        }
    }

    private fun releasePlayer() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }

    internal fun onPip() {
        try {
            if (!isInPictureInPictureMode) {
                playerState.position = exoPlayer?.currentPosition ?: 0
                val params = PictureInPictureParams.Builder()
                    //.setAspectRatio(Rational(player.width, player.height))
                    .build()
                enterPictureInPictureMode(params)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun onSkip() {
        exoPlayer?.seekTo(
            exoPlayer?.currentMediaItemIndex ?: 0, (exoPlayer?.currentPosition
                ?: 0) + 85000
        )
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) {
            runOnUiThread {
                find<View>(R.id.lay_top).visibility = View.VISIBLE
                find<View>(R.id.lay_bottom).visibility = View.VISIBLE
                binding.player.useController = true
            }
            /*getApplication().startActivity(new Intent(this, getClass())
                    .putExtra("isReorder", true)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));*/
        } else {
            runOnUiThread {
                find<View>(R.id.lay_top).visibility = View.GONE
                find<View>(R.id.lay_bottom).visibility = View.GONE
                find<View>(R.id.progress).visibility = View.GONE
                binding.player.useController = false
            }
        }
    }

    private fun checkPlaylist(intent: Intent) {
        if (!intent.getBooleanExtra("isPlayList", false)) {
            find<View>(androidx.media3.ui.R.id.exo_next).visibility = View.GONE
            find<View>(androidx.media3.ui.R.id.exo_prev).visibility = View.GONE
        } else {
            find<View>(androidx.media3.ui.R.id.exo_next).visibility = View.VISIBLE
            find<View>(androidx.media3.ui.R.id.exo_prev).visibility = View.VISIBLE
        }
    }

    override fun onUserLeaveHint() {
        if (exoPlayer?.playWhenReady == true && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            try {
                val builder = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(builder)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        super.onUserLeaveHint()
        exoPlayer?.playWhenReady = false
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        releasePlayer()
        playerState.window = C.INDEX_UNSET
        playerState.position = 0
        checkPlaylist(intent)
        initPlayer(intent)
        super.onNewIntent(intent)
    }

    override fun onResume() {
        doOnUI { hideUI() }
        exoPlayer?.playWhenReady = true
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (isInPictureInPictureMode)
            return
        val state = playerState.apply {
            title = find<TextView>(R.id.video_title).text.toString()
            position = if (!isEnding) {
                exoPlayer?.currentPosition ?: 0
            } else
                0
        }
        doAsync {
            CacheDB.INSTANCE.playerStateDAO().set(state)
        }
        if (!isFinishing)
            exoPlayer?.pause()
        else
            exoPlayer?.stop()
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {

    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        if (isInPictureInPictureMode)
            return
        find<View>(R.id.progress).post { find<View>(R.id.progress).visibility = if (isLoading) View.VISIBLE else View.GONE }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            doOnUI { hideUI() }
        }
        if (playbackState == Player.STATE_ENDED) {
            isEnding = true
            finish()
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {

    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

    }

    override fun onPlayerError(error: PlaybackException) {
        Toaster.toast(
            "Error al reproducir: " + error.message?.replace("%", "%%"),
            emptyArray<Any>()
        )
        /*MaterialDialog(this).show {
            message(text = error.stackTraceToString().also {
                (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(ClipData.newPlainText("stack", it))
            })
            positiveButton(text = "OK")
        }*/
        Firebase.crashlytics.recordException(error, CustomKeysAndValues.Builder().putString("link", intent.dataString?: "Empty").putString("extras", intent.extras.toString()).build())
        finish()
    }

    /*override fun onPositionDiscontinuity(reason: Int) {
        try {
            val latestPosition = exoPlayer?.currentWindowIndex ?: 0
            if (latestPosition != listPosition) {
                val state = PlayerState().apply {
                    title = playList[listPosition].title()
                    if (reason == 0) {
                        position = 0
                    } else if (reason in 1..2) {
                        position = lastPosition
                    }
                }
                doAsync {
                    CacheDB.INSTANCE.playerStateDAO().set(state)
                }
                listPosition = latestPosition
                video_title.text = playList[listPosition].title()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }*/

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        try {
            val latestPosition = newPosition.mediaItemIndex
            if (latestPosition != playerState.window) {
                val state = playerState.apply {
                    title = playList[playerState.window].title()
                    if (reason == 0) {
                        position = 0
                    } else if (reason in 1..2) {
                        position = oldPosition.positionMs
                    }
                }
                doAsync {
                    CacheDB.INSTANCE.playerStateDAO().set(state)
                }
                playerState.window = latestPosition
                find<TextView>(R.id.video_title).text = playList[playerState.window].title()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {

    }
}
