package knf.kuma.tv.exoplayer

import android.annotation.TargetApi
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class PlaybackFragment : VideoSupportFragment() {
    private var mPlayerGlue: VideoPlayerGlue? = null
    private var mPlayerAdapter: LeanbackPlayerAdapter? = null
    private var mPlayer: SimpleExoPlayer? = null
    private var mTrackSelector: TrackSelector? = null
    private var mVideo: Video? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mVideo = Video(activity?.intent?.extras)
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || mPlayer ==
                null) {
            initializePlayer()
        }
    }

    /**
     * Pauses the exoPlayer.
     */
    @TargetApi(Build.VERSION_CODES.N)
    override fun onPause() {
        super.onPause()

        if (mPlayerGlue?.isPlaying == true)
            mPlayerGlue?.pause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun initializePlayer() {
        val bandwidthMeter = DefaultBandwidthMeter()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        mTrackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        mPlayer = ExoPlayerFactory.newSimpleInstance(activity, mTrackSelector)
        mPlayerAdapter = LeanbackPlayerAdapter(activity as Context, mPlayer as SimpleExoPlayer, UPDATE_DELAY)
        mPlayerGlue = VideoPlayerGlue(activity as Context, mPlayerAdapter as LeanbackPlayerAdapter)
        mPlayerGlue?.host = VideoSupportFragmentGlueHost(this)
        mPlayerGlue?.playWhenPrepared()

        play(mVideo)
    }

    private fun releasePlayer() {
        mPlayer?.release()
        mPlayer = null
        mTrackSelector = null
        mPlayerGlue = null
        mPlayerAdapter = null
    }

    private fun play(video: Video?) {
        mPlayerGlue?.title = video?.title
        mPlayerGlue?.subtitle = video?.chapter
        prepareMediaForPlaying(video?.uri ?: Uri.EMPTY)
        mPlayerGlue?.play()
    }

    private fun prepareMediaForPlaying(mediaSourceUri: Uri) {
        activity?.let {
            val userAgent = Util.getUserAgent(it, "VideoPlayerGlue")
            val mediaSource = ExtractorMediaSource(
                    mediaSourceUri,
                    DefaultDataSourceFactory(it, userAgent),
                    DefaultExtractorsFactory(), null, null)

            mPlayer?.prepare(mediaSource)
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
