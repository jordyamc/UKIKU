package knf.kuma.player

import android.annotation.TargetApi
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.firebase.crashlytics.FirebaseCrashlytics
import knf.kuma.R
import knf.kuma.commons.BypassUtil
import knf.kuma.commons.EAHelper
import knf.kuma.commons.SSLSkipper
import knf.kuma.commons.doOnUI
import knf.kuma.commons.noCrash
import knf.kuma.custom.GenericActivity
import knf.kuma.database.CacheDB
import knf.kuma.databinding.ExoPlayerBinding
import knf.kuma.pojos.QueueObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import xdroid.toaster.Toaster

class CustomExoPlayer : GenericActivity(), Player.Listener {
    private var exoPlayer: ExoPlayer? = null
    private var playerState: PlayerState = PlayerState()
    private var isEnding = false
    private var playList: List<QueueObject> = ArrayList()
    private val binding by lazy { ExoPlayerBinding.inflate(layoutInflater) }

    private val resizeMode: Int
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE))
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
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun initPlayer(intent: Intent) {
        if (exoPlayer == null) {
            SSLSkipper.skip()
            lifecycleScope.launch(Dispatchers.Main) {
                find<TextView>(R.id.video_title).text = intent.getStringExtra("title")
                exoPlayer = ExoPlayer.Builder(this@CustomExoPlayer).build()
                binding.player.player = exoPlayer
                exoPlayer?.addListener(this@CustomExoPlayer)
                addMedia(exoPlayer, intent)
                exoPlayer?.prepare()
                val canResume = playerState.window >= 0 && playerState.position != C.TIME_UNSET
                if (canResume)
                    exoPlayer?.seekTo(playerState.window, playerState.position)
                exoPlayer?.playWhenReady = true
                /*disposable = Observable.interval(1, TimeUnit.SECONDS).map { exoPlayer?.currentPosition?:0 }
                        .subscribeOn(AndroidSchedulers.from(exoPlayer?.applicationLooper, false))
                        .subscribe {
                            if (it > 0)
                                lastPosition = it
                        }*/
            }
        }
    }

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
                if (intent.getBooleanExtra("isFile", false) || !intent.hasExtra("headers")) {
                    player.addMediaItem(MediaItem.fromUri(intent.data ?: Uri.parse("")))
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
                    val factory = when(MimeTypeMap.getFileExtensionFromUrl(intent.data?.toString())) {
                        "m3u8" -> HlsMediaSource.Factory(httpFactory)
                        else -> ProgressiveMediaSource.Factory(httpFactory)
                    }
                    player.addMediaSource(
                        factory.createMediaSource(
                            MediaItem.fromUri(
                                intent.data ?: Uri.parse("")
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

    @TargetApi(Build.VERSION_CODES.N)
    internal fun onPip() {
        try {
            if (!isInPictureInPictureMode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    playerState.position = exoPlayer?.currentPosition ?: 0
                    val params = PictureInPictureParams.Builder()
                            //.setAspectRatio(Rational(player.width, player.height))
                            .build()
                    enterPictureInPictureMode(params)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun onSkip() {
        exoPlayer?.seekTo(
            exoPlayer?.currentWindowIndex ?: 0, (exoPlayer?.currentPosition
                ?: 0) + 85000
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
            find<View>(com.google.android.exoplayer2.R.id.exo_next).visibility = View.GONE
            find<View>(com.google.android.exoplayer2.R.id.exo_prev).visibility = View.GONE
        } else {
            find<View>(com.google.android.exoplayer2.R.id.exo_next).visibility = View.VISIBLE
            find<View>(com.google.android.exoplayer2.R.id.exo_prev).visibility = View.VISIBLE
        }
    }

    override fun onUserLeaveHint() {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode)
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

    override fun onLoadingChanged(isLoading: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode)
            return
        find<View>(R.id.progress).post { find<View>(R.id.progress).visibility = if (isLoading) View.VISIBLE else View.GONE }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playbackState == Player.STATE_READY)
            doOnUI { hideUI() }
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
        FirebaseCrashlytics.getInstance().recordException(error)
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
            val latestPosition = newPosition.windowIndex
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
