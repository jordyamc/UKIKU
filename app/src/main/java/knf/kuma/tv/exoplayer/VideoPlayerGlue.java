package knf.kuma.tv.exoplayer;

import android.content.Context;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.PlaybackControlsRow;

import java.util.concurrent.TimeUnit;

/**
 * Manages customizing the actions in the {@link PlaybackControlsRow}. Adds and manages the
 * following actions to the primary and secondary controls:
 *
 * <ul>
 * <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.RepeatAction}
 * <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsDownAction}
 * <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsUpAction}
 * <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.SkipPreviousAction}
 * <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.SkipNextAction}
 * <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.FastForwardAction}
 * <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.RewindAction}
 * </ul>
 * <p>
 * Note that the superclass, {@link PlaybackTransportControlGlue}, manages the playback controls
 * row.
 */
public class VideoPlayerGlue extends PlaybackTransportControlGlue<LeanbackPlayerAdapter> {

    private static final long TEN_SECONDS = TimeUnit.SECONDS.toMillis(30);
    private PlaybackControlsRow.RepeatAction mRepeatAction;
    private PlaybackControlsRow.ThumbsUpAction mThumbsUpAction;
    private PlaybackControlsRow.ThumbsDownAction mThumbsDownAction;
    private PlaybackControlsRow.SkipPreviousAction mSkipPreviousAction;
    private PlaybackControlsRow.SkipNextAction mSkipNextAction;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;
    private PlaybackControlsRow.RewindAction mRewindAction;

    public VideoPlayerGlue(
            Context context,
            LeanbackPlayerAdapter playerAdapter) {
        super(context, playerAdapter);

        mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(context);
        mSkipNextAction = new PlaybackControlsRow.SkipNextAction(context);
        mFastForwardAction = new PlaybackControlsRow.FastForwardAction(context);
        mRewindAction = new PlaybackControlsRow.RewindAction(context);

        mThumbsUpAction = new PlaybackControlsRow.ThumbsUpAction(context);
        mThumbsUpAction.setIndex(PlaybackControlsRow.ThumbsUpAction.INDEX_OUTLINE);
        mThumbsDownAction = new PlaybackControlsRow.ThumbsDownAction(context);
        mThumbsDownAction.setIndex(PlaybackControlsRow.ThumbsDownAction.INDEX_OUTLINE);
        mRepeatAction = new PlaybackControlsRow.RepeatAction(context);
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter adapter) {
        // Order matters, super.onCreatePrimaryActions() will create the play / pause action.
        // Will display as follows:
        // play/pause, previous, rewind, fast forward, next
        //   > /||      |<        <<        >>         >|
        //adapter.add(mSkipPreviousAction);
        adapter.add(mRewindAction);
        super.onCreatePrimaryActions(adapter);
        adapter.add(mFastForwardAction);
        //adapter.add(mSkipNextAction);
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter adapter) {
        super.onCreateSecondaryActions(adapter);
        /*adapter.add(mThumbsDownAction);
        adapter.add(mThumbsUpAction);
        adapter.add(mRepeatAction);*/
    }

    @Override
    public void onActionClicked(Action action) {
        if (shouldDispatchAction(action)) {
            dispatchAction(action);
            return;
        }
        // Super class handles play/pause and delegates to abstract methods next()/previous().
        super.onActionClicked(action);
    }

    // Should dispatch actions that the super class does not supply callbacks for.
    private boolean shouldDispatchAction(Action action) {
        return action == mRewindAction
                || action == mFastForwardAction
                || action == mThumbsDownAction
                || action == mThumbsUpAction
                || action == mRepeatAction;
    }

    private void dispatchAction(Action action) {
        // Primary actions are handled manually.
        if (action == mRewindAction) {
            rewind();
        } else if (action == mFastForwardAction) {
            fastForward();
        } else if (action instanceof PlaybackControlsRow.MultiAction) {
            PlaybackControlsRow.MultiAction multiAction = (PlaybackControlsRow.MultiAction) action;
            multiAction.nextIndex();
            // Notify adapter of action changes to handle secondary actions, such as, thumbs up/down
            // and repeat.
            notifyActionChanged(
                    multiAction,
                    (ArrayObjectAdapter) getControlsRow().getSecondaryActionsAdapter());
        }
    }

    private void notifyActionChanged(
            PlaybackControlsRow.MultiAction action, ArrayObjectAdapter adapter) {
        if (adapter != null) {
            int index = adapter.indexOf(action);
            if (index >= 0) {
                adapter.notifyArrayItemRangeChanged(index, 1);
            }
        }
    }

    @Override
    public void next() {
    }

    @Override
    public void previous() {
    }

    /**
     * Skips backwards 10 seconds.
     */
    public void rewind() {
        long newPosition = getCurrentPosition() - TEN_SECONDS;
        newPosition = (newPosition < 0) ? 0 : newPosition;
        getPlayerAdapter().seekTo(newPosition);
    }

    /**
     * Skips forward 10 seconds.
     */
    public void fastForward() {
        if (getDuration() > -1) {
            long newPosition = getCurrentPosition() + TEN_SECONDS;
            newPosition = (newPosition > getDuration()) ? getDuration() : newPosition;
            getPlayerAdapter().seekTo(newPosition);
        }
    }

    /**
     * Listens for when skip to next and previous actions have been dispatched.
     */
    public interface OnActionClickedListener {

        /**
         * Skip to the previous item in the queue.
         */
        void onPrevious();

        /**
         * Skip to the next item in the queue.
         */
        void onNext();
    }
}
