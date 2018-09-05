package knf.kuma.player;

import android.annotation.TargetApi;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Rational;
import android.view.View;
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
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import knf.kuma.R;
import knf.kuma.commons.EAHelper;
import knf.kuma.database.CacheDB;
import knf.kuma.pojos.QueueObject;
import xdroid.toaster.Toaster;

public class CustomExoPlayer extends AppCompatActivity implements Player.EventListener {
    @BindView(R.id.player)
    SimpleExoPlayerView exoPlayerView;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.pip)
    ImageButton pip;
    @BindView(R.id.exo_next)
    ImageButton next;
    @BindView(R.id.exo_prev)
    ImageButton prev;
    @BindView(R.id.progress)
    ProgressBar progressBar;

    @BindView(R.id.lay_top)
    View top;
    @BindView(R.id.lay_bottom)
    View bottom;
    @BindView(R.id.pip_exit)
    ImageButton pip_exit;

    SimpleExoPlayer player;
    private long currentPosition = C.TIME_UNSET;
    private int resumeWindow = C.INDEX_UNSET;

    private int listPosition = 0;
    private List<QueueObject> playList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(EAHelper.getTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exo_player);
        ButterKnife.bind(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE))
            pip.setVisibility(View.VISIBLE);
        hideUI();
        exoPlayerView.setResizeMode(getResizeMode());
        exoPlayerView.requestFocus();
        if (savedInstanceState != null)
            currentPosition = savedInstanceState.getLong("position", C.TIME_UNSET);
        checkPlaylist(getIntent());
        initPlayer(getIntent());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("position", currentPosition);
    }

    private void hideUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private int getResizeMode() {
        switch (Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(this).getString("player_resize", "0"))) {
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

    private void initPlayer(Intent intent) {
        if (player == null) {
            title.setText(intent.getStringExtra("title"));
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelection.Factory videoTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(bandwidthMeter);
            TrackSelector trackSelector =
                    new DefaultTrackSelector(videoTrackSelectionFactory);
            MediaSource source;
            if (intent.getBooleanExtra("isPlayList", false)) {
                List<MediaSource> sourceList = new ArrayList<>();
                playList = CacheDB.INSTANCE.queueDAO().getAllByAid(intent.getStringExtra("playlist"));
                title.setText(playList.get(0).getTitle());
                for (QueueObject object : playList) {
                    if (object.isFile)
                        sourceList.add(new ExtractorMediaSource.Factory(new FileDataSourceFactory()).createMediaSource(object.uri));
                    else
                        sourceList.add(new ExtractorMediaSource.Factory(new DefaultHttpDataSourceFactory(Util.getUserAgent(this, "UKIKU"))).createMediaSource(object.uri));
                }
                source = new ConcatenatingMediaSource(sourceList.toArray(new MediaSource[]{}));
            } else if (!intent.getBooleanExtra("isFile", false)) {
                source = new ExtractorMediaSource.Factory(new DefaultHttpDataSourceFactory(Util.getUserAgent(this, "UKIKU"))).createMediaSource(intent.getData());
            } else {
                source = new ExtractorMediaSource.Factory(new FileDataSourceFactory()).createMediaSource(intent.getData());
            }
            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
            exoPlayerView.setPlayer(player);
            player.addListener(this);

            boolean canResume = currentPosition != C.TIME_UNSET;
            if (canResume)
                player.seekTo(currentPosition);
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
        try {
            if (!isInPictureInPictureMode()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    currentPosition = player.getCurrentPosition();
                    PictureInPictureParams params = new PictureInPictureParams.Builder()
                            .setAspectRatio(new Rational(exoPlayerView.getWidth(), exoPlayerView.getHeight()))
                            .build();
                    enterPictureInPictureMode(params);
                } else {
                    enterPictureInPictureMode();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.skip)
    void onSkip(View view) {
        if (player != null)
            player.seekTo(player.getCurrentWindowIndex(), player.getCurrentPosition() + 85000);
    }

    @OnClick(R.id.pip_exit)
    void onExitPip(View view) {
        getApplication().startActivity(new Intent(this, getClass())
                .putExtra("isReorder", true)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (!isInPictureInPictureMode) {
            runOnUiThread(() -> {
                top.setVisibility(View.VISIBLE);
                bottom.setVisibility(View.VISIBLE);
                exoPlayerView.setUseController(true);
            });
            /*getApplication().startActivity(new Intent(this, getClass())
                    .putExtra("isReorder", true)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));*/
        } else {
            runOnUiThread(() -> {
                top.setVisibility(View.GONE);
                bottom.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                exoPlayerView.setUseController(false);
            });
        }
    }

    private void checkPlaylist(Intent intent) {
        if (!intent.getBooleanExtra("isPlayList", false)) {
            next.setVisibility(View.GONE);
            prev.setVisibility(View.GONE);
        } else {
            next.setVisibility(View.VISIBLE);
            prev.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        releasePlayer();
        currentPosition = 0;
        resumeWindow = C.INDEX_UNSET;
        checkPlaylist(intent);
        initPlayer(intent);
        super.onNewIntent(intent);
    }

    @Override
    protected void onStart() {
        initPlayer(getIntent());
        super.onStart();
    }

    @Override
    protected void onResume() {
        initPlayer(getIntent());
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                isInPictureInPictureMode())
            return;
        if (player != null) {
            resumeWindow = player.getCurrentWindowIndex();
            currentPosition = player.getCurrentPosition();
            releasePlayer();
        }
    }

    @Override
    protected void onStop() {
        releasePlayer();
        super.onStop();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(final boolean isLoading) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode())
            return;
        progressBar.post(() -> progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE));
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == Player.STATE_ENDED) {
            finish();
        }
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
        try {
            int latestPosition = player.getCurrentWindowIndex();
            if (latestPosition != listPosition) {
                listPosition = latestPosition;
                title.setText(playList.get(listPosition).getTitle());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }
}
