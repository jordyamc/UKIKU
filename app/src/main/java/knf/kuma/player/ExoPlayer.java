package knf.kuma.player;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import knf.kuma.R;
import xdroid.toaster.Toaster;

/**
 * Created by Jordy on 10/01/2018.
 */

public class ExoPlayer extends AppCompatActivity implements Player.EventListener, PlaybackControlView.VisibilityListener {
    @BindView(R.id.player)
    SimpleExoPlayerView exoPlayerView;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.pip)
    ImageButton pip;
    @BindView(R.id.progress)
    ProgressBar progressBar;

    SimpleExoPlayer player;
    private long currentPosition = 0;
    private int resumeWindow = C.INDEX_UNSET;

    private static void addVisibilityListener(SimpleExoPlayerView playerView, PlaybackControlView.VisibilityListener listener) {
        PlaybackControlView playbackControlView = findPlaybackControlView(playerView);
        if (playbackControlView != null)
            playbackControlView.setVisibilityListener(listener);
    }

    private static PlaybackControlView findPlaybackControlView(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);

            if (child instanceof PlaybackControlView)
                return (PlaybackControlView) child;

            if (child instanceof ViewGroup) {
                PlaybackControlView result = findPlaybackControlView((ViewGroup) child);
                if (result != null)
                    return result;
            }
        }

        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exo_player);
        ButterKnife.bind(this);
        title.setText(getIntent().getStringExtra("title"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE))
            pip.setVisibility(View.VISIBLE);
        addVisibilityListener(exoPlayerView, this);
        exoPlayerView.setResizeMode(getResizeMode());
        exoPlayerView.requestFocus();
    }

    private int getResizeMode() {
        switch (PreferenceManager.getDefaultSharedPreferences(this).getString("player_resize", "0")) {
            default:
            case "0":
                return AspectRatioFrameLayout.RESIZE_MODE_FIT;
            case "1":
                return AspectRatioFrameLayout.RESIZE_MODE_FILL;
            case "2":
                return AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH;
            case "3":
                return AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT;
        }
    }

    private void initPlayer() {
        if (player == null) {
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelection.Factory videoTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(bandwidthMeter);
            TrackSelector trackSelector =
                    new DefaultTrackSelector(videoTrackSelectionFactory);
            MediaSource source;
            if (!getIntent().getBooleanExtra("isFile", false)) {
                source = new ExtractorMediaSource.Factory(new DefaultHttpDataSourceFactory(Util.getUserAgent(this, "UKIKU"))).createMediaSource(getIntent().getData());
            } else {
                source = new ExtractorMediaSource.Factory(new FileDataSourceFactory()).createMediaSource(getIntent().getData());
            }
            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
            exoPlayerView.setPlayer(player);
            player.addListener(this);

            boolean canResume = resumeWindow != C.INDEX_UNSET;
            if (canResume)
                player.seekTo(resumeWindow, currentPosition);
            player.prepare(source, !canResume, false);
            player.setPlayWhenReady(true);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    @OnClick(R.id.pip)
    @TargetApi(Build.VERSION_CODES.N)
    void onPip(View view) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isInPictureInPictureMode())
                    enterPictureInPictureMode();
            }
        });
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (!isInPictureInPictureMode)
            getApplication().startActivity(new Intent(this, getClass())
                    .putExtra("isReorder", true)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (exoPlayerView != null && !intent.getBooleanExtra("isReorder", false)) {
            exoPlayerView.getPlayer().stop();
            currentPosition = 0;
            resumeWindow = C.INDEX_UNSET;
            initPlayer();
        }
        super.onNewIntent(intent);
    }

    @Override
    protected void onStart() {
        initPlayer();
        super.onStart();
    }

    @Override
    protected void onResume() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
        initPlayer();
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (player != null) {
            resumeWindow = player.getCurrentWindowIndex();
            currentPosition = Math.max(0, player.getContentPosition());
            releasePlayer();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        releasePlayer();
        super.onStop();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(final boolean isLoading) {
        progressBar.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Exception exception = null;
        switch (error.type) {
            case ExoPlaybackException.TYPE_RENDERER:
                exception = error.getRendererException();
                break;
            case ExoPlaybackException.TYPE_UNEXPECTED:
                exception = error.getUnexpectedException();
                break;
            case ExoPlaybackException.TYPE_SOURCE:
                exception = error.getSourceException();
                break;
        }
        if (exception != null) {
            Toaster.toast("Error al reproducir: " + exception.getMessage());
            Crashlytics.logException(exception);
        } else {
            Toaster.toast("Error desconocido al reproducir");
        }
        finish();
    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    @Override
    public void onVisibilityChange(int visibility) {
        if (visibility != View.VISIBLE)
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }
}
