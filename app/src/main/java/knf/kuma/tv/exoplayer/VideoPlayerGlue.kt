package knf.kuma.tv.exoplayer

import android.content.Context
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow
import knf.kuma.database.CacheDB
import knf.kuma.player.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Manages customizing the actions in the [PlaybackControlsRow]. Adds and manages the
 * following actions to the primary and secondary controls:
 *
 *
 *  * [androidx.leanback.widget.PlaybackControlsRow.RepeatAction]
 *  * [androidx.leanback.widget.PlaybackControlsRow.ThumbsDownAction]
 *  * [androidx.leanback.widget.PlaybackControlsRow.ThumbsUpAction]
 *  * [androidx.leanback.widget.PlaybackControlsRow.SkipPreviousAction]
 *  * [androidx.leanback.widget.PlaybackControlsRow.SkipNextAction]
 *  * [androidx.leanback.widget.PlaybackControlsRow.FastForwardAction]
 *  * [androidx.leanback.widget.PlaybackControlsRow.RewindAction]
 *
 *
 *
 * Note that the superclass, [PlaybackTransportControlGlue], manages the playback controls
 * row.
 */
class VideoPlayerGlue(
        context: Context,
        playerAdapter: LeanbackPlayerAdapter) : PlaybackTransportControlGlue<LeanbackPlayerAdapter>(context, playerAdapter) {
    private val mRepeatAction: PlaybackControlsRow.RepeatAction
    private val mThumbsUpAction: PlaybackControlsRow.ThumbsUpAction = PlaybackControlsRow.ThumbsUpAction(context)
    private val mThumbsDownAction: PlaybackControlsRow.ThumbsDownAction
    private val mSkipPreviousAction: PlaybackControlsRow.SkipPreviousAction = PlaybackControlsRow.SkipPreviousAction(context)
    private val mSkipNextAction: PlaybackControlsRow.SkipNextAction = PlaybackControlsRow.SkipNextAction(context)
    private val mFastForwardAction: PlaybackControlsRow.FastForwardAction = PlaybackControlsRow.FastForwardAction(context)
    private val mRewindAction: PlaybackControlsRow.RewindAction = PlaybackControlsRow.RewindAction(context)

    init {
        mThumbsUpAction.index = PlaybackControlsRow.ThumbsUpAction.INDEX_OUTLINE
        mThumbsDownAction = PlaybackControlsRow.ThumbsDownAction(context)
        mThumbsDownAction.index = PlaybackControlsRow.ThumbsDownAction.INDEX_OUTLINE
        mRepeatAction = PlaybackControlsRow.RepeatAction(context)
    }

    override fun onCreatePrimaryActions(adapter: ArrayObjectAdapter) {
        // Order matters, super.onCreatePrimaryActions() will create the play / pause action.
        // Will display as follows:
        // play/pause, previous, rewind, fast forward, next
        //   > /||      |<        <<        >>         >|
        //adapter.add(mSkipPreviousAction);
        adapter.add(mRewindAction)
        super.onCreatePrimaryActions(adapter)
        adapter.add(mFastForwardAction)
        //adapter.add(mSkipNextAction);
    }

    override fun onActionClicked(action: Action) {
        if (shouldDispatchAction(action)) {
            dispatchAction(action)
            return
        }
        // Super class handles play/pause and delegates to abstract methods next()/previous().
        super.onActionClicked(action)
    }

    // Should dispatch actions that the super class does not supply callbacks for.
    private fun shouldDispatchAction(action: Action): Boolean {
        return (action === mRewindAction
                || action === mFastForwardAction
                || action === mThumbsDownAction
                || action === mThumbsUpAction
                || action === mRepeatAction)
    }

    private fun dispatchAction(action: Action) {
        // Primary actions are handled manually.
        when {
            action === mRewindAction -> rewind()
            action === mFastForwardAction -> fastForward()
            action is PlaybackControlsRow.MultiAction -> {
                action.nextIndex()
                // Notify adapter of action changes to handle secondary actions, such as, thumbs up/down
                // and repeat.
                notifyActionChanged(
                        action,
                        controlsRow.secondaryActionsAdapter as ArrayObjectAdapter)
            }
        }
    }

    fun save(video: Video?) {
        if (video != null) {
            val pos = currentPosition
            GlobalScope.launch(Dispatchers.IO) {
                CacheDB.INSTANCE.playerStateDAO().set(PlayerState("${video.title}: ${video.chapter}", pos))
            }
        }
    }

    private fun notifyActionChanged(
            action: PlaybackControlsRow.MultiAction, adapter: ArrayObjectAdapter?) {
        if (adapter != null) {
            val index = adapter.indexOf(action)
            if (index >= 0) {
                adapter.notifyArrayItemRangeChanged(index, 1)
            }
        }
    }

    override fun next() {}

    override fun previous() {}

    /**
     * Skips backwards 10 seconds.
     */
    fun rewind() {
        var newPosition = currentPosition - TEN_SECONDS
        newPosition = if (newPosition < 0) 0 else newPosition
        playerAdapter.seekTo(newPosition)
    }

    /**
     * Skips forward 10 seconds.
     */
    fun fastForward() {
        if (duration > -1) {
            var newPosition = currentPosition + TEN_SECONDS
            newPosition = if (newPosition > duration) duration else newPosition
            playerAdapter.seekTo(newPosition)
        }
    }


    /**
     * Listens for when skip to next and previous actions have been dispatched.
     */
    interface OnActionClickedListener {

        /**
         * Skip to the previous item in the queue.
         */
        fun onPrevious()

        /**
         * Skip to the next item in the queue.
         */
        fun onNext()
    }

    companion object {

        private val TEN_SECONDS = TimeUnit.SECONDS.toMillis(30)
    }
}
