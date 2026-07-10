package knf.kuma.tv.exoplayer

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
import knf.kuma.database.CacheDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class PlaybackFragment : VideoSupportFragment() {
    private var mPlayerGlue: VideoPlayerGlue? = null
    private var mPlayerAdapter: LeanbackPlayerAdapter? = null
    private var mPlayer: ExoPlayer? = null
    private var mTrackSelector: TrackSelector? = null
    private var mVideo: Video? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.intent?.let {
            mVideo = Video(it)
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        if (mPlayer == null) {
            initializePlayer()
        }
    }

    /**
     * Pauses the exoPlayer.
     */
    override fun onPause() {
        super.onPause()
        mPlayerGlue?.save(mVideo)
        if (mPlayerGlue?.isPlaying == true)
            mPlayerGlue?.pause()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initializePlayer() {
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory()
        mTrackSelector = DefaultTrackSelector(requireContext(), videoTrackSelectionFactory)
        mPlayer =
            ExoPlayer.Builder(
                requireContext(),
                DefaultRenderersFactory(requireContext())
            ).build()
        mPlayerAdapter =
            LeanbackPlayerAdapter(activity as Context, mPlayer as ExoPlayer, UPDATE_DELAY)
        mPlayerGlue = VideoPlayerGlue(activity as Context, mPlayerAdapter as LeanbackPlayerAdapter)
        mPlayerGlue?.host = VideoSupportFragmentGlueHost(this)
        mPlayerGlue?.playWhenPrepared()

        play(mVideo)
    }

    private fun releasePlayer() {
        mPlayer?.pause()
        mPlayer?.release()
        mPlayer = null
        mTrackSelector = null
        mPlayerGlue = null
        mPlayerAdapter = null
    }

    private fun play(video: Video?) {
        lifecycleScope.launch {
            mPlayerGlue?.title = video?.title
            mPlayerGlue?.subtitle = video?.chapter
            prepareMediaForPlaying(video?.uri ?: Uri.EMPTY, video?.headers)
            val state = withContext(Dispatchers.IO) {
                CacheDB.INSTANCE.playerStateDAO().find("${video?.title}: ${video?.chapter}")
            }
            if (state != null) {
                mPlayerGlue?.seekTo(state.position)
            }
            mPlayerGlue?.play()
        }
    }

    private fun prepareMediaForPlaying(mediaSourceUri: Uri, headers: Map<String, String>?) {
        activity?.let {
            val httpFactory = DefaultHttpDataSource.Factory().apply {
                if (headers?.containsKey("User-Agent") != true) {
                    setUserAgent(Util.getUserAgent(it, "UKIKU"))
                } else {
                    setUserAgent(headers["User-Agent"])
                }
                setDefaultRequestProperties(headers ?: emptyMap())
            }
            val url = mediaSourceUri.toString()
            val mediaSource = if (url.contains("m3u8") || url.contains("cf-master")) {
                HlsMediaSource.Factory(httpFactory)
            } else {
                ProgressiveMediaSource.Factory(httpFactory)
            }.createMediaSource(MediaItem.fromUri(mediaSourceUri))
            mPlayer?.setMediaSource(mediaSource)
            mPlayer?.prepare()
        }
    }

    fun skipToNext() {
        mPlayerGlue?.next()
    }

    fun skipToPrevious() {
        mPlayerGlue?.previous()
    }

    fun rewind() {
        mPlayerGlue?.rewind()
    }

    fun fastForward() {
        mPlayerGlue?.fastForward()
    }

    companion object {

        private const val UPDATE_DELAY = 16

        operator fun get(bundle: Bundle?): PlaybackFragment {
            val fragment = PlaybackFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}
